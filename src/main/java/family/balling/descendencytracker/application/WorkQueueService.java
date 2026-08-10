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
import java.time.LocalDate;

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
            if (person == null || person.getPersonId() == null || person.isLiving()) {
                continue;
            }

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
                    .filter(row -> isQueueBucket(queueStatusFor(row)))
                    .min(Comparator
                            .comparingInt((OrdinanceEligibilityRow row) -> bucketPriority(queueStatusFor(row)))
                            .thenComparing(OrdinanceEligibilityRow::getOrdinanceName)
                            .thenComparing(row -> row.getRelatedPersonName() == null ? "" : row.getRelatedPersonName()))
                    .orElse(null);

            boolean hasConnectedParents = !parents.isEmpty();
            boolean hasConnectedChildren = !children.isEmpty();
            boolean hasConnectedSpouses = !spouses.isEmpty();

            rows.add(new WorkQueueRow(
                    person.getPersonId(),
                    person.getDisplayName(),
                    person.getFsPid(),
                    bestRow == null ? null : queueStatusFor(bestRow),
                    bestRow == null
                            ? buildFallbackTriggerLabel(person, parents, children, spouses)
                            : buildTriggerLabel(bestRow),
                    bestRow == null
                            ? buildFallbackReason(person, parents, children, spouses)
                            : buildQueueReason(bestRow),
                    parents.size(),
                    children.size(),
                    spouses.size(),
                    dashboard.stream().anyMatch(row -> row.getRecordedStatus() == OrdinanceStatus.OPEN),
                    isBornMoreThan110YearsAgo(person),
                    hasReservedOrdinances(ordinanceStatus, spouses),
                    hasConnectedParents,
                    hasConnectedChildren,
                    hasConnectedSpouses,
                    person.isConfirmedNoChildren(),
                    person.isConfirmedNoSpouse(),
                    person.isNonBloodRelative(),
                    person.isNoMoreFindable()
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
        return row.getOrdinanceName() + " - " + related;
    }

    private OrdinanceStatus queueStatusFor(OrdinanceEligibilityRow row) {
        if (row == null) {
            return null;
        }

        if (row.getRecordedStatus() == OrdinanceStatus.OPEN) {
            return OrdinanceStatus.OPEN;
        }

        return row.getSuggestedStatus();
    }

    private String buildQueueReason(OrdinanceEligibilityRow row) {
        if (row == null) {
            return "";
        }

        if (row.getRecordedStatus() == OrdinanceStatus.OPEN) {
            return "Recorded ordinance status is marked open.";
        }

        return row.getReason();
    }

    private boolean hasReservedOrdinances(PersonOrdinanceStatus ordinanceStatus, List<SpouseLink> spouses) {
        if (ordinanceStatus.isBaptismReserved()
                || ordinanceStatus.isConfirmationReserved()
                || ordinanceStatus.isInitiatoryReserved()
                || ordinanceStatus.isEndowmentReserved()
                || ordinanceStatus.isSealedToParentsReserved()) {
            return true;
        }

        return spouses.stream().anyMatch(SpouseLink::isSealedToSpouseReserved);
    }

    private boolean isBornMoreThan110YearsAgo(Person person) {
        Integer birthYear = extractYear(person == null ? null : person.getBirthDateText());
        if (birthYear == null) {
            return false;
        }

        return birthYear <= LocalDate.now().minusYears(110).getYear();
    }

    private Integer extractYear(String dateText) {
        if (dateText == null) {
            return null;
        }

        String trimmed = dateText.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String[] tokens = trimmed.split("[^0-9]+");
        for (int index = tokens.length - 1; index >= 0; index--) {
            String token = tokens[index];
            if (token.length() == 4) {
                try {
                    return Integer.parseInt(token);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private String buildFallbackTriggerLabel(
            Person person,
            List<ParentChildLink> parents,
            List<ParentChildLink> children,
            List<SpouseLink> spouses
    ) {
        if (person.isConfirmedNoSpouse() && spouses.isEmpty()) {
            return "Confirmed no spouse";
        }
        if (person.isConfirmedNoChildren() && children.isEmpty()) {
            return "Confirmed no children";
        }
        if (spouses.isEmpty()) {
            return "Missing spouse connection";
        }
        if (children.isEmpty()) {
            return "Missing child connection";
        }
        if (parents.isEmpty()) {
            return "Missing parent connection";
        }
        return "No current trigger";
    }

    private String buildFallbackReason(
            Person person,
            List<ParentChildLink> parents,
            List<ParentChildLink> children,
            List<SpouseLink> spouses
    ) {
        if (person.isConfirmedNoSpouse() && spouses.isEmpty()) {
            return "Spouse absence has been explicitly confirmed.";
        }
        if (person.isConfirmedNoChildren() && children.isEmpty()) {
            return "Child absence has been explicitly confirmed.";
        }
        if (spouses.isEmpty()) {
            return "No connected spouse relationship is recorded.";
        }
        if (children.isEmpty()) {
            return "No connected child relationship is recorded.";
        }
        if (parents.isEmpty()) {
            return "No connected parent relationship is recorded.";
        }
        return "No open or unresolved ordinance work is currently flagged.";
    }
}
