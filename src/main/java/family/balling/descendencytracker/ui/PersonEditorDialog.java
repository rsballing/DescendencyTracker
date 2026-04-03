package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.enums.DatePrecision;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;
import family.balling.descendencytracker.domain.enums.Sex;
import family.balling.descendencytracker.domain.enums.StewardshipStatus;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonEditorDialog extends Dialog<Person> {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(\\d{4})\\b");

    private final Person existingPerson;

    private final TextField preferredNameField = new TextField();
    private final TextField fsPidField = new TextField();
    private final TextField givenNamesField = new TextField();
    private final TextField surnameField = new TextField();

    private final ComboBox<Sex> sexComboBox = new ComboBox<>();
    private final CheckBox livingCheckBox = new CheckBox("Living");

    private final TextField birthDateField = new TextField();
    private final ComboBox<DatePrecision> birthPrecisionComboBox = new ComboBox<>();

    private final TextField deathDateField = new TextField();
    private final ComboBox<DatePrecision> deathPrecisionComboBox = new ComboBox<>();

    private final ComboBox<ReviewedStatus> reviewedStatusComboBox = new ComboBox<>();
    private final ComboBox<StewardshipStatus> stewardshipStatusComboBox = new ComboBox<>();
    private final TextArea notesArea = new TextArea();

    public PersonEditorDialog(Person existingPerson) {
        this.existingPerson = existingPerson;

        setTitle(existingPerson == null ? "Add Person" : "Edit Person");
        setHeaderText(existingPerson == null ? "Create a new person." : "Edit the selected person.");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setContent(buildForm());

        configureControls();
        populateFields();

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateInputs();
            if (validationError != null) {
                event.consume();
                showWarning(validationError);
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }

            Person result = existingPerson == null ? new Person() : existingPerson;

            result.setPreferredName(clean(preferredNameField.getText()));
            result.setFsPid(clean(fsPidField.getText()));
            result.setGivenNames(clean(givenNamesField.getText()));
            result.setSurname(clean(surnameField.getText()));
            result.setSex(sexComboBox.getValue() == null ? Sex.UNKNOWN : sexComboBox.getValue());
            result.setLiving(livingCheckBox.isSelected());
            result.setBirthDateText(clean(birthDateField.getText()));
            result.setBirthDatePrecision(
                    birthPrecisionComboBox.getValue() == null ? DatePrecision.UNKNOWN : birthPrecisionComboBox.getValue()
            );
            result.setDeathDateText(clean(deathDateField.getText()));
            result.setDeathDatePrecision(
                    deathPrecisionComboBox.getValue() == null ? DatePrecision.UNKNOWN : deathPrecisionComboBox.getValue()
            );
            result.setReviewedStatus(
                    reviewedStatusComboBox.getValue() == null ? ReviewedStatus.NOT_REVIEWED : reviewedStatusComboBox.getValue()
            );
            result.setStewardshipStatus(
                    stewardshipStatusComboBox.getValue() == null ? StewardshipStatus.UNASSIGNED : stewardshipStatusComboBox.getValue()
            );
            result.setNotes(clean(notesArea.getText()));

            return result;
        });
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;

        grid.add(new Label("Preferred Name"), 0, row);
        grid.add(preferredNameField, 1, row++);

        grid.add(new Label("FamilySearch PID"), 0, row);
        grid.add(fsPidField, 1, row++);

        grid.add(new Label("Given Names"), 0, row);
        grid.add(givenNamesField, 1, row++);

        grid.add(new Label("Surname"), 0, row);
        grid.add(surnameField, 1, row++);

        grid.add(new Label("Sex"), 0, row);
        grid.add(sexComboBox, 1, row++);

        grid.add(new Label("Status"), 0, row);
        grid.add(livingCheckBox, 1, row++);

        grid.add(new Label("Birth Date"), 0, row);
        grid.add(birthDateField, 1, row++);

        grid.add(new Label("Birth Precision"), 0, row);
        grid.add(birthPrecisionComboBox, 1, row++);

        grid.add(new Label("Death Date"), 0, row);
        grid.add(deathDateField, 1, row++);

        grid.add(new Label("Death Precision"), 0, row);
        grid.add(deathPrecisionComboBox, 1, row++);

        grid.add(new Label("Reviewed Status"), 0, row);
        grid.add(reviewedStatusComboBox, 1, row++);

        grid.add(new Label("Stewardship"), 0, row);
        grid.add(stewardshipStatusComboBox, 1, row++);

        grid.add(new Label("Notes"), 0, row);
        grid.add(notesArea, 1, row);

        preferredNameField.setPrefWidth(320);
        fsPidField.setPrefWidth(320);
        givenNamesField.setPrefWidth(320);
        surnameField.setPrefWidth(320);
        birthDateField.setPrefWidth(320);
        deathDateField.setPrefWidth(320);

        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(6);

        return grid;
    }

    private void configureControls() {
        sexComboBox.getItems().setAll(Sex.values());
        birthPrecisionComboBox.getItems().setAll(DatePrecision.values());
        deathPrecisionComboBox.getItems().setAll(DatePrecision.values());
        reviewedStatusComboBox.getItems().setAll(ReviewedStatus.values());
        stewardshipStatusComboBox.getItems().setAll(StewardshipStatus.values());

        preferredNameField.setPromptText("Required");
        fsPidField.setPromptText("Optional, e.g. KWZ3-ABC");
        birthDateField.setPromptText("Optional text, e.g. 14 Jun 1904 or 1904");
        deathDateField.setPromptText("Optional text, e.g. 9 Mar 1988 or 1988");

        sexComboBox.setValue(Sex.UNKNOWN);
        birthPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
        deathPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
        reviewedStatusComboBox.setValue(ReviewedStatus.NOT_REVIEWED);
        stewardshipStatusComboBox.setValue(StewardshipStatus.UNASSIGNED);

        livingCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateLivingState());
    }

    private void populateFields() {
        if (existingPerson == null) {
            updateLivingState();
            return;
        }

        preferredNameField.setText(nullSafe(existingPerson.getPreferredName()));
        fsPidField.setText(nullSafe(existingPerson.getFsPid()));
        givenNamesField.setText(nullSafe(existingPerson.getGivenNames()));
        surnameField.setText(nullSafe(existingPerson.getSurname()));

        sexComboBox.setValue(existingPerson.getSex() == null ? Sex.UNKNOWN : existingPerson.getSex());
        livingCheckBox.setSelected(existingPerson.isLiving());

        birthDateField.setText(nullSafe(existingPerson.getBirthDateText()));
        birthPrecisionComboBox.setValue(
                existingPerson.getBirthDatePrecision() == null ? DatePrecision.UNKNOWN : existingPerson.getBirthDatePrecision()
        );

        deathDateField.setText(nullSafe(existingPerson.getDeathDateText()));
        deathPrecisionComboBox.setValue(
                existingPerson.getDeathDatePrecision() == null ? DatePrecision.UNKNOWN : existingPerson.getDeathDatePrecision()
        );

        reviewedStatusComboBox.setValue(
                existingPerson.getReviewedStatus() == null ? ReviewedStatus.NOT_REVIEWED : existingPerson.getReviewedStatus()
        );
        stewardshipStatusComboBox.setValue(
                existingPerson.getStewardshipStatus() == null
                        ? StewardshipStatus.UNASSIGNED
                        : existingPerson.getStewardshipStatus()
        );

        notesArea.setText(nullSafe(existingPerson.getNotes()));

        updateLivingState();
    }

    private void updateLivingState() {
        boolean living = livingCheckBox.isSelected();

        deathDateField.setDisable(living);
        deathPrecisionComboBox.setDisable(living);

        if (living) {
            deathDateField.clear();
            deathPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
        }
    }

    private String validateInputs() {
        String preferredName = clean(preferredNameField.getText());
        if (preferredName == null) {
            return "Preferred name is required.";
        }

        if (livingCheckBox.isSelected() && clean(deathDateField.getText()) != null) {
            return "A living person cannot have a death date.";
        }

        Integer birthYear = extractYear(birthDateField.getText());
        Integer deathYear = extractYear(deathDateField.getText());

        if (birthYear != null && deathYear != null && deathYear < birthYear) {
            return "Death year cannot be earlier than birth year.";
        }

        return null;
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

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
