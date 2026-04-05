package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.OrdinanceEligibilityRow;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrdinanceEligibilityServiceTest {
    private final OrdinanceEligibilityService service = new OrdinanceEligibilityService();

    @Test
    void buildDashboardClassifies110YearBuckets() {
        int currentYear = LocalDate.now().getYear();

        assertEquals(
                OrdinanceStatus.OPEN,
                firstSuggestedStatusForDeathYear(currentYear - 110)
        );
        assertEquals(
                OrdinanceStatus.SOON_1Y,
                firstSuggestedStatusForDeathYear(currentYear - 109)
        );
        assertEquals(
                OrdinanceStatus.SOON_10Y,
                firstSuggestedStatusForDeathYear(currentYear - 100)
        );
        assertEquals(
                OrdinanceStatus.BLOCKED_110,
                firstSuggestedStatusForDeathYear(currentYear - 99)
        );
    }

    @Test
    void spouseSealingUsesLaterRecordedDeathYear() {
        int currentYear = LocalDate.now().getYear();

        Person selected = person(1L, "Selected Person", currentYear - 140);
        Person spouse = person(2L, "Spouse Person", currentYear - 109);

        var spouseLink = new family.balling.descendencytracker.domain.SpouseLink();
        spouseLink.setPersonAId(selected.getPersonId());
        spouseLink.setPersonADisplayName(selected.getDisplayName());
        spouseLink.setPersonBId(spouse.getPersonId());
        spouseLink.setPersonBDisplayName(spouse.getDisplayName());

        List<OrdinanceEligibilityRow> rows = service.buildDashboard(
                selected,
                defaultStatus(selected.getPersonId()),
                List.of(),
                List.of(spouseLink),
                List.of(selected, spouse)
        );

        OrdinanceEligibilityRow spouseRow = rows.stream()
                .filter(row -> "Sealed to Spouse".equals(row.getOrdinanceName()))
                .findFirst()
                .orElseThrow();

        assertEquals(OrdinanceStatus.SOON_1Y, spouseRow.getSuggestedStatus());
    }

    private OrdinanceStatus firstSuggestedStatusForDeathYear(int deathYear) {
        Person person = person(1L, "Test Person", deathYear);
        PersonOrdinanceStatus status = defaultStatus(person.getPersonId());

        return service.buildDashboard(person, status, List.of(parentLinkFor(person.getPersonId())), List.of(), List.of(person))
                .getFirst()
                .getSuggestedStatus();
    }

    private Person person(Long personId, String name, int deathYear) {
        Person person = new Person();
        person.setPersonId(personId);
        person.setPreferredName(name);
        person.setDeathDateText(String.valueOf(deathYear));
        return person;
    }

    private PersonOrdinanceStatus defaultStatus(Long personId) {
        PersonOrdinanceStatus status = new PersonOrdinanceStatus();
        status.setPersonId(personId);
        return status;
    }

    private family.balling.descendencytracker.domain.ParentChildLink parentLinkFor(Long childId) {
        var link = new family.balling.descendencytracker.domain.ParentChildLink();
        link.setLinkId(1L);
        link.setParentPersonId(99L);
        link.setChildPersonId(childId);
        return link;
    }
}
