package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.repository.PersonRepository;
import family.balling.descendencytracker.repository.RelationshipRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RelationshipService {
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;

    public RelationshipService(PersonRepository personRepository, RelationshipRepository relationshipRepository) {
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
    }

    public List<ParentChildLink> getParentsForPerson(long personId) {
        return relationshipRepository.findParentsForChild(personId);
    }

    public List<ParentChildLink> getChildrenForPerson(long personId) {
        return relationshipRepository.findChildrenForParent(personId);
    }

    public List<SpouseLink> getSpousesForPerson(long personId) {
        return relationshipRepository.findSpousesForPerson(personId);
    }

    public ParentChildLink addParent(long childPersonId, long parentPersonId, Integer childOrder, String notes) {
        validateDistinctPeople(parentPersonId, childPersonId, "A person cannot be their own parent.");
        validatePersonExists(parentPersonId);
        validatePersonExists(childPersonId);
        validateChildOrder(childOrder);
        validateChildHasParentCapacity(childPersonId, null);
        validateNoDuplicateParentChild(parentPersonId, childPersonId, null);
        validateNoParentChildCycle(parentPersonId, childPersonId);

        return relationshipRepository.addParentChild(
                parentPersonId,
                childPersonId,
                childOrder,
                trimToNull(notes)
        );
    }

    public ParentChildLink addChild(long parentPersonId, long childPersonId, Integer childOrder, String notes) {
        validateDistinctPeople(parentPersonId, childPersonId, "A person cannot be their own child.");
        validatePersonExists(parentPersonId);
        validatePersonExists(childPersonId);
        validateChildOrder(childOrder);
        validateChildHasParentCapacity(childPersonId, null);
        validateNoDuplicateParentChild(parentPersonId, childPersonId, null);
        validateNoParentChildCycle(parentPersonId, childPersonId);

        return relationshipRepository.addParentChild(
                parentPersonId,
                childPersonId,
                childOrder,
                trimToNull(notes)
        );
    }

    public ParentChildLink updateParentLink(long linkId, long childPersonId, long parentPersonId, Integer childOrder, String notes) {
        validateDistinctPeople(parentPersonId, childPersonId, "A person cannot be their own parent.");
        validatePersonExists(parentPersonId);
        validatePersonExists(childPersonId);
        validateChildOrder(childOrder);
        validateChildHasParentCapacity(childPersonId, linkId);
        validateNoDuplicateParentChild(parentPersonId, childPersonId, linkId);
        validateNoParentChildCycle(parentPersonId, childPersonId);

        return relationshipRepository.updateParentChild(
                linkId,
                parentPersonId,
                childPersonId,
                childOrder,
                trimToNull(notes)
        );
    }

    public ParentChildLink updateChildLink(long linkId, long parentPersonId, long childPersonId, Integer childOrder, String notes) {
        validateDistinctPeople(parentPersonId, childPersonId, "A person cannot be their own child.");
        validatePersonExists(parentPersonId);
        validatePersonExists(childPersonId);
        validateChildOrder(childOrder);
        validateChildHasParentCapacity(childPersonId, linkId);
        validateNoDuplicateParentChild(parentPersonId, childPersonId, linkId);
        validateNoParentChildCycle(parentPersonId, childPersonId);

        return relationshipRepository.updateParentChild(
                linkId,
                parentPersonId,
                childPersonId,
                childOrder,
                trimToNull(notes)
        );
    }

    public void deleteParentLink(long linkId) {
        relationshipRepository.softDeleteParentChild(linkId);
    }

    public void deleteChildLink(long linkId) {
        relationshipRepository.softDeleteParentChild(linkId);
    }

    public SpouseLink addSpouse(
            long personId,
            long spouseId,
            String marriageDateText,
            String marriageNotes,
            OrdinanceStatus sealingToSpouseStatus,
            String sealingStatusDate,
            String sealingNotes
    ) {
        validateDistinctPeople(personId, spouseId, "A person cannot be their own spouse.");
        validatePersonExists(personId);
        validatePersonExists(spouseId);
        validateNoDuplicateSpouse(personId, spouseId, null);

        return relationshipRepository.addSpouse(
                personId,
                spouseId,
                trimToNull(marriageDateText),
                trimToNull(marriageNotes),
                normalizeStatus(sealingToSpouseStatus),
                trimToNull(sealingStatusDate),
                trimToNull(sealingNotes)
        );
    }

    public SpouseLink updateSpouseLink(
            long spouseLinkId,
            long personId,
            long spouseId,
            String marriageDateText,
            String marriageNotes,
            OrdinanceStatus sealingToSpouseStatus,
            String sealingStatusDate,
            String sealingNotes
    ) {
        validateDistinctPeople(personId, spouseId, "A person cannot be their own spouse.");
        validatePersonExists(personId);
        validatePersonExists(spouseId);
        validateNoDuplicateSpouse(personId, spouseId, spouseLinkId);

        return relationshipRepository.updateSpouse(
                spouseLinkId,
                personId,
                spouseId,
                trimToNull(marriageDateText),
                trimToNull(marriageNotes),
                normalizeStatus(sealingToSpouseStatus),
                trimToNull(sealingStatusDate),
                trimToNull(sealingNotes)
        );
    }

    public void deleteSpouseLink(long spouseLinkId) {
        relationshipRepository.softDeleteSpouse(spouseLinkId);
    }

    private void validateDistinctPeople(long firstPersonId, long secondPersonId, String message) {
        if (firstPersonId == secondPersonId) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePersonExists(long personId) {
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));
    }

    private void validateChildOrder(Integer childOrder) {
        if (childOrder != null && childOrder <= 0) {
            throw new IllegalArgumentException("Child order must be a positive number when provided.");
        }
    }

    private void validateNoDuplicateParentChild(long parentPersonId, long childPersonId, Long currentLinkId) {
        List<ParentChildLink> existingParents = relationshipRepository.findParentsForChild(childPersonId);

        for (ParentChildLink link : existingParents) {
            if (link.getParentPersonId() == null || link.getLinkId() == null) {
                continue;
            }

            boolean sameParent = link.getParentPersonId().equals(parentPersonId);
            boolean sameLink = currentLinkId != null && link.getLinkId().equals(currentLinkId);

            if (sameParent && !sameLink) {
                throw new IllegalArgumentException("That parent-child relationship already exists.");
            }
        }
    }

    private void validateChildHasParentCapacity(long childPersonId, Long currentLinkId) {
        long activeParentCount = relationshipRepository.findParentsForChild(childPersonId).stream()
                .filter(link -> link.getLinkId() != null)
                .filter(link -> currentLinkId == null || !link.getLinkId().equals(currentLinkId))
                .count();

        if (activeParentCount >= 2) {
            throw new IllegalArgumentException("A child cannot have more than two parents.");
        }
    }

    private void validateNoDuplicateSpouse(long personId, long spouseId, Long currentSpouseLinkId) {
        List<SpouseLink> existingSpouses = relationshipRepository.findSpousesForPerson(personId);

        for (SpouseLink link : existingSpouses) {
            if (link.getSpouseLinkId() == null) {
                continue;
            }

            Long otherPersonId = link.getOtherPersonId(personId);
            boolean sameSpouse = otherPersonId != null && otherPersonId.equals(spouseId);
            boolean sameLink = currentSpouseLinkId != null && link.getSpouseLinkId().equals(currentSpouseLinkId);

            if (sameSpouse && !sameLink) {
                throw new IllegalArgumentException("That spouse relationship already exists.");
            }
        }
    }

    private void validateNoParentChildCycle(long proposedParentPersonId, long proposedChildPersonId) {
        if (isDescendant(proposedChildPersonId, proposedParentPersonId, new HashSet<>())) {
            throw new IllegalArgumentException(
                    "That parent-child relationship would create a cycle in the family tree."
            );
        }
    }

    private boolean isDescendant(long currentAncestorId, long targetDescendantId, Set<Long> visited) {
        if (!visited.add(currentAncestorId)) {
            return false;
        }

        List<ParentChildLink> childLinks = relationshipRepository.findChildrenForParent(currentAncestorId);
        for (ParentChildLink childLink : childLinks) {
            Long childId = childLink.getChildPersonId();
            if (childId == null) {
                continue;
            }

            if (childId == targetDescendantId) {
                return true;
            }

            if (isDescendant(childId, targetDescendantId, visited)) {
                return true;
            }
        }

        return false;
    }

    private OrdinanceStatus normalizeStatus(OrdinanceStatus status) {
        return status == null ? OrdinanceStatus.UNKNOWN : status;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
