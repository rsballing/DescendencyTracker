package family.balling.descendencytracker.domain.enums;

public enum SyncStatus {
    LOCAL_ONLY,
    DIRTY_UPDATE,
    DIRTY_DELETE,
    SYNCED,
    CONFLICT,
    ERROR
}
