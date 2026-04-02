package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.OrdinanceEligibilityRow;
import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.WorkQueueRow;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorkQueueService {
    private final RelationshipService relationshipService;
    private final OrdinanceService ordinanceService;
    private final OrdinanceEligibilityService ordinanceEligibilityService;

    public WorkQueueService(
            RelationshipService relationshipService,
            OrdinanceService ordinanceService,
            OrdinanceEligibilityService ordinanceEligibilityService
    ) {
        this.relationshipService = relationshipService;
        this.ordinanceService = ordinanceService;
        this.ordinanceEligibilityService = ordinanceEligibilityService;
    }

    public List<WorkQueueRow> buildWorkQueue(List<Person> allPeople) {
        List<WorkQueueRow> rows = new ArrayList<>();

        if (allPeople == null || allPeople.isEmpty()) {
            return rows;
        }

        for (Person person : allPeople) {
            PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(person.getPersonId());
            List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
            List<ParentChildLink> children = relationshipService.getChildrenForPerson(person.getPersonId());
            List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());

            List<OrdinanceEligibilityRow> dashboard = ordinanceEligibilityService.buildDashboard(
                    person,
                    ordinanceStatus,
                    parents,
                    spouses,
                    allPeople
            );

            OrdinanceEligibilityRow bestRow = dashboard.stream()
                    .filter(row -> isQueueBucket(row.getSuggestedStatus()))
                    .min(Comparator
                            .comparingInt((OrdinanceEligibilityRow row) -> bucketPriority(row.getSuggestedStatus()))
                            .thenComparing(OrdinanceEligibilityRow::getOrdinanceName)
                            .thenComparing(row -> row.getRelatedPersonName() == null ? "" : row.getRelatedPersonName()))
                    .orElse(null);

            if (bestRow == null) {
                continue;
            }

            String triggerLabel = buildTriggerLabel(bestRow);

            rows.add(new WorkQueueRow(
                    person.getPersonId(),
                    person.getDisplayName(),
                    person.getFsPid(),
                    bestRow.getSuggestedStatus(),
                    triggerLabel,
                    bestRow.getReason(),
                    parents.size(),
                    children.size(),
                    spouses.size()
            ));
        }

        rows.sort(Comparator
                .comparingInt((WorkQueueRow row) -> bucketPriority(row.getQueueBucket()))
                .thenComparing(WorkQueueRow::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        return rows;
    }

    public boolean isActionable(OrdinanceStatus status) {
        return status == OrdinanceStatus.OPEN
                || status == OrdinanceStatus.SOON_1Y
                || status == OrdinanceStatus.SOON_2Y
                || status == OrdinanceStatus.SOON_5Y
                || status == OrdinanceStatus.SOON_10Y;
    }

    private boolean isQueueBucket(OrdinanceStatus status) {
        return status == OrdinanceStatus.OPEN
                || status == OrdinanceStatus.SOON_1Y
                || status == OrdinanceStatus.SOON_2Y
                || status == OrdinanceStatus.SOON_5Y
                || status == OrdinanceStatus.SOON_10Y
                || status == OrdinanceStatus.UNKNOWN
                || status == OrdinanceStatus.BLOCKED_110;
    }

    private int bucketPriority(OrdinanceStatus status) {
        if (status == null) {
            return 999;
        }

        return switch (status) {
            case OPEN -> 0;
            case SOON_1Y -> 1;
            case SOON_2Y -> 2;
            case SOON_5Y -> 3;
            case SOON_10Y -> 4;
            case UNKNOWN -> 5;
            case BLOCKED_110 -> 6;
            case NOT_APPLICABLE -> 7;
            case COMPLETE -> 8;
        };
    }

    private String buildTriggerLabel(OrdinanceEligibilityRow row) {
        String related = row.getRelatedPersonName();
        if (related == null || related.isBlank()) {
            return row.getOrdinanceName();
        }
        return row.getOrdinanceName() + " — " + related;
    }
}