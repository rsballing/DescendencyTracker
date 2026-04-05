package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import family.balling.descendencytracker.domain.enums.Sex;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
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

public class SpouseLinkDialog extends Dialog<SpouseLinkDialog.Result> {
    private static final KeyCombination SUBMIT_SHORTCUT = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);

    private final ComboBox<Person> spouseComboBox = new ComboBox<>();
    private final CheckBox createNewPersonCheckBox = new CheckBox("Create a new spouse instead");

    private final TextField preferredNameField = new TextField();
    private final TextField fsPidField = new TextField();
    private final TextField givenNamesField = new TextField();
    private final TextField surnameField = new TextField();
    private final ComboBox<Sex> sexComboBox = new ComboBox<>();
    private final CheckBox livingCheckBox = new CheckBox("Living");

    private final TextField marriageDateField = new TextField();
    private final TextArea marriageNotesArea = new TextArea();

    private final ComboBox<OrdinanceStatus> sealingStatusComboBox = new ComboBox<>();
    private final TextField sealingDateField = new TextField();
    private final TextArea sealingNotesArea = new TextArea();

    public SpouseLinkDialog(Person selectedPerson, List<Person> candidates) {
        this(selectedPerson, candidates, null);
    }

    public SpouseLinkDialog(Person selectedPerson, List<Person> candidates, SpouseLink existingLink) {
        setTitle(existingLink == null ? "Add Spouse" : "Edit Spouse Link");
        setHeaderText(existingLink == null
                ? "Add a spouse relationship for " + selectedPerson.getDisplayName()
                : "Edit the selected spouse relationship for " + selectedPerson.getDisplayName());

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setContent(buildForm());

        spouseComboBox.setPrefWidth(320);
        PersonSelectionSupport.configurePersonAutocomplete(
                spouseComboBox,
                candidates,
                "Type to find an existing spouse"
        );

        sexComboBox.getItems().setAll(Sex.values());
        sexComboBox.setValue(Sex.UNKNOWN);

        sealingStatusComboBox.getItems().setAll(OrdinanceStatus.values());
        sealingStatusComboBox.setValue(OrdinanceStatus.UNKNOWN);

        marriageDateField.setPromptText("Optional text, e.g. 14 Jun 1904 or 1904");
        marriageNotesArea.setWrapText(true);
        marriageNotesArea.setPrefRowCount(4);
        fsPidField.setPromptText("Optional, e.g. KWZ3-ABC");
        sealingDateField.setPromptText("Optional text");
        sealingNotesArea.setWrapText(true);
        sealingNotesArea.setPrefRowCount(4);
        sexComboBox.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSexShortcut);
        sealingStatusComboBox.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSealingStatusShortcut);

        if (existingLink != null) {
            Long otherPersonId = existingLink.getOtherPersonId(selectedPerson.getPersonId());
            for (Person candidate : candidates) {
                if (candidate.getPersonId().equals(otherPersonId)) {
                    spouseComboBox.setValue(candidate);
                    break;
                }
            }

            marriageDateField.setText(existingLink.getMarriageDateText() == null ? "" : existingLink.getMarriageDateText());
            marriageNotesArea.setText(existingLink.getMarriageNotes() == null ? "" : existingLink.getMarriageNotes());
            sealingStatusComboBox.setValue(existingLink.getSealingToSpouseStatus() == null
                    ? OrdinanceStatus.UNKNOWN
                    : existingLink.getSealingToSpouseStatus());
            sealingDateField.setText(existingLink.getSealingStatusDate() == null ? "" : existingLink.getSealingStatusDate());
            sealingNotesArea.setText(existingLink.getSealingNotes() == null ? "" : existingLink.getSealingNotes());

            createNewPersonCheckBox.setSelected(false);
        } else {
            createNewPersonCheckBox.setSelected(candidates.isEmpty());
        }

        updateMode();
        createNewPersonCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateMode());
        focusPrimaryInput();

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (createNewPersonCheckBox.isSelected()) {
                if (preferredNameField.getText() == null || preferredNameField.getText().isBlank()) {
                    event.consume();
                    showWarning("Please enter a preferred name for the new spouse.");
                }
            } else {
                if (spouseComboBox.getValue() == null) {
                    event.consume();
                    showWarning("Please select a spouse.");
                }
            }
        });

        configureSubmitShortcut();

        setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }

            if (createNewPersonCheckBox.isSelected()) {
                Person newPerson = new Person();
                newPerson.setPreferredName(preferredNameField.getText().trim());
                newPerson.setFsPid(clean(fsPidField.getText()));
                newPerson.setGivenNames(clean(givenNamesField.getText()));
                newPerson.setSurname(clean(surnameField.getText()));
                newPerson.setSex(sexComboBox.getValue());
                newPerson.setLiving(livingCheckBox.isSelected());

                return new Result(
                        null,
                        newPerson,
                        clean(marriageDateField.getText()),
                        clean(marriageNotesArea.getText()),
                        sealingStatusComboBox.getValue(),
                        clean(sealingDateField.getText()),
                        clean(sealingNotesArea.getText())
                );
            }

            return new Result(
                    spouseComboBox.getValue().getPersonId(),
                    null,
                    clean(marriageDateField.getText()),
                    clean(marriageNotesArea.getText()),
                    sealingStatusComboBox.getValue(),
                    clean(sealingDateField.getText()),
                    clean(sealingNotesArea.getText())
            );
        });
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;

        grid.add(new Label("Existing Spouse"), 0, row);
        grid.add(spouseComboBox, 1, row++);

        grid.add(new Label("Mode"), 0, row);
        grid.add(createNewPersonCheckBox, 1, row++);

        grid.add(new Label("New Spouse Preferred Name"), 0, row);
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

        grid.add(new Label("Marriage Date"), 0, row);
        grid.add(marriageDateField, 1, row++);

        grid.add(new Label("Marriage Notes"), 0, row);
        grid.add(marriageNotesArea, 1, row++);

        grid.add(new Label("Sealed to Spouse"), 0, row);
        grid.add(sealingStatusComboBox, 1, row++);

        grid.add(new Label("Sealing Status Date"), 0, row);
        grid.add(sealingDateField, 1, row++);

        grid.add(new Label("Sealing Notes"), 0, row);
        grid.add(sealingNotesArea, 1, row);

        preferredNameField.setPrefWidth(320);
        fsPidField.setPrefWidth(320);
        givenNamesField.setPrefWidth(320);
        surnameField.setPrefWidth(320);

        return grid;
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
            if (createNewPersonCheckBox.isSelected()) {
                preferredNameField.requestFocus();
                preferredNameField.selectAll();
                return;
            }

            spouseComboBox.requestFocus();
            if (spouseComboBox.isEditable()) {
                spouseComboBox.getEditor().requestFocus();
                spouseComboBox.getEditor().selectAll();
            }
        });
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

    private void handleSealingStatusShortcut(KeyEvent event) {
        OrdinanceStatus status = switch (event.getCode()) {
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

        if (status != null) {
            sealingStatusComboBox.setValue(status);
            event.consume();
        }
    }

    private void updateMode() {
        boolean creatingNew = createNewPersonCheckBox.isSelected();

        spouseComboBox.setDisable(creatingNew);

        preferredNameField.setDisable(!creatingNew);
        fsPidField.setDisable(!creatingNew);
        givenNamesField.setDisable(!creatingNew);
        surnameField.setDisable(!creatingNew);
        sexComboBox.setDisable(!creatingNew);
        livingCheckBox.setDisable(!creatingNew);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Result {
        private final Long spousePersonId;
        private final Person newPerson;
        private final String marriageDateText;
        private final String marriageNotes;
        private final OrdinanceStatus sealingStatus;
        private final String sealingStatusDate;
        private final String sealingNotes;

        public Result(
                Long spousePersonId,
                Person newPerson,
                String marriageDateText,
                String marriageNotes,
                OrdinanceStatus sealingStatus,
                String sealingStatusDate,
                String sealingNotes
        ) {
            this.spousePersonId = spousePersonId;
            this.newPerson = newPerson;
            this.marriageDateText = marriageDateText;
            this.marriageNotes = marriageNotes;
            this.sealingStatus = sealingStatus;
            this.sealingStatusDate = sealingStatusDate;
            this.sealingNotes = sealingNotes;
        }

        public Long getSpousePersonId() {
            return spousePersonId;
        }

        public Person getNewPerson() {
            return newPerson;
        }

        public String getMarriageDateText() {
            return marriageDateText;
        }

        public String getMarriageNotes() {
            return marriageNotes;
        }

        public OrdinanceStatus getSealingStatus() {
            return sealingStatus;
        }

        public String getSealingStatusDate() {
            return sealingStatusDate;
        }

        public String getSealingNotes() {
            return sealingNotes;
        }
    }
}
