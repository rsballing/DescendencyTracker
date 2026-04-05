package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.domain.enums.SyncStatus;

public class PersonOrdinanceStatus {
    private Long personId;
    private OrdinanceStatus baptismStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus confirmationStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus initiatoryStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus endowmentStatus = OrdinanceStatus.UNKNOWN;
    private OrdinanceStatus sealedToParentsStatus = OrdinanceStatus.UNKNOWN;
    private String ordinanceNotes;
    private String updatedAt;
    private int version = 1;
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;
    private String lastSyncedAt;

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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = Math.max(1, version);
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus == null ? SyncStatus.LOCAL_ONLY : syncStatus;
    }

    public String getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(String lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
