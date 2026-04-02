package family.balling.descendencytracker.repository;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

import java.util.List;

public interface RelationshipRepository {
    List<ParentChildLink> findParentsForChild(long childPersonId);

    List<ParentChildLink> findChildrenForParent(long parentPersonId);

    List<SpouseLink> findSpousesForPerson(long personId);

    ParentChildLink addParentChild(long parentPersonId, long childPersonId, Integer childOrder, String notes);

    ParentChildLink updateParentChild(long linkId, long parentPersonId, long childPersonId, Integer childOrder, String notes);

    void softDeleteParentChild(long linkId);

    SpouseLink addSpouse(
            long personAId,
            long personBId,
            String marriageDateText,
            String marriageNotes,
            OrdinanceStatus sealingToSpouseStatus,
            String sealingStatusDate,
            String sealingNotes
    );

    SpouseLink updateSpouse(
            long spouseLinkId,
            long personAId,
            long personBId,
            String marriageDateText,
            String marriageNotes,
            OrdinanceStatus sealingToSpouseStatus,
            String sealingStatusDate,
            String sealingNotes
    );

    void softDeleteSpouse(long spouseLinkId);
}