package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

import java.util.List;

final class OrdinanceStatusChoice {
    private static final List<OrdinanceStatusChoice> ALL = List.of(
            new OrdinanceStatusChoice("Complete", OrdinanceStatus.COMPLETE, false),
            new OrdinanceStatusChoice("Open", OrdinanceStatus.OPEN, false),
            new OrdinanceStatusChoice("Reserved", OrdinanceStatus.OPEN, true),
            new OrdinanceStatusChoice("Blocked 110", OrdinanceStatus.BLOCKED_110, false),
            new OrdinanceStatusChoice("Soon 1Y", OrdinanceStatus.SOON_1Y, false),
            new OrdinanceStatusChoice("Soon 2Y", OrdinanceStatus.SOON_2Y, false),
            new OrdinanceStatusChoice("Soon 5Y", OrdinanceStatus.SOON_5Y, false),
            new OrdinanceStatusChoice("Soon 10Y", OrdinanceStatus.SOON_10Y, false),
            new OrdinanceStatusChoice("Not Applicable", OrdinanceStatus.NOT_APPLICABLE, false),
            new OrdinanceStatusChoice("Unknown", OrdinanceStatus.UNKNOWN, false)
    );

    private final String label;
    private final OrdinanceStatus status;
    private final boolean reserved;

    private OrdinanceStatusChoice(String label, OrdinanceStatus status, boolean reserved) {
        this.label = label;
        this.status = status;
        this.reserved = reserved;
    }

    static List<OrdinanceStatusChoice> all() {
        return ALL;
    }

    static OrdinanceStatusChoice of(OrdinanceStatus status, boolean reserved) {
        if (reserved) {
            return ALL.stream()
                    .filter(choice -> choice.reserved)
                    .findFirst()
                    .orElseThrow();
        }

        return ALL.stream()
                .filter(choice -> !choice.reserved && choice.status == safeStatus(status))
                .findFirst()
                .orElseGet(() -> new OrdinanceStatusChoice(safeStatus(status).name(), safeStatus(status), false));
    }

    static OrdinanceStatusChoice fromShortcut(OrdinanceStatus status) {
        return of(status, false);
    }

    static OrdinanceStatusChoice reservedChoice() {
        return of(OrdinanceStatus.OPEN, true);
    }

    OrdinanceStatus status() {
        return status;
    }

    boolean isReserved() {
        return reserved;
    }

    @Override
    public String toString() {
        return label;
    }

    private static OrdinanceStatus safeStatus(OrdinanceStatus status) {
        return status == null ? OrdinanceStatus.UNKNOWN : status;
    }
}
