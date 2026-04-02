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

    public WorkQueueRow(
            Long personId,
            String displayName,
            String fsPid,
            OrdinanceStatus queueBucket,
            String triggerLabel,
            String reason,
            int parentCount,
            int childCount,
            int spouseCount
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
}