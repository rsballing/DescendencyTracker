package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.LineStewardshipStatus;
import family.balling.descendencytracker.domain.enums.SyncStatus;

public class LineStewardship {
    private Long ancestorPersonId;
    private LineStewardshipStatus stewardshipStatus = LineStewardshipStatus.UNASSIGNED;
    private String notes;
    private String updatedAt;
    private int version = 1;
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;
    private String lastSyncedAt;

    public Long getAncestorPersonId() {
        return ancestorPersonId;
    }

    public void setAncestorPersonId(Long ancestorPersonId) {
        this.ancestorPersonId = ancestorPersonId;
    }

    public LineStewardshipStatus getStewardshipStatus() {
        return stewardshipStatus;
    }

    public void setStewardshipStatus(LineStewardshipStatus stewardshipStatus) {
        this.stewardshipStatus = stewardshipStatus == null
                ? LineStewardshipStatus.UNASSIGNED
                : stewardshipStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
