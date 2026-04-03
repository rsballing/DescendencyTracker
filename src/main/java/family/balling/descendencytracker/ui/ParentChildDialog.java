package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.enums.Sex;
import javafx.collections.FXCollections;
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

import java.util.List;

public class ParentChildDialog extends Dialog<ParentChildDialog.Result> {
    private final ComboBox<Person> relatedPersonComboBox = new ComboBox<>();
    private final CheckBox createNewPersonCheckBox = new CheckBox("Create a new person instead");

    private final TextField preferredNameField = new TextField();
    private final TextField fsPidField = new TextField();
    private final TextField givenNamesField = new TextField();
    private final TextField surnameField = new TextField();
    private final ComboBox<Sex> sexComboBox = new ComboBox<>();
    private final CheckBox livingCheckBox = new CheckBox("Living");

    private final TextField childOrderField = new TextField();
    private final TextArea notesArea = new TextArea();

    public ParentChildDialog(boolean addingParent, Person selectedPerson, List<Person> candidates) {
        this(addingParent, selectedPerson, candidates, null);
    }

    public ParentChildDialog(boolean addingParent, Person selectedPerson, List<Person> candidates, ParentChildLink existingLink) {
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

        relatedPersonComboBox.setItems(FXCollections.observableArrayList(candidates));
        relatedPersonComboBox.setPrefWidth(320);

        sexComboBox.getItems().setAll(Sex.values());
        sexComboBox.setValue(Sex.UNKNOWN);

        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(5);
        childOrderField.setPromptText("Optional integer");
        fsPidField.setPromptText("Optional, e.g. KWZ3-ABC");

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

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (createNewPersonCheckBox.isSelected()) {
                if (preferredNameField.getText() == null || preferredNameField.getText().isBlank()) {
                    event.consume();
                    showWarning("Please enter a preferred name for the new person.");
                    return;
                }
            } else {
                if (relatedPersonComboBox.getValue() == null) {
                    event.consume();
                    showWarning("Please select a person.");
                    return;
                }
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
                newPerson.setFsPid(clean(fsPidField.getText()));
                newPerson.setGivenNames(clean(givenNamesField.getText()));
                newPerson.setSurname(clean(surnameField.getText()));
                newPerson.setSex(sexComboBox.getValue());
                newPerson.setLiving(livingCheckBox.isSelected());

                return new Result(
                        null,
                        newPerson,
                        childOrder,
                        clean(notesArea.getText())
                );
            }

            return new Result(
                    relatedPersonComboBox.getValue().getPersonId(),
                    null,
                    childOrder,
                    clean(notesArea.getText())
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

        grid.add(new Label(addingParent ? "New Parent Preferred Name" : "New Child Preferred Name"), 0, row);
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

        grid.add(new Label("Child Order"), 0, row);
        grid.add(childOrderField, 1, row++);

        grid.add(new Label("Notes"), 0, row);
        grid.add(notesArea, 1, row);

        preferredNameField.setPrefWidth(320);
        fsPidField.setPrefWidth(320);
        givenNamesField.setPrefWidth(320);
        surnameField.setPrefWidth(320);

        return grid;
    }

    private void updateMode() {
        boolean creatingNew = createNewPersonCheckBox.isSelected();

        relatedPersonComboBox.setDisable(creatingNew);

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
        private final Long relatedPersonId;
        private final Person newPerson;
        private final Integer childOrder;
        private final String notes;

        public Result(Long relatedPersonId, Person newPerson, Integer childOrder, String notes) {
            this.relatedPersonId = relatedPersonId;
            this.newPerson = newPerson;
            this.childOrder = childOrder;
            this.notes = notes;
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
    }
}