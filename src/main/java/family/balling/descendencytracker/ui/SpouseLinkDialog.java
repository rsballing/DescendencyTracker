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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class SpouseLinkDialog extends Dialog<SpouseLinkDialog.Result> {
    private static final KeyCombination SUBMIT_SHORTCUT = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);

    private final ComboBox<Person> spouseComboBox = new ComboBox<>();
    private final CheckBox createNewPersonCheckBox = new CheckBox("Create a new spouse instead");

    private final TextField preferredNameField = new TextField();
    private final FsPidFields fsPidFields = new FsPidFields();
    private final TextField givenNamesField = new TextField();
    private final TextField surnameField = new TextField();
    private final ComboBox<Sex> sexComboBox = new ComboBox<>();
    private final CheckBox livingCheckBox = new CheckBox("Living");

    private final TextField marriageDateField = new TextField();
    private final TextArea marriageNotesArea = new TextArea();

    private final ComboBox<OrdinanceStatusChoice> sealingStatusComboBox = new ComboBox<>();
    private final TextField sealingDateField = new TextField();
    private final TextArea sealingNotesArea = new TextArea();
    private final VBox childSelectionBox = new VBox(6);
    private final ScrollPane childSelectionPane = new ScrollPane(childSelectionBox);
    private final List<CheckBox> childSelectionCheckBoxes = new ArrayList<>();
    private final boolean allowChildSelection;

    public SpouseLinkDialog(Person selectedPerson, List<Person> candidates, List<Person> childCandidates) {
        this(selectedPerson, candidates, childCandidates, null);
    }

    public SpouseLinkDialog(Person selectedPerson, List<Person> candidates, List<Person> childCandidates, SpouseLink existingLink) {
        this.allowChildSelection = existingLink == null;
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

        sealingStatusComboBox.getItems().setAll(OrdinanceStatusChoice.all());
        sealingStatusComboBox.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));

        marriageDateField.setPromptText("Optional text, e.g. 14 Jun 1904 or 1904");
        marriageNotesArea.setWrapText(true);
        marriageNotesArea.setPrefRowCount(4);
        sealingDateField.setPromptText("Optional text, e.g. 14 Jun 1904 or 1904");
        sealingNotesArea.setWrapText(true);
        sealingNotesArea.setPrefRowCount(4);
        childSelectionPane.setFitToWidth(true);
        childSelectionPane.setPrefViewportHeight(110);
        childSelectionPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        populateChildChoices(childCandidates);
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
            sealingStatusComboBox.setValue(OrdinanceStatusChoice.of(
                    existingLink.getSealingToSpouseStatus(),
                    existingLink.isSealedToSpouseReserved()
            ));
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
            String validationError = validateInputs();
            if (validationError != null) {
                event.consume();
                showWarning(validationError);
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
                newPerson.setFsPid(fsPidFields.getValue());
                newPerson.setGivenNames(DateTextSupport.clean(givenNamesField.getText()));
                newPerson.setSurname(DateTextSupport.clean(surnameField.getText()));
                newPerson.setSex(sexComboBox.getValue());
                newPerson.setLiving(livingCheckBox.isSelected());

                return new Result(
                        null,
                        newPerson,
                        DateTextSupport.normalizeDateText(marriageDateField.getText()),
                        DateTextSupport.clean(marriageNotesArea.getText()),
                        selectedOrdinanceStatus(sealingStatusComboBox),
                        selectedReserved(sealingStatusComboBox),
                        DateTextSupport.normalizeDateText(sealingDateField.getText()),
                        DateTextSupport.clean(sealingNotesArea.getText()),
                        selectedChildIds()
                );
            }

            return new Result(
                    spouseComboBox.getValue().getPersonId(),
                    null,
                    DateTextSupport.normalizeDateText(marriageDateField.getText()),
                    DateTextSupport.clean(marriageNotesArea.getText()),
                    selectedOrdinanceStatus(sealingStatusComboBox),
                    selectedReserved(sealingStatusComboBox),
                    DateTextSupport.normalizeDateText(sealingDateField.getText()),
                    DateTextSupport.clean(sealingNotesArea.getText()),
                    selectedChildIds()
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
        grid.add(fsPidFields.getNode(), 1, row++);

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
        grid.add(sealingNotesArea, 1, row++);

        if (allowChildSelection) {
            grid.add(new Label("Add Existing Children"), 0, row);
            grid.add(childSelectionPane, 1, row);
        }

        preferredNameField.setPrefWidth(320);
        fsPidFields.setPrefWidth(156);
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
        OrdinanceStatusChoice choice = switch (event.getCode()) {
            case C -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.COMPLETE);
            case O -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.OPEN);
            case R -> OrdinanceStatusChoice.reservedChoice();
            case U -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.UNKNOWN);
            case N -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.NOT_APPLICABLE);
            case B -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.BLOCKED_110);
            case DIGIT1, NUMPAD1 -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.SOON_1Y);
            case DIGIT2, NUMPAD2 -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.SOON_2Y);
            case DIGIT5, NUMPAD5 -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.SOON_5Y);
            case DIGIT0, NUMPAD0 -> OrdinanceStatusChoice.fromShortcut(OrdinanceStatus.SOON_10Y);
            default -> null;
        };

        if (choice != null) {
            sealingStatusComboBox.setValue(choice);
            event.consume();
        }
    }

    private void updateMode() {
        boolean creatingNew = createNewPersonCheckBox.isSelected();

        spouseComboBox.setDisable(creatingNew);

        preferredNameField.setDisable(!creatingNew);
        fsPidFields.setDisable(!creatingNew);
        givenNamesField.setDisable(!creatingNew);
        surnameField.setDisable(!creatingNew);
        sexComboBox.setDisable(!creatingNew);
        livingCheckBox.setDisable(!creatingNew);
    }

    private void populateChildChoices(List<Person> childCandidates) {
        childSelectionCheckBoxes.clear();
        childSelectionBox.getChildren().clear();

        if (!allowChildSelection || childCandidates == null || childCandidates.isEmpty()) {
            childSelectionBox.getChildren().add(new Label("No children available to copy."));
            return;
        }

        for (Person child : childCandidates) {
            CheckBox checkBox = new CheckBox(formatPerson(child));
            checkBox.setUserData(child.getPersonId());
            childSelectionCheckBoxes.add(checkBox);
            childSelectionBox.getChildren().add(checkBox);
        }
    }

    private List<Long> selectedChildIds() {
        List<Long> selectedIds = new ArrayList<>();
        for (CheckBox checkBox : childSelectionCheckBoxes) {
            if (checkBox.isSelected() && checkBox.getUserData() instanceof Long personId) {
                selectedIds.add(personId);
            }
        }
        return selectedIds;
    }

    private String validateInputs() {
        if (createNewPersonCheckBox.isSelected()) {
            if (preferredNameField.getText() == null || preferredNameField.getText().isBlank()) {
                return "Please enter a preferred name for the new spouse.";
            }
        } else if (spouseComboBox.getValue() == null) {
            return "Please select a spouse.";
        }

        if (createNewPersonCheckBox.isSelected() && !fsPidFields.isCompleteOrBlank()) {
            return "FamilySearch PID must be entered as four characters plus three characters.";
        }

        return null;
    }

    private String formatPerson(Person person) {
        if (person == null) {
            return "";
        }

        String label = person.getDisplayName();
        if (person.getBirthDateText() != null && !person.getBirthDateText().isBlank()) {
            label += " (" + person.getBirthDateText() + ")";
        }
        if (person.getFsPid() != null && !person.getFsPid().isBlank()) {
            label += " [" + person.getFsPid() + "]";
        }
        return label;
    }

    private OrdinanceStatus selectedOrdinanceStatus(ComboBox<OrdinanceStatusChoice> comboBox) {
        return comboBox.getValue() == null ? OrdinanceStatus.UNKNOWN : comboBox.getValue().status();
    }

    private boolean selectedReserved(ComboBox<OrdinanceStatusChoice> comboBox) {
        return comboBox.getValue() != null && comboBox.getValue().isReserved();
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
        private final boolean sealingReserved;
        private final String sealingStatusDate;
        private final String sealingNotes;
        private final List<Long> childPersonIdsToCopy;

        public Result(
                Long spousePersonId,
                Person newPerson,
                String marriageDateText,
                String marriageNotes,
                OrdinanceStatus sealingStatus,
                boolean sealingReserved,
                String sealingStatusDate,
                String sealingNotes,
                List<Long> childPersonIdsToCopy
        ) {
            this.spousePersonId = spousePersonId;
            this.newPerson = newPerson;
            this.marriageDateText = marriageDateText;
            this.marriageNotes = marriageNotes;
            this.sealingStatus = sealingStatus;
            this.sealingReserved = sealingReserved;
            this.sealingStatusDate = sealingStatusDate;
            this.sealingNotes = sealingNotes;
            this.childPersonIdsToCopy = childPersonIdsToCopy == null ? List.of() : List.copyOf(childPersonIdsToCopy);
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

        public boolean isSealingReserved() {
            return sealingReserved;
        }

        public String getSealingStatusDate() {
            return sealingStatusDate;
        }

        public String getSealingNotes() {
            return sealingNotes;
        }

        public List<Long> getChildPersonIdsToCopy() {
            return childPersonIdsToCopy;
        }
    }
}
