package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.DatePrecision;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;
import family.balling.descendencytracker.domain.enums.Sex;
import family.balling.descendencytracker.domain.enums.SyncStatus;

public class Person {
    private Long personId;
    private String stableUuid;
    private String fsPid;
    private String preferredName = "";
    private String givenNames;
    private String surname;
    private Sex sex = Sex.UNKNOWN;
    private boolean living;
    private String birthDateText;
    private String deathDateText;
    private DatePrecision birthDatePrecision = DatePrecision.UNKNOWN;
    private DatePrecision deathDatePrecision = DatePrecision.UNKNOWN;
    private ReviewedStatus reviewedStatus = ReviewedStatus.NOT_REVIEWED;
    private String notes;
    private boolean root;
    private boolean confirmedNoChildren;
    private boolean confirmedNoSpouse;
    private boolean deleted;
    private String createdAt;
    private String updatedAt;
    private int version = 1;
    private SyncStatus syncStatus = SyncStatus.LOCAL_ONLY;
    private String lastSyncedAt;

    public Person() {
    }

    public Person(Person other) {
        this.personId = other.personId;
        this.stableUuid = other.stableUuid;
        this.fsPid = other.fsPid;
        this.preferredName = other.preferredName;
        this.givenNames = other.givenNames;
        this.surname = other.surname;
        this.sex = other.sex;
        this.living = other.living;
        this.birthDateText = other.birthDateText;
        this.deathDateText = other.deathDateText;
        this.birthDatePrecision = other.birthDatePrecision;
        this.deathDatePrecision = other.deathDatePrecision;
        this.reviewedStatus = other.reviewedStatus;
        this.notes = other.notes;
        this.root = other.root;
        this.confirmedNoChildren = other.confirmedNoChildren;
        this.confirmedNoSpouse = other.confirmedNoSpouse;
        this.deleted = other.deleted;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
        this.version = other.version;
        this.syncStatus = other.syncStatus;
        this.lastSyncedAt = other.lastSyncedAt;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public String getStableUuid() {
        return stableUuid;
    }

    public void setStableUuid(String stableUuid) {
        this.stableUuid = stableUuid;
    }

    public String getFsPid() {
        return fsPid;
    }

    public void setFsPid(String fsPid) {
        this.fsPid = fsPid;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getGivenNames() {
        return givenNames;
    }

    public void setGivenNames(String givenNames) {
        this.givenNames = givenNames;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public boolean isLiving() {
        return living;
    }

    public void setLiving(boolean living) {
        this.living = living;
    }

    public String getBirthDateText() {
        return birthDateText;
    }

    public void setBirthDateText(String birthDateText) {
        this.birthDateText = birthDateText;
    }

    public String getDeathDateText() {
        return deathDateText;
    }

    public void setDeathDateText(String deathDateText) {
        this.deathDateText = deathDateText;
    }

    public DatePrecision getBirthDatePrecision() {
        return birthDatePrecision;
    }

    public void setBirthDatePrecision(DatePrecision birthDatePrecision) {
        this.birthDatePrecision = birthDatePrecision;
    }

    public DatePrecision getDeathDatePrecision() {
        return deathDatePrecision;
    }

    public void setDeathDatePrecision(DatePrecision deathDatePrecision) {
        this.deathDatePrecision = deathDatePrecision;
    }

    public ReviewedStatus getReviewedStatus() {
        return reviewedStatus;
    }

    public void setReviewedStatus(ReviewedStatus reviewedStatus) {
        this.reviewedStatus = reviewedStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isConfirmedNoChildren() {
        return confirmedNoChildren;
    }

    public void setConfirmedNoChildren(boolean confirmedNoChildren) {
        this.confirmedNoChildren = confirmedNoChildren;
    }

    public boolean isConfirmedNoSpouse() {
        return confirmedNoSpouse;
    }

    public void setConfirmedNoSpouse(boolean confirmedNoSpouse) {
        this.confirmedNoSpouse = confirmedNoSpouse;
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

    public String getDisplayName() {
        if (preferredName != null && !preferredName.isBlank()) {
            return preferredName;
        }

        StringBuilder builder = new StringBuilder();

        if (givenNames != null && !givenNames.isBlank()) {
            builder.append(givenNames.trim());
        }

        if (surname != null && !surname.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(surname.trim());
        }

        if (builder.length() > 0) {
            return builder.toString();
        }

        return "(Unnamed Person)";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
