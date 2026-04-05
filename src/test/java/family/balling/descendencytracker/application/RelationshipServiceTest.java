package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.repository.PersonRepository;
import family.balling.descendencytracker.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelationshipServiceTest {
    @Test
    void addParentRejectsCycle() {
        InMemoryPersonRepository people = new InMemoryPersonRepository();
        people.put(person(1L, "Grandparent"));
        people.put(person(2L, "Parent"));
        people.put(person(3L, "Child"));

        InMemoryRelationshipRepository relationships = new InMemoryRelationshipRepository();
        relationships.addExistingParentChild(1L, 2L);
        relationships.addExistingParentChild(2L, 3L);

        RelationshipService service = new RelationshipService(people, relationships);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.addParent(1L, 3L, null, "cycle")
        );

        assertEquals("That parent-child relationship would create a cycle in the family tree.", ex.getMessage());
    }

    @Test
    void addParentRejectsDuplicateActiveLink() {
        InMemoryPersonRepository people = new InMemoryPersonRepository();
        people.put(person(1L, "Parent"));
        people.put(person(2L, "Child"));

        InMemoryRelationshipRepository relationships = new InMemoryRelationshipRepository();
        relationships.addExistingParentChild(1L, 2L);

        RelationshipService service = new RelationshipService(people, relationships);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.addParent(2L, 1L, null, null)
        );

        assertEquals("That parent-child relationship already exists.", ex.getMessage());
    }

    private Person person(long id, String name) {
        Person person = new Person();
        person.setPersonId(id);
        person.setPreferredName(name);
        return person;
    }

    private static class InMemoryPersonRepository implements PersonRepository {
        private final Map<Long, Person> people = new HashMap<>();

        void put(Person person) {
            people.put(person.getPersonId(), person);
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

        void addExistingParentChild(long parentId, long childId) {
            ParentChildLink link = new ParentChildLink();
            link.setLinkId((long) (parentChildLinks.size() + 1));
            link.setParentPersonId(parentId);
            link.setChildPersonId(childId);
            parentChildLinks.add(link);
        }

        @Override
        public List<ParentChildLink> findParentsForChild(long childPersonId) {
            return parentChildLinks.stream()
                    .filter(link -> !link.isDeleted() && Long.valueOf(childPersonId).equals(link.getChildPersonId()))
                    .toList();
        }

        @Override
        public List<ParentChildLink> findChildrenForParent(long parentPersonId) {
            return parentChildLinks.stream()
                    .filter(link -> !link.isDeleted() && Long.valueOf(parentPersonId).equals(link.getParentPersonId()))
                    .toList();
        }

        @Override
        public List<SpouseLink> findSpousesForPerson(long personId) {
            return List.of();
        }

        @Override
        public ParentChildLink addParentChild(long parentPersonId, long childPersonId, Integer childOrder, String notes) {
            ParentChildLink link = new ParentChildLink();
            link.setLinkId((long) (parentChildLinks.size() + 1));
            link.setParentPersonId(parentPersonId);
            link.setChildPersonId(childPersonId);
            link.setChildOrder(childOrder);
            link.setNotes(notes);
            parentChildLinks.add(link);
            return link;
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
}
