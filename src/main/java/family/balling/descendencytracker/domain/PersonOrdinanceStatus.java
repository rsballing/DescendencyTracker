package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

public class PersonOrdinanceStatus {
    private Long personId;
    private OrdinanceStatus baptismStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus confirmationStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus initiatoryStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus endowmentStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus sealedToParentsStatus = OrdinanceStatus.UNKNOWN;
    private String ordinanceNotes;
    private String updatedAt;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public OrdinanceStatus getBaptismStatus() {
        return baptismStatus;
    }

    public void setBaptismStatus(OrdinanceStatus baptismStatus) {
        this.baptismStatus = baptismStatus;
    }

    public OrdinanceStatus getConfirmationStatus() {
        return confirmationStatus;
    }

    public void setConfirmationStatus(OrdinanceStatus confirmationStatus) {
        this.confirmationStatus = confirmationStatus;
    }

    public OrdinanceStatus getInitiatoryStatus() {
        return initiatoryStatus;
    }

    public void setInitiatoryStatus(OrdinanceStatus initiatoryStatus) {
        this.initiatoryStatus = initiatoryStatus;
    }

    public OrdinanceStatus getEndowmentStatus() {
        return endowmentStatus;
    }

    public void setEndowmentStatus(OrdinanceStatus endowmentStatus) {
        this.endowmentStatus = endowmentStatus;
    }

    public OrdinanceStatus getSealedToParentsStatus() {
        return sealedToParentsStatus;
    }

    public void setSealedToParentsStatus(OrdinanceStatus sealedToParentsStatus) {
        this.sealedToParentsStatus = sealedToParentsStatus;
    }

    public String getOrdinanceNotes() {
        return ordinanceNotes;
    }

    public void setOrdinanceNotes(String ordinanceNotes) {
        this.ordinanceNotes = ordinanceNotes;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}