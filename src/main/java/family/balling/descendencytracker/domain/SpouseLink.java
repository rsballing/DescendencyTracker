package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.domain.enums.SyncStatus;

public class SpouseLink {
    private Long spouseLinkId;
    private String stableUuid;
    private Long personAId;
    private String personADisplayName;
    private Long personBId;
    private String personBDisplayName;
    private String marriageDateText;
    private String marriageNotes;
    private OrdinanceStatus sealingToSpouseStatus = OrdinanceStatus.UNKNOWN;
    private String sealingStatusDate;
    private String sealingNotes;
    private boolean deleted;
    private String createdAt;
    private String updatedAt;
    private int version = 1;
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;
    private String lastSyncedAt;

    public Long getSpouseLinkId() {
        return spouseLinkId;
    }

    public void setSpouseLinkId(Long spouseLinkId) {
        this.spouseLinkId = spouseLinkId;
    }

    public String getStableUuid() {
        return stableUuid;
    }

    public void setStableUuid(String stableUuid) {
        this.stableUuid = stableUuid;
    }

    public Long getPersonAId() {
        return personAId;
    }

    public void setPersonAId(Long personAId) {
        this.personAId = personAId;
    }

    public String getPersonADisplayName() {
        return personADisplayName;
    }

    public void setPersonADisplayName(String personADisplayName) {
        this.personADisplayName = personADisplayName;
    }

    public Long getPersonBId() {
        return personBId;
    }

    public void setPersonBId(Long personBId) {
        this.personBId = personBId;
    }

    public String getPersonBDisplayName() {
        return personBDisplayName;
    }

    public void setPersonBDisplayName(String personBDisplayName) {
        this.personBDisplayName = personBDisplayName;
    }

    public String getMarriageDateText() {
        return marriageDateText;
    }

    public void setMarriageDateText(String marriageDateText) {
        this.marriageDateText = marriageDateText;
    }

    public String getMarriageNotes() {
        return marriageNotes;
    }

    public void setMarriageNotes(String marriageNotes) {
        this.marriageNotes = marriageNotes;
    }

    public OrdinanceStatus getSealingToSpouseStatus() {
        return sealingToSpouseStatus;
    }

    public void setSealingToSpouseStatus(OrdinanceStatus sealingToSpouseStatus) {
        this.sealingToSpouseStatus = sealingToSpouseStatus;
    }

    public String getSealingStatusDate() {
        return sealingStatusDate;
    }

    public void setSealingStatusDate(String sealingStatusDate) {
        this.sealingStatusDate = sealingStatusDate;
    }

    public String getSealingNotes() {
        return sealingNotes;
    }

    public void setSealingNotes(String sealingNotes) {
        this.sealingNotes = sealingNotes;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
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

    public Long getOtherPersonId(long selectedPersonId) {
        return personAId != null && personAId == selectedPersonId ? personBId : personAId;
    }

    public String getOtherPersonDisplayName(long selectedPersonId) {
        return personAId != null && personAId == selectedPersonId ? personBDisplayName : personADisplayName;
    }
}
