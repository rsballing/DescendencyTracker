package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.OrdinanceEligibilityRow;
import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrdinanceEligibilityService {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(\\d{4})\\b");

    public List<OrdinanceEligibilityRow> buildDashboard(
            Person selectedPerson,
            PersonOrdinanceStatus personStatus,
            List<ParentChildLink> parentLinks,
            List<SpouseLink> spouseLinks,
            List<Person> allPeople
    ) {
        List<OrdinanceEligibilityRow> rows = new ArrayList<>();

        rows.add(buildPersonRow(
                "Baptism",
                personStatus.getBaptismStatus(),
                selectedPerson,
                true
        ));
        rows.add(buildPersonRow(
                "Confirmation",
                personStatus.getConfirmationStatus(),
                selectedPerson,
                true
        ));
        rows.add(buildPersonRow(
                "Initiatory",
                personStatus.getInitiatoryStatus(),
                selectedPerson,
                true
        ));
        rows.add(buildPersonRow(
                "Endowment",
                personStatus.getEndowmentStatus(),
                selectedPerson,
                true
        ));
        rows.add(buildSealedToParentsRow(
                personStatus.getSealedToParentsStatus(),
                selectedPerson,
                parentLinks
        ));

        for (SpouseLink spouseLink : spouseLinks) {
            Person spouse = findPerson(allPeople, spouseLink.getOtherPersonId(selectedPerson.getPersonId()));
            rows.add(buildSpouseSealingRow(selectedPerson, spouseLink, spouse));
        }

        return rows;
    }

    private OrdinanceEligibilityRow buildPersonRow(
            String ordinanceName,
            OrdinanceStatus recordedStatus,
            Person person,
            boolean apply110Rule
    ) {
        Suggestion suggestion = computeSuggestionFromPerson(person, apply110Rule);

        return new OrdinanceEligibilityRow(
                true,
                ordinanceName,
                "",
                safeStatus(recordedStatus),
                recordedStatus == OrdinanceStatus.COMPLETE ? OrdinanceStatus.COMPLETE : suggestion.status(),
                recordedStatus == OrdinanceStatus.COMPLETE ? "Already marked complete." : suggestion.reason()
        );
    }

    private OrdinanceEligibilityRow buildSealedToParentsRow(
            OrdinanceStatus recordedStatus,
            Person person,
            List<ParentChildLink> parentLinks
    ) {
        if (recordedStatus == OrdinanceStatus.COMPLETE) {
            return new OrdinanceEligibilityRow(
                    true,
                    "Sealed to Parents",
                    "",
                    recordedStatus,
                    OrdinanceStatus.COMPLETE,
                    "Already marked complete."
            );
        }

        if (person.isLiving()) {
            return new OrdinanceEligibilityRow(
                    true,
                    "Sealed to Parents",
                    "",
                    safeStatus(recordedStatus),
                    OrdinanceStatus.NOT_APPLICABLE,
                    "Selected person is marked living."
            );
        }

        if (parentLinks == null || parentLinks.isEmpty()) {
            return new OrdinanceEligibilityRow(
                    true,
                    "Sealed to Parents",
                    "",
                    safeStatus(recordedStatus),
                    OrdinanceStatus.UNKNOWN,
                    "No parent relationship is recorded."
            );
        }

        Suggestion suggestion = computeSuggestionFromPerson(person, true);

        return new OrdinanceEligibilityRow(
                true,
                "Sealed to Parents",
                "",
                safeStatus(recordedStatus),
                suggestion.status(),
                suggestion.reason()
        );
    }

    private OrdinanceEligibilityRow buildSpouseSealingRow(Person selectedPerson, SpouseLink spouseLink, Person spouse) {
        String spouseName = spouseLink.getOtherPersonDisplayName(selectedPerson.getPersonId());
        OrdinanceStatus recordedStatus = safeStatus(spouseLink.getSealingToSpouseStatus());

        if (recordedStatus == OrdinanceStatus.COMPLETE) {
            return new OrdinanceEligibilityRow(
                    false,
                    "Sealed to Spouse",
                    spouseName,
                    recordedStatus,
                    OrdinanceStatus.COMPLETE,
                    "Already marked complete."
            );
        }

        if (spouse == null) {
            return new OrdinanceEligibilityRow(
                    false,
                    "Sealed to Spouse",
                    spouseName,
                    recordedStatus,
                    OrdinanceStatus.UNKNOWN,
                    "Related spouse record could not be resolved."
            );
        }

        if (selectedPerson.isLiving() || spouse.isLiving()) {
            return new OrdinanceEligibilityRow(
                    false,
                    "Sealed to Spouse",
                    spouse.getDisplayName(),
                    recordedStatus,
                    OrdinanceStatus.NOT_APPLICABLE,
                    "At least one spouse is marked living."
            );
        }

        Integer selectedDeathYear = extractYear(selectedPerson.getDeathDateText());
        Integer spouseDeathYear = extractYear(spouse.getDeathDateText());

        if (selectedDeathYear == null || spouseDeathYear == null) {
            return new OrdinanceEligibilityRow(
                    false,
                    "Sealed to Spouse",
                    spouse.getDisplayName(),
                    recordedStatus,
                    OrdinanceStatus.UNKNOWN,
                    "A death year is missing for one or both spouses."
            );
        }

        int latestDeathYear = Math.max(selectedDeathYear, spouseDeathYear);
        Suggestion suggestion = computeSuggestionFromDeathYear(latestDeathYear);

        return new OrdinanceEligibilityRow(
                false,
                "Sealed to Spouse",
                spouse.getDisplayName(),
                recordedStatus,
                suggestion.status(),
                suggestion.reason() + " Conservatively based on the later recorded death year."
        );
    }

    private Suggestion computeSuggestionFromPerson(Person person, boolean apply110Rule) {
        if (person == null) {
            return new Suggestion(OrdinanceStatus.UNKNOWN, "No person is selected.");
        }

        if (person.isLiving()) {
            return new Suggestion(OrdinanceStatus.NOT_APPLICABLE, "Selected person is marked living.");
        }

        if (!apply110Rule) {
            return new Suggestion(OrdinanceStatus.UNKNOWN, "No rule is configured.");
        }

        Integer deathYear = extractYear(person.getDeathDateText());
        if (deathYear == null) {
            return new Suggestion(OrdinanceStatus.UNKNOWN, "No death year could be parsed from the death date text.");
        }

        return computeSuggestionFromDeathYear(deathYear);
    }

    private Suggestion computeSuggestionFromDeathYear(int deathYear) {
        int currentYear = LocalDate.now().getYear();

        if (deathYear > currentYear) {
            return new Suggestion(OrdinanceStatus.UNKNOWN, "The recorded death year is in the future.");
        }

        int yearsSinceDeath = currentYear - deathYear;
        int yearsUntil110 = 110 - yearsSinceDeath;

        if (yearsUntil110 <= 0) {
            return new Suggestion(
                    OrdinanceStatus.OPEN,
                    "Recorded death year " + deathYear + " is at least 110 years ago."
            );
        }

        if (yearsUntil110 <= 1) {
            return new Suggestion(
                    OrdinanceStatus.SOON_1Y,
                    "Recorded death year " + deathYear + " reaches 110 years within 1 year."
            );
        }

        if (yearsUntil110 <= 2) {
            return new Suggestion(
                    OrdinanceStatus.SOON_2Y,
                    "Recorded death year " + deathYear + " reaches 110 years within 2 years."
            );
        }

        if (yearsUntil110 <= 5) {
            return new Suggestion(
                    OrdinanceStatus.SOON_5Y,
                    "Recorded death year " + deathYear + " reaches 110 years within 5 years."
            );
        }

        if (yearsUntil110 <= 10) {
            return new Suggestion(
                    OrdinanceStatus.SOON_10Y,
                    "Recorded death year " + deathYear + " reaches 110 years within 10 years."
            );
        }

        return new Suggestion(
                OrdinanceStatus.BLOCKED_110,
                "Recorded death year " + deathYear + " is more than 10 years away from the 110-year mark."
        );
    }

    private Person findPerson(List<Person> allPeople, Long personId) {
        if (personId == null || allPeople == null) {
            return null;
        }

        for (Person person : allPeople) {
            if (person.getPersonId().equals(personId)) {
                return person;
            }
        }

        return null;
    }

    private Integer extractYear(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = YEAR_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    private OrdinanceStatus safeStatus(OrdinanceStatus status) {
        return status == null ? OrdinanceStatus.UNKNOWN : status;
    }

    private record Suggestion(OrdinanceStatus status, String reason) {
    }
}