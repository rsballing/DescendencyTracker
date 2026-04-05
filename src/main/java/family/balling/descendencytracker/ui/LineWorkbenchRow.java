package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;

record LineWorkbenchRow(
        String lineageLabel,
        int generation,
        Long personId,
        String personName,
        String fsPid,
        String ordinanceName,
        String relatedPersonName,
        OrdinanceStatus recordedStatus,
        OrdinanceStatus suggestedStatus,
        String reason,
        SpouseLink spouseLink
) {
}
