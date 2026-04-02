package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

public class OrdinanceEligibilityRow {
    private final boolean personLevel;
    private final String ordinanceName;
    private final String relatedPersonName;
    private final OrdinanceStatus recordedStatus;
    private final OrdinanceStatus suggestedStatus;
    private final String reason;

    public OrdinanceEligibilityRow(
            boolean personLevel,
            String ordinanceName,
            String relatedPersonName,
            OrdinanceStatus recordedStatus,
            OrdinanceStatus suggestedStatus,
            String reason
    ) {
        this.personLevel = personLevel;
        this.ordinanceName = ordinanceName;
        this.relatedPersonName = relatedPersonName;
        this.recordedStatus = recordedStatus;
        this.suggestedStatus = suggestedStatus;
        this.reason = reason;
    }

    public boolean isPersonLevel() {
        return personLevel;
    }

    public String getOrdinanceName() {
        return ordinanceName;
    }

    public String getRelatedPersonName() {
        return relatedPersonName;
    }

    public OrdinanceStatus getRecordedStatus() {
        return recordedStatus;
    }

    public OrdinanceStatus getSuggestedStatus() {
        return suggestedStatus;
    }

    public String getReason() {
        return reason;
    }
}