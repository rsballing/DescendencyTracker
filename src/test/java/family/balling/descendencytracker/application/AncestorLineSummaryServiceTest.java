package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.AncestorLineSummary;
import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.AncestorBadgeStatus;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;
import family.balling.descendencytracker.repository.OrdinanceRepository;
import family.balling.descendencytracker.repository.PersonRepository;
import family.balling.descendencytracker.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AncestorLineSummaryServiceTest {
    @Test
    void buildSummaryAggregatesDescendantsAndAvoidsCycles() {
        int currentYear = LocalDate.now().getYear();

        Person ancestor = person(1L, "Ancestor", currentYear - 130, ReviewedStatus.REVIEWED);
        Person child = person(2L, "Child", currentYear - 109, ReviewedStatus.NOT_REVIEWED);

        InMemoryPersonRepository people = new InMemoryPersonRepository(List.of(ancestor, child));
        InMemoryRelationshipRepository relationships = new InMemoryRelationshipRepository();
        relationships.addParentChild(1L, 2L);
        relationships.addParentChild(2L, 1L);

        OrdinanceService ordinanceService = new OrdinanceService(new InMemoryOrdinanceRepository());
        AncestorLineSummaryService service = new AncestorLineSummaryService(
                new RelationshipService(people, relationships),
                ordinanceService,
                new OrdinanceEligibilityService()
        );

        AncestorLineSummary summary = service.buildSummary(ancestor, List.of(ancestor, child));

        assertEquals(AncestorBadgeStatus.OPEN_NOW, summary.getBadgeStatus());
        assertEquals(5, summary.getOpenCount());
        assertEquals(5, summary.getOpeningSoonCount());
        assertEquals(0, summary.getWaiting110Count());
        assertEquals(0, summary.getUnresolvedCount());
        assertEquals(1, summary.getNotReviewedCount());
        assertEquals(10, summary.getTotalTrackedCount());
        assertEquals(LocalDate.of(currentYear + 1, 1, 1), summary.getNextAvailableDate());
    }

    @Test
    void toHorizonBucketMapsOrdinanceStatusConsistently() {
        AncestorLineSummaryService service = new AncestorLineSummaryService(
                new RelationshipService(new InMemoryPersonRepository(List.of()), new InMemoryRelationshipRepository()),
                new OrdinanceService(new InMemoryOrdinanceRepository()),
                new OrdinanceEligibilityService()
        );

        assertEquals(family.balling.descendencytracker.domain.enums.HorizonBucket.AVAILABLE_NOW, service.toHorizonBucket(OrdinanceStatus.OPEN));
        assertEquals(family.balling.descendencytracker.domain.enums.HorizonBucket.WITHIN_1_YEAR, service.toHorizonBucket(OrdinanceStatus.SOON_1Y));
        assertEquals(family.balling.descendencytracker.domain.enums.HorizonBucket.BLOCKED, service.toHorizonBucket(OrdinanceStatus.BLOCKED_110));
        assertEquals(family.balling.descendencytracker.domain.enums.HorizonBucket.UNKNOWN, service.toHorizonBucket(OrdinanceStatus.UNKNOWN));
    }

    private Person person(long id, String name, int deathYear, ReviewedStatus reviewedStatus) {
        Person person = new Person();
        person.setPersonId(id);
        person.setPreferredName(name);
        person.setDeathDateText(String.valueOf(deathYear));
        person.setReviewedStatus(reviewedStatus);
        return person;
    }

    private static class InMemoryPersonRepository implements PersonRepository {
        private final Map<Long, Person> people = new HashMap<>();

        InMemoryPersonRepository(List<Person> people) {
            for (Person person : people) {
                this.people.put(person.getPersonId(), person);
            }
        }

        @Override
        public List<Person> findAllActive() {
            return new ArrayList<>(people.values());
        }

        @Override
        public Optional<Person> findById(long personId) {
            return Optional.ofNullable(people.get(personId));
        }

        @Override
        public Optional<Person> findRootPerson() {
            return Optional.empty();
        }

        @Override
        public Person save(Person person) {
            people.put(person.getPersonId(), person);
            return person;
        }

        @Override
        public void softDelete(long personId) {
        }

        @Override
        public void setRootPerson(long personId) {
        }
    }

    private static class InMemoryRelationshipRepository implements RelationshipRepository {
        private final List<ParentChildLink> links = new ArrayList<>();

        void addParentChild(long parentId, long childId) {
            ParentChildLink link = new ParentChildLink();
            link.setLinkId((long) (links.size() + 1));
            link.setParentPersonId(parentId);
            link.setChildPersonId(childId);
            links.add(link);
        }

        @Override
        public List<ParentChildLink> findParentsForChild(long childPersonId) {
            return links.stream()
                    .filter(link -> !link.isDeleted() && Long.valueOf(childPersonId).equals(link.getChildPersonId()))
                    .toList();
        }

        @Override
        public List<ParentChildLink> findChildrenForParent(long parentPersonId) {
            return links.stream()
                    .filter(link -> !link.isDeleted() && Long.valueOf(parentPersonId).equals(link.getParentPersonId()))
                    .toList();
        }

        @Override
        public List<SpouseLink> findSpousesForPerson(long personId) {
            return List.of();
        }

        @Override
        public ParentChildLink addParentChild(long parentPersonId, long childPersonId, Integer childOrder, String notes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ParentChildLink updateParentChild(long linkId, long parentPersonId, long childPersonId, Integer childOrder, String notes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void softDeleteParentChild(long linkId) {
        }

        @Override
        public SpouseLink addSpouse(long personAId, long personBId, String marriageDateText, String marriageNotes, OrdinanceStatus sealingToSpouseStatus, String sealingStatusDate, String sealingNotes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpouseLink updateSpouse(long spouseLinkId, long personAId, long personBId, String marriageDateText, String marriageNotes, OrdinanceStatus sealingToSpouseStatus, String sealingStatusDate, String sealingNotes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void softDeleteSpouse(long spouseLinkId) {
        }
    }

    private static class InMemoryOrdinanceRepository implements OrdinanceRepository {
        @Override
        public Optional<PersonOrdinanceStatus> findByPersonId(long personId) {
            return Optional.empty();
        }

        @Override
        public PersonOrdinanceStatus save(PersonOrdinanceStatus status) {
            return status;
        }
    }
}
