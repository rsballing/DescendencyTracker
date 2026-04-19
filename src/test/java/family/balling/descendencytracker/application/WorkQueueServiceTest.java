package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.WorkQueueRow;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.repository.OrdinanceRepository;
import family.balling.descendencytracker.repository.PersonRepository;
import family.balling.descendencytracker.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkQueueServiceTest {
    @Test
    void buildWorkQueueIncludesReservationAndConnectionFlags() {
        Person person = new Person();
        person.setPersonId(1L);
        person.setPreferredName("Queue Person");

        Person spouse = new Person();
        spouse.setPersonId(2L);
        spouse.setPreferredName("Spouse");

        PersonOrdinanceStatus ordinanceStatus = new PersonOrdinanceStatus();
        ordinanceStatus.setPersonId(1L);
        ordinanceStatus.setBaptismReserved(true);

        InMemoryPersonRepository personRepository = new InMemoryPersonRepository(List.of(person, spouse));
        InMemoryRelationshipRepository relationshipRepository = new InMemoryRelationshipRepository();
        relationshipRepository.addSpouseLink(1L, 2L, true);

        WorkQueueService service = new WorkQueueService(
                new RelationshipService(personRepository, relationshipRepository),
                new OrdinanceService(new InMemoryOrdinanceRepository(Map.of(1L, ordinanceStatus))),
                new OrdinanceEligibilityService()
        );

        WorkQueueRow row = service.buildWorkQueue(List.of(person, spouse)).stream()
                .filter(candidate -> Long.valueOf(1L).equals(candidate.getPersonId()))
                .findFirst()
                .orElseThrow();

        assertTrue(row.hasReservedOrdinances());
        assertTrue(row.hasConnectedSpouses());
        assertFalse(row.isConfirmedNoChildren());
    }

    @Test
    void buildWorkQueueKeepsRowsForConfirmedMissingRelationships() {
        Person person = new Person();
        person.setPersonId(1L);
        person.setPreferredName("Solo");
        person.setConfirmedNoChildren(true);
        person.setConfirmedNoSpouse(true);

        InMemoryPersonRepository personRepository = new InMemoryPersonRepository(List.of(person));
        InMemoryRelationshipRepository relationshipRepository = new InMemoryRelationshipRepository();

        WorkQueueService service = new WorkQueueService(
                new RelationshipService(personRepository, relationshipRepository),
                new OrdinanceService(new InMemoryOrdinanceRepository(Map.of())),
                new OrdinanceEligibilityService()
        );

        WorkQueueRow row = service.buildWorkQueue(List.of(person)).getFirst();

        assertTrue(row.isConfirmedNoChildren());
        assertTrue(row.isConfirmedNoSpouse());
        assertFalse(row.hasConnectedChildren());
        assertFalse(row.hasConnectedSpouses());
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
        private final List<ParentChildLink> parentChildLinks = new ArrayList<>();
        private final List<SpouseLink> spouseLinks = new ArrayList<>();

        void addSpouseLink(long personId, long spouseId, boolean reserved) {
            SpouseLink link = new SpouseLink();
            link.setSpouseLinkId((long) (spouseLinks.size() + 1));
            link.setPersonAId(personId);
            link.setPersonBId(spouseId);
            link.setPersonADisplayName("Queue Person");
            link.setPersonBDisplayName("Spouse");
            link.setSealedToSpouseReserved(reserved);
            spouseLinks.add(link);
        }

        @Override
        public List<ParentChildLink> findParentsForChild(long childPersonId) {
            return parentChildLinks.stream()
                    .filter(link -> Long.valueOf(childPersonId).equals(link.getChildPersonId()))
                    .toList();
        }

        @Override
        public List<ParentChildLink> findChildrenForParent(long parentPersonId) {
            return parentChildLinks.stream()
                    .filter(link -> Long.valueOf(parentPersonId).equals(link.getParentPersonId()))
                    .toList();
        }

        @Override
        public List<SpouseLink> findSpousesForPerson(long personId) {
            return spouseLinks.stream()
                    .filter(link -> Long.valueOf(personId).equals(link.getPersonAId()) || Long.valueOf(personId).equals(link.getPersonBId()))
                    .toList();
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
        public SpouseLink addSpouse(long personAId, long personBId, String marriageDateText, String marriageNotes, OrdinanceStatus sealingToSpouseStatus, boolean sealedToSpouseReserved, String sealingStatusDate, String sealingNotes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpouseLink updateSpouse(long spouseLinkId, long personAId, long personBId, String marriageDateText, String marriageNotes, OrdinanceStatus sealingToSpouseStatus, boolean sealedToSpouseReserved, String sealingStatusDate, String sealingNotes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void softDeleteSpouse(long spouseLinkId) {
        }
    }

    private static class InMemoryOrdinanceRepository implements OrdinanceRepository {
        private final Map<Long, PersonOrdinanceStatus> byPersonId;

        InMemoryOrdinanceRepository(Map<Long, PersonOrdinanceStatus> byPersonId) {
            this.byPersonId = new HashMap<>(byPersonId);
        }

        @Override
        public Optional<PersonOrdinanceStatus> findByPersonId(long personId) {
            return Optional.ofNullable(byPersonId.get(personId));
        }

        @Override
        public PersonOrdinanceStatus save(PersonOrdinanceStatus status) {
            byPersonId.put(status.getPersonId(), status);
            return status;
        }
    }
}
