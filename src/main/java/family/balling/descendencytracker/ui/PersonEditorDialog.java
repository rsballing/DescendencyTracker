package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.enums.DatePrecision;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.domain.enums.ReviewedStatus;
import family.balling.descendencytracker.domain.enums.Sex;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonEditorDialog extends Dialog<PersonEditorDialog.Result> {
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(\\d{4})\\b");
    private static final KeyCombination SUBMIT_SHORTCUT = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);

    private final Person existingPerson;

    private final TextField preferredNameField = new TextField();
    private final FsPidFields fsPidFields = new FsPidFields();
    private final TextField givenNamesField = new TextField();
    private final TextField surnameField = new TextField();

    private final ComboBox<Sex> sexComboBox = new ComboBox<>();
    private final CheckBox livingCheckBox = new CheckBox("Living");

    private final TextField birthDateField = new TextField();
    private final ComboBox<DatePrecision> birthPrecisionComboBox = new ComboBox<>();

    private final TextField deathDateField = new TextField();
    private final ComboBox<DatePrecision> deathPrecisionComboBox = new ComboBox<>();

    private final ComboBox<ReviewedStatus> reviewedStatusComboBox = new ComboBox<>();
    private final TextArea notesArea = new TextArea();
    private final ComboBox<OrdinanceStatus> baptismStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> confirmationStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> initiatoryStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> endowmentStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> sealedToParentsStatusComboBox = new ComboBox<>();
    private final TextArea ordinanceNotesArea = new TextArea();
    private final PersonOrdinanceStatus existingOrdinanceStatus;

    public PersonEditorDialog(Person existingPerson, PersonOrdinanceStatus existingOrdinanceStatus) {
        this.existingPerson = existingPerson;
        this.existingOrdinanceStatus = existingOrdinanceStatus;

        setTitle(existingPerson == null ? "Add Person" : "Edit Person");
        setHeaderText(existingPerson == null ? "Create a new person." : "Edit the selected person.");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setContent(buildForm());

        configureControls();
        populateFields();
        configureSubmitShortcut();
        focusPrimaryInput();

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

            result.setPreferredName(DateTextSupport.clean(preferredNameField.getText()));
            result.setFsPid(fsPidFields.getValue());
            result.setGivenNames(DateTextSupport.clean(givenNamesField.getText()));
            result.setSurname(DateTextSupport.clean(surnameField.getText()));
            result.setSex(sexComboBox.getValue() == null ? Sex.UNKNOWN : sexComboBox.getValue());
            result.setLiving(livingCheckBox.isSelected());
            result.setBirthDateText(DateTextSupport.normalizeDateText(birthDateField.getText()));
            result.setBirthDatePrecision(
                    birthPrecisionComboBox.getValue() == null ? DatePrecision.UNKNOWN : birthPrecisionComboBox.getValue()
            );
            result.setDeathDateText(DateTextSupport.normalizeDateText(deathDateField.getText()));
            result.setDeathDatePrecision(
                    deathPrecisionComboBox.getValue() == null ? DatePrecision.UNKNOWN : deathPrecisionComboBox.getValue()
            );
            result.setReviewedStatus(
                    reviewedStatusComboBox.getValue() == null ? ReviewedStatus.NOT_REVIEWED : reviewedStatusComboBox.getValue()
            );
            result.setNotes(DateTextSupport.clean(notesArea.getText()));

            PersonOrdinanceStatus ordinanceStatus = existingOrdinanceStatus == null
                    ? new PersonOrdinanceStatus()
                    : existingOrdinanceStatus;
            ordinanceStatus.setBaptismStatus(selectedOrdinanceStatus(baptismStatusComboBox));
            ordinanceStatus.setConfirmationStatus(selectedOrdinanceStatus(confirmationStatusComboBox));
            ordinanceStatus.setInitiatoryStatus(selectedOrdinanceStatus(initiatoryStatusComboBox));
            ordinanceStatus.setEndowmentStatus(selectedOrdinanceStatus(endowmentStatusComboBox));
            ordinanceStatus.setSealedToParentsStatus(selectedOrdinanceStatus(sealedToParentsStatusComboBox));
            ordinanceStatus.setOrdinanceNotes(DateTextSupport.clean(ordinanceNotesArea.getText()));

            return new Result(result, ordinanceStatus);
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
        grid.add(fsPidFields.getNode(), 1, row++);

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

        grid.add(new Label("Baptism"), 0, row);
        grid.add(baptismStatusComboBox, 1, row++);

        grid.add(new Label("Confirmation"), 0, row);
        grid.add(confirmationStatusComboBox, 1, row++);

        grid.add(new Label("Initiatory"), 0, row);
        grid.add(initiatoryStatusComboBox, 1, row++);

        grid.add(new Label("Endowment"), 0, row);
        grid.add(endowmentStatusComboBox, 1, row++);

        grid.add(new Label("Sealed to Parents"), 0, row);
        grid.add(sealedToParentsStatusComboBox, 1, row++);

        grid.add(new Label("Ordinance Notes"), 0, row);
        grid.add(ordinanceNotesArea, 1, row++);

        grid.add(new Label("Notes"), 0, row);
        grid.add(notesArea, 1, row);

        preferredNameField.setPrefWidth(320);
        fsPidFields.setPrefWidth(156);
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
        baptismStatusComboBox.getItems().setAll(OrdinanceStatus.values());
        confirmationStatusComboBox.getItems().setAll(OrdinanceStatus.values());
        initiatoryStatusComboBox.getItems().setAll(OrdinanceStatus.values());
        endowmentStatusComboBox.getItems().setAll(OrdinanceStatus.values());
        sealedToParentsStatusComboBox.getItems().setAll(OrdinanceStatus.values());

        preferredNameField.setPromptText("Required");
        fsPidFields.setValue(null);
        birthDateField.setPromptText("Optional text, e.g. 14 Jun 1904 or 1904");
        deathDateField.setPromptText("Optional text, e.g. 9 Mar 1988 or 1988");

        sexComboBox.setValue(Sex.UNKNOWN);
        birthPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
        deathPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
        reviewedStatusComboBox.setValue(ReviewedStatus.NOT_REVIEWED);
        baptismStatusComboBox.setValue(OrdinanceStatus.UNKNOWN);
        confirmationStatusComboBox.setValue(OrdinanceStatus.UNKNOWN);
        initiatoryStatusComboBox.setValue(OrdinanceStatus.UNKNOWN);
        endowmentStatusComboBox.setValue(OrdinanceStatus.UNKNOWN);
        sealedToParentsStatusComboBox.setValue(OrdinanceStatus.UNKNOWN);

        livingCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateLivingState());
        sexComboBox.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSexShortcut);
        birthPrecisionComboBox.addEventFilter(KeyEvent.KEY_PRESSED, this::handleBirthPrecisionShortcut);
        deathPrecisionComboBox.addEventFilter(KeyEvent.KEY_PRESSED, this::handleDeathPrecisionShortcut);
        reviewedStatusComboBox.addEventFilter(KeyEvent.KEY_PRESSED, this::handleReviewedStatusShortcut);
        configureOrdinanceShortcut(baptismStatusComboBox);
        configureOrdinanceShortcut(confirmationStatusComboBox);
        configureOrdinanceShortcut(initiatoryStatusComboBox);
        configureOrdinanceShortcut(endowmentStatusComboBox);
        configureOrdinanceShortcut(sealedToParentsStatusComboBox);

        ordinanceNotesArea.setWrapText(true);
        ordinanceNotesArea.setPrefRowCount(4);
    }

    private void configureSubmitShortcut() {
        getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!SUBMIT_SHORTCUT.match(event)) {
                return;
            }

            Node okButton = getDialogPane().lookupButton(ButtonType.OK);
            if (okButton instanceof ButtonBase button && !button.isDisabled()) {
                button.fire();
                event.consume();
            }
        });
    }

    private void focusPrimaryInput() {
        Platform.runLater(() -> {
            preferredNameField.requestFocus();
            preferredNameField.selectAll();
        });
    }

    private void populateFields() {
        if (existingPerson == null) {
            updateLivingState();
            return;
        }

        preferredNameField.setText(nullSafe(existingPerson.getPreferredName()));
        fsPidFields.setValue(existingPerson.getFsPid());
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

        notesArea.setText(nullSafe(existingPerson.getNotes()));
        if (existingOrdinanceStatus != null) {
            baptismStatusComboBox.setValue(safeStatus(existingOrdinanceStatus.getBaptismStatus()));
            confirmationStatusComboBox.setValue(safeStatus(existingOrdinanceStatus.getConfirmationStatus()));
            initiatoryStatusComboBox.setValue(safeStatus(existingOrdinanceStatus.getInitiatoryStatus()));
            endowmentStatusComboBox.setValue(safeStatus(existingOrdinanceStatus.getEndowmentStatus()));
            sealedToParentsStatusComboBox.setValue(safeStatus(existingOrdinanceStatus.getSealedToParentsStatus()));
            ordinanceNotesArea.setText(nullSafe(existingOrdinanceStatus.getOrdinanceNotes()));
        }

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

    private void handleSexShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.M) {
            sexComboBox.setValue(Sex.MALE);
            event.consume();
        } else if (event.getCode() == KeyCode.F) {
            sexComboBox.setValue(Sex.FEMALE);
            event.consume();
        }
    }

    private void handleBirthPrecisionShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.E) {
            birthPrecisionComboBox.setValue(DatePrecision.EXACT);
            event.consume();
        } else if (event.getCode() == KeyCode.M) {
            birthPrecisionComboBox.setValue(DatePrecision.MONTH_YEAR);
            event.consume();
        } else if (event.getCode() == KeyCode.Y) {
            birthPrecisionComboBox.setValue(DatePrecision.YEAR_ONLY);
            event.consume();
        } else if (event.getCode() == KeyCode.U) {
            birthPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
            event.consume();
        }
    }

    private void handleDeathPrecisionShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.E) {
            deathPrecisionComboBox.setValue(DatePrecision.EXACT);
            event.consume();
        } else if (event.getCode() == KeyCode.M) {
            deathPrecisionComboBox.setValue(DatePrecision.MONTH_YEAR);
            event.consume();
        } else if (event.getCode() == KeyCode.Y) {
            deathPrecisionComboBox.setValue(DatePrecision.YEAR_ONLY);
            event.consume();
        } else if (event.getCode() == KeyCode.U) {
            deathPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
            event.consume();
        }
    }

    private void handleReviewedStatusShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.N) {
            reviewedStatusComboBox.setValue(ReviewedStatus.NOT_REVIEWED);
            event.consume();
        } else if (event.getCode() == KeyCode.I) {
            reviewedStatusComboBox.setValue(ReviewedStatus.IN_PROGRESS);
            event.consume();
        } else if (event.getCode() == KeyCode.R) {
            reviewedStatusComboBox.setValue(ReviewedStatus.REVIEWED);
            event.consume();
        }
    }

    private void configureOrdinanceShortcut(ComboBox<OrdinanceStatus> comboBox) {
        comboBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            OrdinanceStatus status = mapOrdinanceShortcut(event.getCode());
            if (status != null) {
                comboBox.setValue(status);
                event.consume();
            }
        });
    }

    private OrdinanceStatus mapOrdinanceShortcut(KeyCode keyCode) {
        return switch (keyCode) {
            case C -> OrdinanceStatus.COMPLETE;
            case O -> OrdinanceStatus.OPEN;
            case U -> OrdinanceStatus.UNKNOWN;
            case N -> OrdinanceStatus.NOT_APPLICABLE;
            case B -> OrdinanceStatus.BLOCKED_110;
            case DIGIT1, NUMPAD1 -> OrdinanceStatus.SOON_1Y;
            case DIGIT2, NUMPAD2 -> OrdinanceStatus.SOON_2Y;
            case DIGIT5, NUMPAD5 -> OrdinanceStatus.SOON_5Y;
            case DIGIT0, NUMPAD0 -> OrdinanceStatus.SOON_10Y;
            default -> null;
        };
    }

    private String validateInputs() {
        String preferredName = DateTextSupport.clean(preferredNameField.getText());
        if (preferredName == null) {
            return "Preferred name is required.";
        }

        if (!fsPidFields.isCompleteOrBlank()) {
            return "FamilySearch PID must be entered as four characters plus three characters.";
        }

        String normalizedDeathDate = DateTextSupport.normalizeDateText(deathDateField.getText());
        if (livingCheckBox.isSelected() && normalizedDeathDate != null) {
            return "A living person cannot have a death date.";
        }

        Integer birthYear = extractYear(DateTextSupport.normalizeDateText(birthDateField.getText()));
        Integer deathYear = extractYear(normalizedDeathDate);

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

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private OrdinanceStatus selectedOrdinanceStatus(ComboBox<OrdinanceStatus> comboBox) {
        return comboBox.getValue() == null ? OrdinanceStatus.UNKNOWN : comboBox.getValue();
    }

    private OrdinanceStatus safeStatus(OrdinanceStatus status) {
        return status == null ? OrdinanceStatus.UNKNOWN : status;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Result {
        private final Person person;
        private final PersonOrdinanceStatus ordinanceStatus;

        public Result(Person person, PersonOrdinanceStatus ordinanceStatus) {
            this.person = person;
            this.ordinanceStatus = ordinanceStatus;
        }

        public Person getPerson() {
            return person;
        }

        public PersonOrdinanceStatus getOrdinanceStatus() {
            return ordinanceStatus;
        }
    }
}
