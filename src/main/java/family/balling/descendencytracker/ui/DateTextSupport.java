package family.balling.descendencytracker.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

final class DateTextSupport {
    private static final DateTimeFormatter STANDARD_DATE_FORMAT =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("d MMM uuuu")
                    .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter STANDARD_MONTH_YEAR_FORMAT =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMM uuuu")
                    .toFormatter(Locale.ENGLISH);
    private static final List<DateTimeFormatter> FULL_DATE_INPUT_FORMATS = List.of(
            formatter("M/d/uuuu"),
            formatter("M-d-uuuu"),
            formatter("uuuu-M-d"),
            formatter("d MMM uuuu"),
            formatter("d MMMM uuuu"),
            formatter("MMM d uuuu"),
            formatter("MMMM d uuuu"),
            formatter("MMM d, uuuu"),
            formatter("MMMM d, uuuu")
    );
    private static final List<DateTimeFormatter> MONTH_YEAR_INPUT_FORMATS = List.of(
            formatter("M/uuuu"),
            formatter("M-uuuu"),
            formatter("MMM uuuu"),
            formatter("MMMM uuuu"),
            formatter("uuuu-MM")
    );

    private DateTextSupport() {
    }

    static String normalizeDateText(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }

        if (cleaned.matches("^\\d{4}$")) {
            return cleaned;
        }

        for (DateTimeFormatter formatter : FULL_DATE_INPUT_FORMATS) {
            try {
                return STANDARD_DATE_FORMAT.format(LocalDate.parse(cleaned, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next supported input format.
            }
        }

        for (DateTimeFormatter formatter : MONTH_YEAR_INPUT_FORMATS) {
            try {
                return STANDARD_MONTH_YEAR_FORMAT.format(YearMonth.parse(cleaned, formatter));
            } catch (DateTimeParseException ignored) {
                // Leave unparseable values unchanged so free-text dates still work.
            }
        }

        return cleaned;
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
