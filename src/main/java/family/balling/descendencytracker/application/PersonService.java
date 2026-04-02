package family.balling.descendencytracker.application;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.repository.PersonRepository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonService {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(\\d{4})\\b");

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
        person.setPreferredName(trimToNull(person.getPreferredName()));
        person.setFsPid(trimToNull(person.getFsPid()));
        person.setGivenNames(trimToNull(person.getGivenNames()));
        person.setSurname(trimToNull(person.getSurname()));
        person.setBirthDateText(trimToNull(person.getBirthDateText()));
        person.setDeathDateText(trimToNull(person.getDeathDateText()));
        person.setNotes(trimToNull(person.getNotes()));
    }

    private void validate(Person person) {
        if (person.getPreferredName() == null) {
            throw new IllegalArgumentException("Preferred name is required.");
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}