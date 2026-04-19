package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.repository.OrdinanceRepository;

public class OrdinanceService {
    private final OrdinanceRepository ordinanceRepository;

    public OrdinanceService(OrdinanceRepository ordinanceRepository) {
        this.ordinanceRepository = ordinanceRepository;
    }

    public PersonOrdinanceStatus getOrCreateForPerson(long personId) {
        return ordinanceRepository.findByPersonId(personId).orElseGet(() -> {
            PersonOrdinanceStatus status = new PersonOrdinanceStatus();
            status.setPersonId(personId);
            status.setBaptismStatus(OrdinanceStatus.UNKNOWN);
            status.setConfirmationStatus(OrdinanceStatus.UNKNOWN);
            status.setInitiatoryStatus(OrdinanceStatus.UNKNOWN);
            status.setEndowmentStatus(OrdinanceStatus.UNKNOWN);
            status.setSealedToParentsStatus(OrdinanceStatus.UNKNOWN);
            return status;
        });
    }

    public PersonOrdinanceStatus save(PersonOrdinanceStatus status) {
        normalize(status);
        if (status.getPersonId() == null) {
            throw new IllegalArgumentException("A person must be selected before saving ordinance status.");
        }
        return ordinanceRepository.save(status);
    }

    private void normalize(PersonOrdinanceStatus status) {
        if (status.getBaptismStatus() == null) {
            status.setBaptismStatus(OrdinanceStatus.UNKNOWN);
        }
        if (status.getConfirmationStatus() == null) {
            status.setConfirmationStatus(OrdinanceStatus.UNKNOWN);
        }
        if (status.getInitiatoryStatus() == null) {
            status.setInitiatoryStatus(OrdinanceStatus.UNKNOWN);
        }
        if (status.getEndowmentStatus() == null) {
            status.setEndowmentStatus(OrdinanceStatus.UNKNOWN);
        }
        if (status.getSealedToParentsStatus() == null) {
            status.setSealedToParentsStatus(OrdinanceStatus.UNKNOWN);
        }
        status.setOrdinanceNotes(trimToNull(status.getOrdinanceNotes()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
