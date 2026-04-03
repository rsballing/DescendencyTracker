package family.balling.descendencytracker.domain;

public class ParentChildLink {
    private Long linkId;
    private String stableUuid;
    private Long parentPersonId;
    private String parentDisplayName;
    private Long childPersonId;
    private String childDisplayName;
    private Integer childOrder;
    private String notes;
    private boolean deleted;
    private String createdAt;
    private String updatedAt;

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public String getStableUuid() {
        return stableUuid;
    }

    public void setStableUuid(String stableUuid) {
        this.stableUuid = stableUuid;
    }

    public Long getParentPersonId() {
        return parentPersonId;
    }

    public void setParentPersonId(Long parentPersonId) {
        this.parentPersonId = parentPersonId;
    }

    public String getParentDisplayName() {
        return parentDisplayName;
    }

    public void setParentDisplayName(String parentDisplayName) {
        this.parentDisplayName = parentDisplayName;
    }

    public Long getChildPersonId() {
        return childPersonId;
    }

    public void setChildPersonId(Long childPersonId) {
        this.childPersonId = childPersonId;
    }

    public String getChildDisplayName() {
        return childDisplayName;
    }

    public void setChildDisplayName(String childDisplayName) {
        this.childDisplayName = childDisplayName;
    }

    public Integer getChildOrder() {
        return childOrder;
    }

    public void setChildOrder(Integer childOrder) {
        this.childOrder = childOrder;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}