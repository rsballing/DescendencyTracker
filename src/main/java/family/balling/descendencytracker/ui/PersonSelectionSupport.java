package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.Person;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PersonSelectionSupport {
    private static final String PERSON_SOURCE_KEY = "personSource";
    private static final String UPDATING_EDITOR_KEY = "updatingEditor";

    private PersonSelectionSupport() {
    }

    static void configurePersonAutocomplete(ComboBox<Person> comboBox, List<Person> candidates, String promptText) {
        List<Person> source = candidates == null ? List.of() : new ArrayList<>(candidates);

        comboBox.setEditable(true);
        comboBox.setItems(FXCollections.observableArrayList(source));
        comboBox.setPromptText(promptText);
        comboBox.getProperties().put(PERSON_SOURCE_KEY, source);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Person person) {
                return formatPerson(person);
            }

            @Override
            public Person fromString(String string) {
                return findBestMatch(source, string);
            }
        });

        comboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                setEditorText(comboBox, formatPerson(newValue));
            }
        });

        comboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(comboBox.getProperties().get(UPDATING_EDITOR_KEY))) {
                return;
            }
            if (!comboBox.isFocused() && !comboBox.getEditor().isFocused()) {
                return;
            }

            List<Person> filtered = filterPeople(source, newValue);
            comboBox.setItems(FXCollections.observableArrayList(filtered));

            if (newValue == null || newValue.isBlank()) {
                comboBox.hide();
                return;
            }

            if (!filtered.isEmpty()) {
                comboBox.show();
            } else {
                comboBox.hide();
            }
        });

        comboBox.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                commitEditorText(comboBox);
            } else if (event.getCode() == KeyCode.DOWN && !comboBox.isShowing()) {
                comboBox.show();
            }
        });

        comboBox.getEditor().focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                commitEditorText(comboBox);
            }
        });
        comboBox.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (!comboBox.isShowing() && comboBox.isEditable() && !comboBox.isDisabled()) {
                comboBox.show();
            }
        });
        comboBox.setOnAction(event -> {
            Person selected = comboBox.getValue();
            if (selected != null) {
                setEditorText(comboBox, formatPerson(selected));
            }
        });

        Platform.runLater(() -> comboBox.setItems(FXCollections.observableArrayList(source)));
    }

    @SuppressWarnings("unchecked")
    static void updateCandidates(ComboBox<Person> comboBox, List<Person> candidates) {
        List<Person> source = candidates == null ? List.of() : new ArrayList<>(candidates);
        comboBox.getProperties().put(PERSON_SOURCE_KEY, source);
        comboBox.setItems(FXCollections.observableArrayList(source));

        Person selected = comboBox.getValue();
        if (selected != null && !source.contains(selected)) {
            comboBox.setValue(null);
            comboBox.getEditor().clear();
        }
    }

    static String buildSearchText(Person person) {
        if (person == null) {
            return "";
        }

        return String.join(" ",
                nullSafe(person.getDisplayName()),
                nullSafe(person.getPreferredName()),
                nullSafe(person.getGivenNames()),
                nullSafe(person.getSurname()),
                nullSafe(person.getFsPid()),
                nullSafe(person.getBirthDateText()),
                nullSafe(person.getDeathDateText()),
                nullSafe(person.getNotes()),
                person.getReviewedStatus() == null ? "" : person.getReviewedStatus().name(),
                person.getSex() == null ? "" : person.getSex().name()
        ).toLowerCase(Locale.ENGLISH);
    }

    @SuppressWarnings("unchecked")
    private static void commitEditorText(ComboBox<Person> comboBox) {
        List<Person> source = (List<Person>) comboBox.getProperties().get(PERSON_SOURCE_KEY);
        if (source == null) {
            source = List.of();
        }

        Person currentValue = comboBox.getValue();
        String editorText = comboBox.getEditor().getText();
        if (currentValue != null && (editorText == null || editorText.isBlank())) {
            comboBox.setItems(FXCollections.observableArrayList(source));
            setEditorText(comboBox, formatPerson(currentValue));
            return;
        }

        if (currentValue != null && editorText != null
                && editorText.trim().equalsIgnoreCase(formatPerson(currentValue))) {
            comboBox.setItems(FXCollections.observableArrayList(source));
            return;
        }

        Person match = findBestMatch(source, editorText);
        comboBox.setValue(match);
        comboBox.setItems(FXCollections.observableArrayList(source));
        if (match != null) {
            setEditorText(comboBox, formatPerson(match));
        } else if (editorText == null || editorText.isBlank()) {
            setEditorText(comboBox, "");
        }
    }

    private static void setEditorText(ComboBox<Person> comboBox, String text) {
        comboBox.getProperties().put(UPDATING_EDITOR_KEY, true);
        try {
            comboBox.getEditor().setText(text == null ? "" : text);
        } finally {
            comboBox.getProperties().put(UPDATING_EDITOR_KEY, false);
        }
    }

    private static List<Person> filterPeople(List<Person> source, String query) {
        if (query == null || query.isBlank()) {
            return source;
        }

        String normalized = query.trim().toLowerCase(Locale.ENGLISH);
        String[] tokens = normalized.split("\\s+");
        List<Person> filtered = new ArrayList<>();

        for (Person person : source) {
            String haystack = buildSearchText(person);
            boolean matches = true;
            for (String token : tokens) {
                if (!haystack.contains(token)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                filtered.add(person);
            }
        }

        return filtered;
    }

    private static Person findBestMatch(List<Person> source, String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String normalized = query.trim().toLowerCase(Locale.ENGLISH);
        Person prefixMatch = null;

        for (Person person : source) {
            String label = formatPerson(person).toLowerCase(Locale.ENGLISH);
            if (label.equals(normalized)) {
                return person;
            }
            if (prefixMatch == null && label.startsWith(normalized)) {
                prefixMatch = person;
            }
        }

        if (prefixMatch != null) {
            return prefixMatch;
        }

        List<Person> filtered = filterPeople(source, normalized);
        return filtered.isEmpty() ? null : filtered.get(0);
    }

    private static String formatPerson(Person person) {
        if (person == null) {
            return "";
        }

        String label = nullSafe(person.getDisplayName());
        if (person.getFsPid() != null && !person.getFsPid().isBlank()) {
            label += " [" + person.getFsPid() + "]";
        }
        return label;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
