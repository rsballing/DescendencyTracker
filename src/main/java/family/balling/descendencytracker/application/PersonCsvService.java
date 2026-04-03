package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.enums.DatePrecision;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;
import family.balling.descendencytracker.domain.enums.Sex;
import family.balling.descendencytracker.domain.enums.StewardshipStatus;
import family.balling.descendencytracker.domain.enums.SyncStatus;
import family.balling.descendencytracker.repository.PersonRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PersonCsvService {
    private static final List<String> HEADERS = List.of(
            "stable_uuid",
            "fs_pid",
            "preferred_name",
            "given_names",
            "surname",
            "sex",
            "is_living",
            "birth_date_text",
            "death_date_text",
            "birth_date_precision",
            "death_date_precision",
            "reviewed_status",
            "last_reviewed_on",
            "stewardship_status",
            "notes",
            "is_root",
            "is_deleted",
            "created_at",
            "updated_at",
            "deleted_at",
            "version",
            "sync_status",
            "last_synced_at",
            "last_modified_by_device"
    );

    private final PersonRepository personRepository;
    private final PersonService personService;

    public PersonCsvService(PersonRepository personRepository, PersonService personService) {
        this.personRepository = personRepository;
        this.personService = personService;
    }

    public Path exportPeople(Path targetPath) {
        if (targetPath == null) {
            throw new IllegalArgumentException("A target CSV file must be selected.");
        }

        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            List<String> lines = new ArrayList<>();
            lines.add(String.join(",", HEADERS));
            for (Person person : personRepository.findAllActive()) {
                lines.add(toCsvLine(person));
            }

            Files.write(targetPath, lines, StandardCharsets.UTF_8);
            return targetPath.toAbsolutePath();
        } catch (IOException ex) {
            throw new RuntimeException("Could not export people to CSV.", ex);
        }
    }

    public int importPeople(Path sourcePath) {
        if (sourcePath == null || !Files.exists(sourcePath)) {
            throw new IllegalArgumentException("The selected CSV file does not exist.");
        }

        try {
            List<String> lines = Files.readAllLines(sourcePath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return 0;
            }

            List<String> headers = parseCsvLine(lines.get(0));
            int imported = 0;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }

                Person person = mapPerson(headers, parseCsvLine(line));
                Person existing = personRepository.findByStableUuid(person.getStableUuid()).orElse(null);
                if (existing != null) {
                    person.setPersonId(existing.getPersonId());
                }

                personService.saveImportedPerson(person);
                imported++;
            }

            return imported;
        } catch (IOException ex) {
            throw new RuntimeException("Could not import people from CSV.", ex);
        }
    }

    private Person mapPerson(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), i < values.size() ? emptyToNull(values.get(i)) : null);
        }

        Person person = new Person();
        person.setStableUuid(required(row, "stable_uuid"));
        person.setFsPid(row.get("fs_pid"));
        person.setPreferredName(required(row, "preferred_name"));
        person.setGivenNames(row.get("given_names"));
        person.setSurname(row.get("surname"));
        person.setSex(parseEnum(row.get("sex"), Sex.UNKNOWN, Sex.class));
        person.setLiving(Boolean.parseBoolean(defaultValue(row.get("is_living"), "false")));
        person.setBirthDateText(row.get("birth_date_text"));
        person.setDeathDateText(row.get("death_date_text"));
        person.setBirthDatePrecision(parseEnum(row.get("birth_date_precision"), DatePrecision.UNKNOWN, DatePrecision.class));
        person.setDeathDatePrecision(parseEnum(row.get("death_date_precision"), DatePrecision.UNKNOWN, DatePrecision.class));
        person.setReviewedStatus(parseEnum(row.get("reviewed_status"), ReviewedStatus.NOT_REVIEWED, ReviewedStatus.class));
        person.setLastReviewedOn(row.get("last_reviewed_on"));
        person.setStewardshipStatus(parseEnum(row.get("stewardship_status"), StewardshipStatus.UNASSIGNED, StewardshipStatus.class));
        person.setNotes(row.get("notes"));
        person.setRoot(Boolean.parseBoolean(defaultValue(row.get("is_root"), "false")));
        person.setDeleted(Boolean.parseBoolean(defaultValue(row.get("is_deleted"), "false")));
        person.setCreatedAt(row.get("created_at"));
        person.setUpdatedAt(row.get("updated_at"));
        person.setDeletedAt(row.get("deleted_at"));
        person.setVersion(parseInt(row.get("version"), 1));
        person.setSyncStatus(parseEnum(row.get("sync_status"), SyncStatus.LOCAL_ONLY, SyncStatus.class));
        person.setLastSyncedAt(row.get("last_synced_at"));
        person.setLastModifiedByDevice(row.get("last_modified_by_device"));
        return person;
    }

    private String toCsvLine(Person person) {
        return String.join(",", List.of(
                escape(person.getStableUuid()),
                escape(person.getFsPid()),
                escape(person.getPreferredName()),
                escape(person.getGivenNames()),
                escape(person.getSurname()),
                escape(person.getSex().name()),
                escape(Boolean.toString(person.isLiving())),
                escape(person.getBirthDateText()),
                escape(person.getDeathDateText()),
                escape(person.getBirthDatePrecision().name()),
                escape(person.getDeathDatePrecision().name()),
                escape(person.getReviewedStatus().name()),
                escape(person.getLastReviewedOn()),
                escape(person.getStewardshipStatus().name()),
                escape(person.getNotes()),
                escape(Boolean.toString(person.isRoot())),
                escape(Boolean.toString(person.isDeleted())),
                escape(person.getCreatedAt()),
                escape(person.getUpdatedAt()),
                escape(person.getDeletedAt()),
                escape(Integer.toString(person.getVersion())),
                escape(person.getSyncStatus().name()),
                escape(person.getLastSyncedAt()),
                escape(person.getLastModifiedByDevice())
        ));
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        values.add(current.toString());
        return values;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String required(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CSV is missing required field: " + key);
        }
        return value;
    }

    private String defaultValue(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value);
    }

    private <T extends Enum<T>> T parseEnum(String value, T fallback, Class<T> enumType) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Enum.valueOf(enumType, value);
    }
}
