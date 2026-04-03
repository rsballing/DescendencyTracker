package family.balling.descendencytracker.repository;

import family.balling.descendencytracker.domain.PersonOrdinanceStatus;

import java.util.Optional;

public interface OrdinanceRepository {
    Optional<PersonOrdinanceStatus> findByPersonId(long personId);

    PersonOrdinanceStatus save(PersonOrdinanceStatus status);
}