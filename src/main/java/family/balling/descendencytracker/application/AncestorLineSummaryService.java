package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.AncestorLineSummary;
import family.balling.descendencytracker.domain.OrdinanceEligibilityRow;
import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.AncestorBadgeStatus;
import family.balling.descendencytracker.domain.enums.HorizonBucket;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AncestorLineSummaryService {
    private final RelationshipService relationshipService;
    private final OrdinanceService ordinanceService;
    private final OrdinanceEligibilityService ordinanceEligibilityService;

    public AncestorLineSummaryService(
            RelationshipService relationshipService,
            OrdinanceService ordinanceService,
            OrdinanceEligibilityService ordinanceEligibilityService
    ) {
        this.relationshipService = relationshipService;
        this.ordinanceService = ordinanceService;
        this.ordinanceEligibilityService = ordinanceEligibilityService;
    }

    public AncestorLineSummary buildSummary(Person ancestor, List<Person> allPeople) {
        if (ancestor == null) {
            throw new IllegalArgumentException("Ancestor cannot be null.");
        }

        Map<Long, Person> peopleById = indexPeople(allPeople);
        if (ancestor.getPersonId() != null) {
            peopleById.putIfAbsent(ancestor.getPersonId(), ancestor);
        }

        Set<Long> linePersonIds = new HashSet<>();
        collectDescendantIds(ancestor.getPersonId(), linePersonIds, new HashSet<>());

        SummaryAccumulator accumulator = new SummaryAccumulator();
        for (Long personId : linePersonIds) {
            Person person = peopleById.get(personId);
            if (person == null) {
                continue;
            }

            accumulatePerson(person, allPeople, accumulator);
        }

        AncestorLineSummary summary = new AncestorLineSummary();
        summary.setAncestorPersonId(ancestor.getPersonId());
        summary.setAncestorDisplayName(ancestor.getDisplayName());
        summary.setOpenCount(accumulator.openCount);
        summary.setOpeningSoonCount(accumulator.openingSoonCount);
        summary.setWaiting110Count(accumulator.waiting110Count);
        summary.setUnresolvedCount(accumulator.unresolvedCount);
        summary.setCompleteCount(accumulator.completeCount);
        summary.setNotReviewedCount(accumulator.notReviewedCount);
        summary.setTotalTrackedCount(accumulator.totalTrackedCount);
        summary.setNextAvailableDate(accumulator.nextAvailableDate);
        summary.setBadgeStatus(resolveBadgeStatus(accumulator));
        summary.setSummaryReason(buildSummaryReason(summary));
        return summary;
    }

    public List<AncestorLineSummary> buildSummaries(List<Person> ancestors, List<Person> allPeople) {
        List<AncestorLineSummary> summaries = new ArrayList<>();
        if (ancestors == null || ancestors.isEmpty()) {
            return summaries;
        }

        for (Person ancestor : ancestors) {
            summaries.add(buildSummary(ancestor, allPeople));
        }

        summaries.sort(Comparator.comparing(
                AncestorLineSummary::getAncestorDisplayName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        return summaries;
    }

    public HorizonBucket toHorizonBucket(OrdinanceStatus status) {
        if (status == null) {
            return HorizonBucket.UNKNOWN;
        }

        return switch (status) {
            case OPEN -> HorizonBucket.AVAILABLE_NOW;
            case SOON_1Y -> HorizonBucket.WITHIN_1_YEAR;
            case SOON_2Y -> HorizonBucket.WITHIN_2_YEARS;
            case SOON_5Y -> HorizonBucket.WITHIN_5_YEARS;
            case SOON_10Y -> HorizonBucket.WITHIN_10_YEARS;
            case BLOCKED_110 -> HorizonBucket.BLOCKED;
            case COMPLETE, NOT_APPLICABLE, UNKNOWN -> HorizonBucket.UNKNOWN;
        };
    }

    private void accumulatePerson(Person person, List<Person> allPeople, SummaryAccumulator accumulator) {
        PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(person.getPersonId());
        List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
        List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());
        List<OrdinanceEligibilityRow> dashboard = ordinanceEligibilityService.buildDashboard(
                person,
                ordinanceStatus,
                parents,
                spouses,
                allPeople
        );

        if (person.getReviewedStatus() == ReviewedStatus.NOT_REVIEWED) {
            accumulator.notReviewedCount++;
        }

        for (OrdinanceEligibilityRow row : dashboard) {
            accumulator.totalTrackedCount++;

            OrdinanceStatus status = row.getSuggestedStatus();
            switch (status) {
                case OPEN -> accumulator.openCount++;
                case SOON_1Y, SOON_2Y, SOON_5Y, SOON_10Y -> {
                    accumulator.openingSoonCount++;
                    LocalDate candidateDate = estimateNextAvailableDate(person, row, allPeople);
                    if (candidateDate != null && (accumulator.nextAvailableDate == null
                            || candidateDate.isBefore(accumulator.nextAvailableDate))) {
                        accumulator.nextAvailableDate = candidateDate;
                    }
                }
                case BLOCKED_110 -> {
                    accumulator.waiting110Count++;
                    LocalDate candidateDate = estimateNextAvailableDate(person, row, allPeople);
                    if (candidateDate != null && (accumulator.nextAvailableDate == null
                            || candidateDate.isBefore(accumulator.nextAvailableDate))) {
                        accumulator.nextAvailableDate = candidateDate;
                    }
                }
                case UNKNOWN -> accumulator.unresolvedCount++;
                case COMPLETE, NOT_APPLICABLE -> accumulator.completeCount++;
            }
        }
    }

    private AncestorBadgeStatus resolveBadgeStatus(SummaryAccumulator accumulator) {
        if (accumulator.openCount > 0) {
            return AncestorBadgeStatus.OPEN_NOW;
        }
        if (accumulator.openingSoonCount > 0) {
            return AncestorBadgeStatus.OPENING_SOON;
        }
        if (accumulator.waiting110Count > 0) {
            return AncestorBadgeStatus.WAITING_110;
        }
        if (accumulator.unresolvedCount > 0) {
            return AncestorBadgeStatus.UNRESOLVED;
        }
        if (accumulator.notReviewedCount > 0) {
            return AncestorBadgeStatus.NOT_REVIEWED;
        }
        return AncestorBadgeStatus.COMPLETE_FOR_NOW;
    }

    private String buildSummaryReason(AncestorLineSummary summary) {
        return switch (summary.getBadgeStatus()) {
            case OPEN_NOW -> summary.getOpenCount() + " tracked item(s) are open now.";
            case OPENING_SOON -> summary.getOpeningSoonCount() + " tracked item(s) are opening soon.";
            case WAITING_110 -> summary.getWaiting110Count() + " tracked item(s) are still waiting on the 110-year rule.";
            case UNRESOLVED -> summary.getUnresolvedCount() + " tracked item(s) need more data before eligibility can be determined.";
            case COMPLETE_FOR_NOW -> "All tracked items in this line are complete or not currently applicable.";
            case NOT_REVIEWED -> summary.getNotReviewedCount() + " person record(s) in this line are still marked not reviewed.";
        };
    }

    private LocalDate estimateNextAvailableDate(Person person, OrdinanceEligibilityRow row, List<Person> allPeople) {
        if ("Sealed to Spouse".equals(row.getOrdinanceName())) {
            Person spouse = findSpouseByName(person, row.getRelatedPersonName(), allPeople);
            Integer personDeathYear = extractDeathYear(person);
            Integer spouseDeathYear = extractDeathYear(spouse);
            if (personDeathYear == null || spouseDeathYear == null) {
                return null;
            }
            return LocalDate.of(Math.max(personDeathYear, spouseDeathYear) + 110, 1, 1);
        }

        Integer deathYear = extractDeathYear(person);
        if (deathYear == null) {
            return null;
        }
        return LocalDate.of(deathYear + 110, 1, 1);
    }

    private Person findSpouseByName(Person person, String spouseName, List<Person> allPeople) {
        if (person == null || spouseName == null || spouseName.isBlank()) {
            return null;
        }

        Map<Long, Person> peopleById = indexPeople(allPeople);
        for (SpouseLink spouseLink : relationshipService.getSpousesForPerson(person.getPersonId())) {
            Long otherPersonId = spouseLink.getOtherPersonId(person.getPersonId());
            Person spouse = otherPersonId == null ? null : peopleById.get(otherPersonId);
            if (spouse != null && spouseName.equalsIgnoreCase(spouse.getDisplayName())) {
                return spouse;
            }
        }

        return null;
    }

    private Integer extractDeathYear(Person person) {
        if (person == null || person.getDeathDateText() == null || person.getDeathDateText().isBlank()) {
            return null;
        }

        String text = person.getDeathDateText();
        for (int i = 0; i <= text.length() - 4; i++) {
            String candidate = text.substring(i, i + 4);
            if (candidate.chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(candidate);
            }
        }

        return null;
    }

    private void collectDescendantIds(Long personId, Set<Long> collected, Set<Long> path) {
        if (personId == null || !path.add(personId) || !collected.add(personId)) {
            return;
        }

        for (ParentChildLink childLink : relationshipService.getChildrenForPerson(personId)) {
            collectDescendantIds(childLink.getChildPersonId(), collected, new HashSet<>(path));
        }
    }

    private Map<Long, Person> indexPeople(List<Person> allPeople) {
        Map<Long, Person> peopleById = new HashMap<>();
        if (allPeople == null) {
            return peopleById;
        }

        for (Person person : allPeople) {
            if (person != null && person.getPersonId() != null) {
                peopleById.put(person.getPersonId(), person);
            }
        }

        return peopleById;
    }

    private static class SummaryAccumulator {
        private int openCount;
        private int openingSoonCount;
        private int waiting110Count;
        private int unresolvedCount;
        private int completeCount;
        private int notReviewedCount;
        private int totalTrackedCount;
        private LocalDate nextAvailableDate;
    }
}
