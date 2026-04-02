package family.balling.descendencytracker.repository;

import family.balling.descendencytracker.domain.Person;

import java.util.List;
import java.util.Optional;

public interface PersonRepository {
    List<Person> findAllActive();

    Optional<Person> findById(long personId);

    Optional<Person> findRootPerson();

    Person save(Person person);

    void softDelete(long personId);

    void setRootPerson(long personId);
}