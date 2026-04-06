package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.repository.PersonRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonService {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(\\d{4})\\b");
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

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<Person> getAllPeople() {
        return personRepository.findAllActive();
    }

    public Optional<Person> getRootPerson() {
        return personRepository.findRootPerson();
    }

    public Optional<Person> getPersonById(long personId) {
        return personRepository.findById(personId);
    }

    public Person savePerson(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person cannot be null.");
        }

        normalize(person);
        validate(person);

        return personRepository.save(person);
    }

    public void deletePerson(long personId) {
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        personRepository.softDelete(personId);
    }

    public void setRootPerson(long personId) {
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        personRepository.setRootPerson(personId);
    }

    private void normalize(Person person) {
        person.setPreferredName(clean(person.getPreferredName()));
        person.setFsPid(normalizeFsPid(person.getFsPid()));
        person.setGivenNames(clean(person.getGivenNames()));
        person.setSurname(clean(person.getSurname()));
        person.setBirthDateText(normalizeDateText(person.getBirthDateText()));
        person.setDeathDateText(normalizeDateText(person.getDeathDateText()));
        person.setNotes(clean(person.getNotes()));
    }

    private void validate(Person person) {
        if (person.getPreferredName() == null) {
            throw new IllegalArgumentException("Preferred name is required.");
        }

        if (person.getFsPid() != null && !person.getFsPid().matches("^[A-Z0-9]{4}-[A-Z0-9]{3}$")) {
            throw new IllegalArgumentException("FamilySearch PID must be entered as four characters plus three characters.");
        }

        if (person.isLiving() && person.getDeathDateText() != null) {
            throw new IllegalArgumentException("A living person cannot have a death date.");
        }

        Integer birthYear = extractYear(person.getBirthDateText());
        Integer deathYear = extractYear(person.getDeathDateText());

        if (birthYear != null && deathYear != null && deathYear < birthYear) {
            throw new IllegalArgumentException("Death year cannot be earlier than birth year.");
        }
    }

    private Integer extractYear(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = YEAR_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeFsPid(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }

        String compact = cleaned.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ENGLISH);
        if (compact.length() != 7) {
            return cleaned.toUpperCase(Locale.ENGLISH);
        }

        return compact.substring(0, 4) + "-" + compact.substring(4);
    }

    private String normalizeDateText(String value) {
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

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
