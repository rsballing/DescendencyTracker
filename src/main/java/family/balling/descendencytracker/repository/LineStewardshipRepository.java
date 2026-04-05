package family.balling.descendencytracker.repository;

import family.balling.descendencytracker.domain.LineStewardship;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LineStewardshipRepository {
    Optional<LineStewardship> findByAncestorPersonId(long ancestorPersonId);

    Map<Long, LineStewardship> findByAncestorPersonIds(List<Long> ancestorPersonIds);

    LineStewardship save(LineStewardship stewardship);
}
