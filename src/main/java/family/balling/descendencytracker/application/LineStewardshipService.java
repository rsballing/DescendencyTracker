package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.LineStewardship;
import family.balling.descendencytracker.domain.enums.LineStewardshipStatus;
import family.balling.descendencytracker.repository.LineStewardshipRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LineStewardshipService {
    private final LineStewardshipRepository lineStewardshipRepository;

    public LineStewardshipService(LineStewardshipRepository lineStewardshipRepository) {
        this.lineStewardshipRepository = lineStewardshipRepository;
    }

    public LineStewardship getOrCreateForAncestor(long ancestorPersonId) {
        return lineStewardshipRepository.findByAncestorPersonId(ancestorPersonId)
                .orElseGet(() -> {
                    LineStewardship stewardship = new LineStewardship();
                    stewardship.setAncestorPersonId(ancestorPersonId);
                    stewardship.setStewardshipStatus(LineStewardshipStatus.UNASSIGNED);
                    return stewardship;
                });
    }

    public Map<Long, LineStewardship> getByAncestorIds(List<Long> ancestorPersonIds) {
        if (ancestorPersonIds == null || ancestorPersonIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, LineStewardship> found = new LinkedHashMap<>(
                lineStewardshipRepository.findByAncestorPersonIds(ancestorPersonIds)
        );

        for (Long ancestorPersonId : ancestorPersonIds) {
            if (ancestorPersonId == null || found.containsKey(ancestorPersonId)) {
                continue;
            }

            LineStewardship stewardship = new LineStewardship();
            stewardship.setAncestorPersonId(ancestorPersonId);
            stewardship.setStewardshipStatus(LineStewardshipStatus.UNASSIGNED);
            found.put(ancestorPersonId, stewardship);
        }

        return found;
    }

    public LineStewardship save(LineStewardship stewardship) {
        if (stewardship == null || stewardship.getAncestorPersonId() == null) {
            throw new IllegalArgumentException("An ancestor line must be selected before saving stewardship.");
        }

        if (stewardship.getStewardshipStatus() == null) {
            stewardship.setStewardshipStatus(LineStewardshipStatus.UNASSIGNED);
        }
        stewardship.setNotes(trimToNull(stewardship.getNotes()));

        return lineStewardshipRepository.save(stewardship);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
