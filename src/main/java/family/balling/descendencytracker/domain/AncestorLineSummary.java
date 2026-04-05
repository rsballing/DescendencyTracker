package family.balling.descendencytracker.domain;

import family.balling.descendencytracker.domain.enums.AncestorBadgeStatus;
import family.balling.descendencytracker.domain.enums.LineStewardshipStatus;

import java.time.LocalDate;

public class AncestorLineSummary {
    private Long ancestorPersonId;
    private String ancestorDisplayName;
    private AncestorBadgeStatus badgeStatus = AncestorBadgeStatus.NOT_REVIEWED;
    private int openCount;
    private int openingSoonCount;
    private int waiting110Count;
    private int unresolvedCount;
    private int completeCount;
    private int notReviewedCount;
    private int totalTrackedCount;
    private LocalDate nextAvailableDate;
    private String summaryReason;
    private LineStewardshipStatus stewardshipStatus = LineStewardshipStatus.UNASSIGNED;
    private String stewardshipNotes;

    public Long getAncestorPersonId() {
        return ancestorPersonId;
    }

    public void setAncestorPersonId(Long ancestorPersonId) {
        this.ancestorPersonId = ancestorPersonId;
    }

    public String getAncestorDisplayName() {
        return ancestorDisplayName;
    }

    public void setAncestorDisplayName(String ancestorDisplayName) {
        this.ancestorDisplayName = ancestorDisplayName;
    }

    public AncestorBadgeStatus getBadgeStatus() {
        return badgeStatus;
    }

    public void setBadgeStatus(AncestorBadgeStatus badgeStatus) {
        this.badgeStatus = badgeStatus == null ? AncestorBadgeStatus.NOT_REVIEWED : badgeStatus;
    }

    public int getOpenCount() {
        return openCount;
    }

    public void setOpenCount(int openCount) {
        this.openCount = Math.max(0, openCount);
    }

    public int getOpeningSoonCount() {
        return openingSoonCount;
    }

    public void setOpeningSoonCount(int openingSoonCount) {
        this.openingSoonCount = Math.max(0, openingSoonCount);
    }

    public int getWaiting110Count() {
        return waiting110Count;
    }

    public void setWaiting110Count(int waiting110Count) {
        this.waiting110Count = Math.max(0, waiting110Count);
    }

    public int getUnresolvedCount() {
        return unresolvedCount;
    }

    public void setUnresolvedCount(int unresolvedCount) {
        this.unresolvedCount = Math.max(0, unresolvedCount);
    }

    public int getCompleteCount() {
        return completeCount;
    }

    public void setCompleteCount(int completeCount) {
        this.completeCount = Math.max(0, completeCount);
    }

    public int getNotReviewedCount() {
        return notReviewedCount;
    }

    public void setNotReviewedCount(int notReviewedCount) {
        this.notReviewedCount = Math.max(0, notReviewedCount);
    }

    public int getTotalTrackedCount() {
        return totalTrackedCount;
    }

    public void setTotalTrackedCount(int totalTrackedCount) {
        this.totalTrackedCount = Math.max(0, totalTrackedCount);
    }

    public LocalDate getNextAvailableDate() {
        return nextAvailableDate;
    }

    public void setNextAvailableDate(LocalDate nextAvailableDate) {
        this.nextAvailableDate = nextAvailableDate;
    }

    public String getSummaryReason() {
        return summaryReason;
    }

    public void setSummaryReason(String summaryReason) {
        this.summaryReason = summaryReason;
    }

    public LineStewardshipStatus getStewardshipStatus() {
        return stewardshipStatus;
    }

    public void setStewardshipStatus(LineStewardshipStatus stewardshipStatus) {
        this.stewardshipStatus = stewardshipStatus == null
                ? LineStewardshipStatus.UNASSIGNED
                : stewardshipStatus;
    }

    public String getStewardshipNotes() {
        return stewardshipNotes;
    }

    public void setStewardshipNotes(String stewardshipNotes) {
        this.stewardshipNotes = stewardshipNotes;
    }
}
