package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

public class WorkQueueRow {
    private final Long personId;
    private final String displayName;
    private final String fsPid;
    private final OrdinanceStatus queueBucket;
    private final String triggerLabel;
    private final String reason;
    private final int parentCount;
    private final int childCount;
    private final int spouseCount;
    private final boolean hasOpenOrdinances;
    private final boolean hasReservedOrdinances;
    private final boolean hasConnectedParents;
    private final boolean hasConnectedChildren;
    private final boolean hasConnectedSpouses;
    private final boolean confirmedNoChildren;
    private final boolean confirmedNoSpouse;

    public WorkQueueRow(
            Long personId,
            String displayName,
            String fsPid,
            OrdinanceStatus queueBucket,
            String triggerLabel,
            String reason,
            int parentCount,
            int childCount,
            int spouseCount,
            boolean hasOpenOrdinances,
            boolean hasReservedOrdinances,
            boolean hasConnectedParents,
            boolean hasConnectedChildren,
            boolean hasConnectedSpouses,
            boolean confirmedNoChildren,
            boolean confirmedNoSpouse
    ) {
        this.personId = personId;
        this.displayName = displayName;
        this.fsPid = fsPid;
        this.queueBucket = queueBucket;
        this.triggerLabel = triggerLabel;
        this.reason = reason;
        this.parentCount = parentCount;
        this.childCount = childCount;
        this.spouseCount = spouseCount;
        this.hasOpenOrdinances = hasOpenOrdinances;
        this.hasReservedOrdinances = hasReservedOrdinances;
        this.hasConnectedParents = hasConnectedParents;
        this.hasConnectedChildren = hasConnectedChildren;
        this.hasConnectedSpouses = hasConnectedSpouses;
        this.confirmedNoChildren = confirmedNoChildren;
        this.confirmedNoSpouse = confirmedNoSpouse;
    }

    public Long getPersonId() {
        return personId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFsPid() {
        return fsPid;
    }

    public OrdinanceStatus getQueueBucket() {
        return queueBucket;
    }

    public String getTriggerLabel() {
        return triggerLabel;
    }

    public String getReason() {
        return reason;
    }

    public int getParentCount() {
        return parentCount;
    }

    public int getChildCount() {
        return childCount;
    }

    public int getSpouseCount() {
        return spouseCount;
    }

    public boolean hasOpenOrdinances() {
        return hasOpenOrdinances;
    }

    public boolean hasReservedOrdinances() {
        return hasReservedOrdinances;
    }

    public boolean hasConnectedParents() {
        return hasConnectedParents;
    }

    public boolean hasConnectedChildren() {
        return hasConnectedChildren;
    }

    public boolean hasConnectedSpouses() {
        return hasConnectedSpouses;
    }

    public boolean isConfirmedNoChildren() {
        return confirmedNoChildren;
    }

    public boolean isConfirmedNoSpouse() {
        return confirmedNoSpouse;
    }
}
