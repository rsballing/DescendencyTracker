package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ParentChildDialog extends Dialog<ParentChildDialog.Result> {
    private static final KeyCombination SUBMIT_SHORTCUT = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);

    private final ComboBox<Person> relatedPersonComboBox = new ComboBox<>();
    private final CheckBox createNewPersonCheckBox = new CheckBox("Create a new person instead");
    private final CheckBox mirrorToSpouseCheckBox = new CheckBox();
    private final ComboBox<Person> mirrorSpouseComboBox = new ComboBox<>();

    private final TextField preferredNameField = new TextField();
    private final FsPidFields fsPidFields = new FsPidFields();
    private final TextField givenNamesField = new TextField();
    private final TextField surnameField = new TextField();
    private final ComboBox<Sex> sexComboBox = new ComboBox<>();
    private final CheckBox livingCheckBox = new CheckBox("Living");

    private final TextField childOrderField = new TextField();
    private final TextArea notesArea = new TextArea();
    private final boolean addingParent;
    private final Person selectedPerson;
    private final boolean allowSpouseMirror;
    private final Map<Long, List<Person>> spouseCandidatesByParentId;

    public ParentChildDialog(boolean addingParent, Person selectedPerson, List<Person> candidates) {
        this(addingParent, selectedPerson, candidates, Collections.emptyMap(), null);
    }

    public ParentChildDialog(boolean addingParent, Person selectedPerson, List<Person> candidates, ParentChildLink existingLink) {
        this(addingParent, selectedPerson, candidates, Collections.emptyMap(), existingLink);
    }

    public ParentChildDialog(
            boolean addingParent,
            Person selectedPerson,
            List<Person> candidates,
            Map<Long, List<Person>> spouseCandidatesByParentId,
            ParentChildLink existingLink
    ) {
        this.addingParent = addingParent;
        this.selectedPerson = selectedPerson;
        this.allowSpouseMirror = existingLink == null;
        this.spouseCandidatesByParentId = spouseCandidatesByParentId == null ? Collections.emptyMap() : spouseCandidatesByParentId;
        mirrorToSpouseCheckBox.setText(addingParent
                ? "Also add this child to another spouse of the selected parent"
                : "Also add this child to a spouse of " + selectedPerson.getDisplayName());

        setTitle(existingLink == null
                ? (addingParent ? "Add Parent" : "Add Child")
                : (addingParent ? "Edit Parent Link" : "Edit Child Link"));

        setHeaderText(existingLink == null
                ? (addingParent
                   ? "Add a parent relationship for " + selectedPerson.getDisplayName()
                   : "Add a child relationship for " + selectedPerson.getDisplayName())
                : (addingParent
                   ? "Edit the selected parent relationship for " + selectedPerson.getDisplayName()
                   : "Edit the selected child relationship for " + selectedPerson.getDisplayName()));

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setContent(buildForm(addingParent));
        DialogThemeSupport.apply(this);

        relatedPersonComboBox.setPrefWidth(320);
        mirrorSpouseComboBox.setPrefWidth(320);
        mirrorSpouseComboBox.setEditable(false);
        mirrorSpouseComboBox.setPromptText("Select a spouse to mirror");
        mirrorSpouseComboBox.setButtonCell(createPersonCell());
        mirrorSpouseComboBox.setCellFactory(ignored -> createPersonCell());
        PersonSelectionSupport.configurePersonAutocomplete(
                relatedPersonComboBox,
                candidates,
                addingParent ? "Type to find an existing parent" : "Type to find an existing child"
        );
        mirrorSpouseComboBox.setItems(FXCollections.observableArrayList());

        sexComboBox.getItems().setAll(Sex.values());
        sexComboBox.setValue(Sex.UNKNOWN);

        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(5);
        childOrderField.setPromptText("Optional integer");
        fsPidFields.setValue(null);
        sexComboBox.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSexShortcut);

        if (existingLink != null) {
            Long relatedPersonId = addingParent ? existingLink.getParentPersonId() : existingLink.getChildPersonId();
            for (Person candidate : candidates) {
                if (candidate.getPersonId().equals(relatedPersonId)) {
                    relatedPersonComboBox.setValue(candidate);
                    break;
                }
            }

            if (existingLink.getChildOrder() != null) {
                childOrderField.setText(String.valueOf(existingLink.getChildOrder()));
            }
            notesArea.setText(existingLink.getNotes() == null ? "" : existingLink.getNotes());
            createNewPersonCheckBox.setSelected(false);
        } else {
            createNewPersonCheckBox.setSelected(candidates.isEmpty());
        }

        updateMode();
        createNewPersonCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateMode());
        relatedPersonComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateMirrorSpouseChoices());
        mirrorToSpouseCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateMirrorSpouseControls());
        mirrorSpouseComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (allowSpouseMirror && newValue != null && !mirrorToSpouseCheckBox.isSelected() && !mirrorToSpouseCheckBox.isDisabled()) {
                mirrorToSpouseCheckBox.setSelected(true);
            }
        });
        updateMirrorSpouseChoices();
        focusPrimaryInput();

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (createNewPersonCheckBox.isSelected()) {
                if (preferredNameField.getText() == null || preferredNameField.getText().isBlank()) {
                    event.consume();
                    showWarning("Please enter a preferred name for the new person.");
                    return;
                }
                if (!fsPidFields.isCompleteOrBlank()) {
                    event.consume();
                    showWarning("FamilySearch PID must be entered as four characters plus three characters.");
                    return;
                }
            } else {
                if (relatedPersonComboBox.getValue() == null) {
                    event.consume();
                    showWarning("Please select a person.");
                    return;
                }
            }

            if (allowSpouseMirror && mirrorToSpouseCheckBox.isSelected() && mirrorSpouseComboBox.getValue() == null) {
                event.consume();
                showWarning("Please select the spouse that should also be linked to this child.");
                return;
            }

            String orderText = childOrderField.getText();
            if (orderText != null && !orderText.isBlank()) {
                try {
                    Integer.parseInt(orderText.trim());
                } catch (NumberFormatException ex) {
                    event.consume();
                    showWarning("Child order must be a whole number.");
                }
            }
        });

        configureSubmitShortcut();

        setResultConverter(buttonType -> {
            if (buttonType != ButtonType.OK) {
                return null;
            }

            Integer childOrder = null;
            String orderText = childOrderField.getText();
            if (orderText != null && !orderText.isBlank()) {
                childOrder = Integer.parseInt(orderText.trim());
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
                        childOrder,
                        clean(notesArea.getText()),
                        allowSpouseMirror && mirrorToSpouseCheckBox.isSelected() && mirrorSpouseComboBox.getValue() != null
                                ? mirrorSpouseComboBox.getValue().getPersonId()
                                : null
                );
            }

            return new Result(
                    relatedPersonComboBox.getValue().getPersonId(),
                    null,
                    childOrder,
                    clean(notesArea.getText()),
                    allowSpouseMirror && mirrorToSpouseCheckBox.isSelected() && mirrorSpouseComboBox.getValue() != null
                            ? mirrorSpouseComboBox.getValue().getPersonId()
                            : null
            );
        });
    }

    private GridPane buildForm(boolean addingParent) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;

        grid.add(new Label(addingParent ? "Existing Parent" : "Existing Child"), 0, row);
        grid.add(relatedPersonComboBox, 1, row++);

        grid.add(new Label("Mode"), 0, row);
        grid.add(createNewPersonCheckBox, 1, row++);

        if (allowSpouseMirror) {
            grid.add(new Label("Mirror to Spouse"), 0, row);
            grid.add(mirrorToSpouseCheckBox, 1, row++);

            grid.add(new Label("Selected Spouse"), 0, row);
            grid.add(mirrorSpouseComboBox, 1, row++);
        }

        grid.add(new Label(addingParent ? "New Parent Preferred Name" : "New Child Preferred Name"), 0, row);
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

        grid.add(new Label("Child Order"), 0, row);
        grid.add(childOrderField, 1, row++);

        grid.add(new Label("Notes"), 0, row);
        grid.add(notesArea, 1, row);

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

            relatedPersonComboBox.requestFocus();
            if (relatedPersonComboBox.isEditable()) {
                relatedPersonComboBox.getEditor().requestFocus();
                relatedPersonComboBox.getEditor().selectAll();
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

    private void updateMode() {
        boolean creatingNew = createNewPersonCheckBox.isSelected();

        relatedPersonComboBox.setDisable(creatingNew);

        preferredNameField.setDisable(!creatingNew);
        fsPidFields.setDisable(!creatingNew);
        givenNamesField.setDisable(!creatingNew);
        surnameField.setDisable(!creatingNew);
        sexComboBox.setDisable(!creatingNew);
        livingCheckBox.setDisable(!creatingNew);
        updateMirrorSpouseControls();
    }

    private void updateMirrorSpouseChoices() {
        if (!allowSpouseMirror) {
            return;
        }

        Person selectedParent = addingParent ? relatedPersonComboBox.getValue() : selectedPerson;
        List<Person> spouseCandidates = selectedParent == null || selectedParent.getPersonId() == null
                ? List.of()
                : spouseCandidatesByParentId.getOrDefault(selectedParent.getPersonId(), List.of());

        mirrorSpouseComboBox.setItems(FXCollections.observableArrayList(spouseCandidates));
        Person selectedSpouse = mirrorSpouseComboBox.getValue();
        if (selectedSpouse != null && !spouseCandidates.contains(selectedSpouse)) {
            mirrorSpouseComboBox.setValue(null);
        }
        if (mirrorSpouseComboBox.getValue() == null && spouseCandidates.size() == 1) {
            mirrorSpouseComboBox.setValue(spouseCandidates.get(0));
        }

        if (spouseCandidates.size() == 1 && (!addingParent || !createNewPersonCheckBox.isSelected())) {
            mirrorToSpouseCheckBox.setSelected(true);
        }

        if (spouseCandidates.isEmpty()) {
            mirrorToSpouseCheckBox.setSelected(false);
        }

        updateMirrorSpouseControls();
    }

    private void updateMirrorSpouseControls() {
        if (!allowSpouseMirror) {
            return;
        }

        boolean hasSpouses = !mirrorSpouseComboBox.getItems().isEmpty();
        boolean mirrorEnabled = hasSpouses && (!addingParent || !createNewPersonCheckBox.isSelected());

        mirrorToSpouseCheckBox.setDisable(!mirrorEnabled);
        if (!mirrorEnabled) {
            mirrorToSpouseCheckBox.setSelected(false);
        }

        mirrorSpouseComboBox.setDisable(!mirrorEnabled);
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

    private ListCell<Person> createPersonCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                String text = item.getDisplayName();
                if (item.getFsPid() != null && !item.getFsPid().isBlank()) {
                    text += " [" + item.getFsPid() + "]";
                }
                setText(text);
            }
        };
    }

    public static class Result {
        private final Long relatedPersonId;
        private final Person newPerson;
        private final Integer childOrder;
        private final String notes;
        private final Long mirrorSpousePersonId;

        public Result(Long relatedPersonId, Person newPerson, Integer childOrder, String notes, Long mirrorSpousePersonId) {
            this.relatedPersonId = relatedPersonId;
            this.newPerson = newPerson;
            this.childOrder = childOrder;
            this.notes = notes;
            this.mirrorSpousePersonId = mirrorSpousePersonId;
        }

        public Long getRelatedPersonId() {
            return relatedPersonId;
        }

        public Person getNewPerson() {
            return newPerson;
        }

        public Integer getChildOrder() {
            return childOrder;
        }

        public String getNotes() {
            return notes;
        }

        public Long getMirrorSpousePersonId() {
            return mirrorSpousePersonId;
        }
    }
}
