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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
    private final ComboBox<OrdinanceStatusChoice> baptismStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> confirmationStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> initiatoryStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> endowmentStatusComboBox = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> sealedToParentsStatusComboBox = new ComboBox<>();
    private final CheckBox confirmedNoChildrenCheckBox = new CheckBox("Confirmed no children");
    private final CheckBox confirmedNoSpouseCheckBox = new CheckBox("Confirmed no spouse");
    private final CheckBox nonBloodRelativeCheckBox = new CheckBox("Non-blood relative");
    private final CheckBox noMoreFindableCheckBox = new CheckBox("No more findable");
    private final TextArea ordinanceNotesArea = new TextArea();
    private final PersonOrdinanceStatus existingOrdinanceStatus;

    public PersonEditorDialog(Person existingPerson, PersonOrdinanceStatus existingOrdinanceStatus) {
        this.existingPerson = existingPerson;
        this.existingOrdinanceStatus = existingOrdinanceStatus;

        setTitle(existingPerson == null ? "Add Person" : "Edit Person");
        setHeaderText(existingPerson == null ? "Create a new person." : "Edit the selected person.");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setContent(buildForm());
        DialogThemeSupport.apply(this);

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
            result.setConfirmedNoChildren(confirmedNoChildrenCheckBox.isSelected());
            result.setConfirmedNoSpouse(confirmedNoSpouseCheckBox.isSelected());
            result.setNonBloodRelative(nonBloodRelativeCheckBox.isSelected());
            result.setNoMoreFindable(noMoreFindableCheckBox.isSelected());

            PersonOrdinanceStatus ordinanceStatus = existingOrdinanceStatus == null
                    ? new PersonOrdinanceStatus()
                    : existingOrdinanceStatus;
            ordinanceStatus.setBaptismStatus(selectedOrdinanceStatus(baptismStatusComboBox));
            ordinanceStatus.setBaptismReserved(selectedReserved(baptismStatusComboBox));
            ordinanceStatus.setConfirmationStatus(selectedOrdinanceStatus(confirmationStatusComboBox));
            ordinanceStatus.setConfirmationReserved(selectedReserved(confirmationStatusComboBox));
            ordinanceStatus.setInitiatoryStatus(selectedOrdinanceStatus(initiatoryStatusComboBox));
            ordinanceStatus.setInitiatoryReserved(selectedReserved(initiatoryStatusComboBox));
            ordinanceStatus.setEndowmentStatus(selectedOrdinanceStatus(endowmentStatusComboBox));
            ordinanceStatus.setEndowmentReserved(selectedReserved(endowmentStatusComboBox));
            ordinanceStatus.setSealedToParentsStatus(selectedOrdinanceStatus(sealedToParentsStatusComboBox));
            ordinanceStatus.setSealedToParentsReserved(selectedReserved(sealedToParentsStatusComboBox));
            ordinanceStatus.setOrdinanceNotes(DateTextSupport.clean(ordinanceNotesArea.getText()));

            return new Result(result, ordinanceStatus);
        });
    }

    private VBox buildForm() {
        preferredNameField.setPrefWidth(260);
        fsPidFields.setPrefWidth(120);
        givenNamesField.setPrefWidth(260);
        surnameField.setPrefWidth(260);
        birthDateField.setPrefWidth(220);
        deathDateField.setPrefWidth(220);
        birthPrecisionComboBox.setPrefWidth(180);
        deathPrecisionComboBox.setPrefWidth(180);
        reviewedStatusComboBox.setPrefWidth(220);

        Label heading = new Label(existingPerson == null ? "New Person" : nullSafe(existingPerson.getDisplayName()));
        heading.getStyleClass().add("section-title");
        Label subheading = new Label(existingPerson == null
                ? "Start with identity and life details, then record ordinances."
                : "Update the core record first, then review ordinances.");
        subheading.getStyleClass().add("muted-text");

        HBox identityRowOne = new HBox(12,
                createFieldBox("Preferred Name", preferredNameField),
                createFieldBox("FamilySearch PID", fsPidFields.getNode())
        );
        HBox.setHgrow(identityRowOne.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(identityRowOne.getChildren().get(1), Priority.ALWAYS);

        HBox identityRowTwo = new HBox(12,
                createFieldBox("Given Names", givenNamesField),
                createFieldBox("Surname", surnameField)
        );
        HBox.setHgrow(identityRowTwo.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(identityRowTwo.getChildren().get(1), Priority.ALWAYS);

        HBox identityRowThree = new HBox(12,
                createFieldBox("Sex", sexComboBox),
                createFieldBox("Status", livingCheckBox)
        );
        HBox.setHgrow(identityRowThree.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(identityRowThree.getChildren().get(1), Priority.ALWAYS);

        VBox identitySection = buildSectionBox(
                heading,
                subheading,
                identityRowOne,
                identityRowTwo,
                identityRowThree
        );

        HBox datesRowOne = new HBox(12,
                createFieldBox("Birth Date", birthDateField),
                createFieldBox("Birth Precision", birthPrecisionComboBox)
        );
        HBox.setHgrow(datesRowOne.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(datesRowOne.getChildren().get(1), Priority.ALWAYS);

        HBox datesRowTwo = new HBox(12,
                createFieldBox("Death Date", deathDateField),
                createFieldBox("Death Precision", deathPrecisionComboBox)
        );
        HBox.setHgrow(datesRowTwo.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(datesRowTwo.getChildren().get(1), Priority.ALWAYS);

        VBox lifeSection = buildSectionBox(
                datesRowOne,
                datesRowTwo,
                createFieldBox("Reviewed Status", reviewedStatusComboBox)
        );

        HBox confirmationsRow = new HBox(12, confirmedNoChildrenCheckBox, confirmedNoSpouseCheckBox);
        confirmationsRow.getStyleClass().add("detail-card");
        HBox statusFlagsRow = new HBox(12, nonBloodRelativeCheckBox, noMoreFindableCheckBox);
        statusFlagsRow.getStyleClass().add("detail-card");

        VBox relationshipSection = buildSectionBox(confirmationsRow, statusFlagsRow);

        HBox ordinanceRowOne = new HBox(12,
                createFieldBox("Baptism", baptismStatusComboBox),
                createFieldBox("Confirmation", confirmationStatusComboBox)
        );
        HBox.setHgrow(ordinanceRowOne.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(ordinanceRowOne.getChildren().get(1), Priority.ALWAYS);

        HBox ordinanceRowTwo = new HBox(12,
                createFieldBox("Initiatory", initiatoryStatusComboBox),
                createFieldBox("Endowment", endowmentStatusComboBox)
        );
        HBox.setHgrow(ordinanceRowTwo.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(ordinanceRowTwo.getChildren().get(1), Priority.ALWAYS);

        VBox ordinanceSection = buildSectionBox(
                ordinanceRowOne,
                ordinanceRowTwo,
                createFieldBox("Sealed to Parents", sealedToParentsStatusComboBox)
        );
        TitledPane ordinancesPane = buildSectionPane("Ordinances", ordinanceSection, true);

        TitledPane identityPane = buildSectionPane("Identity", identitySection, true);
        TitledPane lifePane = buildSectionPane("Life Details", lifeSection, true);
        TitledPane relationshipPane = buildSectionPane("Relationship Status", relationshipSection, true);
        Button addNotesButton = new Button("Add Notes");
        addNotesButton.setOnAction(event -> showNotesDialog());
        VBox notesActions = buildSectionBox(addNotesButton);
        TitledPane notesPane = buildSectionPane("Notes", notesActions, true);

        VBox leftColumn = new VBox(10, lifePane, ordinancesPane);
        VBox rightColumn = new VBox(10, relationshipPane, notesPane);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox lowerRow = new HBox(10, leftColumn, rightColumn);
        HBox.setHgrow(lowerRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lowerRow.getChildren().get(1), Priority.ALWAYS);

        VBox content = new VBox(10, identityPane, lowerRow);
        content.setPadding(new Insets(12));
        content.getStyleClass().add("dialog-form-stack");
        return content;
    }

    private TitledPane buildSectionPane(String title, Node content, boolean expanded) {
        TitledPane pane = new TitledPane(title, content);
        pane.getStyleClass().add("section-pane");
        pane.setCollapsible(false);
        pane.setExpanded(expanded);
        return pane;
    }

    private VBox buildSectionBox(Node... nodes) {
        VBox box = new VBox(10, nodes);
        box.setPadding(new Insets(10));
        box.getStyleClass().addAll("section-body", "dialog-section-body");
        return box;
    }

    private VBox createFieldBox(String labelText, Node input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("selected-person-prefix");

        VBox box = new VBox(4, label, input);
        box.getStyleClass().add("dialog-field-box");
        VBox.setVgrow(input, Priority.NEVER);
        if (input instanceof TextArea area) {
            VBox.setVgrow(area, Priority.ALWAYS);
        } else if (input instanceof TextField field) {
            field.setMaxWidth(Double.MAX_VALUE);
        } else if (input instanceof ComboBox<?> comboBox) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
        } else if (input instanceof HBox hbox) {
            HBox.setHgrow(hbox, Priority.ALWAYS);
        }
        return box;
    }

    private void configureControls() {
        sexComboBox.getItems().setAll(Sex.values());
        birthPrecisionComboBox.getItems().setAll(DatePrecision.values());
        deathPrecisionComboBox.getItems().setAll(DatePrecision.values());
        reviewedStatusComboBox.getItems().setAll(ReviewedStatus.values());
        baptismStatusComboBox.getItems().setAll(OrdinanceStatusChoice.all());
        confirmationStatusComboBox.getItems().setAll(OrdinanceStatusChoice.all());
        initiatoryStatusComboBox.getItems().setAll(OrdinanceStatusChoice.all());
        endowmentStatusComboBox.getItems().setAll(OrdinanceStatusChoice.all());
        sealedToParentsStatusComboBox.getItems().setAll(OrdinanceStatusChoice.all());

        preferredNameField.setPromptText("Required");
        fsPidFields.setValue(null);
        birthDateField.setPromptText("Optional text, e.g. 14 Jun 1904 or 1904");
        deathDateField.setPromptText("Optional text, e.g. 9 Mar 1988 or 1988");

        sexComboBox.setValue(Sex.UNKNOWN);
        birthPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
        deathPrecisionComboBox.setValue(DatePrecision.UNKNOWN);
        reviewedStatusComboBox.setValue(ReviewedStatus.NOT_REVIEWED);
        baptismStatusComboBox.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        confirmationStatusComboBox.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        initiatoryStatusComboBox.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        endowmentStatusComboBox.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        sealedToParentsStatusComboBox.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        confirmedNoChildrenCheckBox.setSelected(false);
        confirmedNoSpouseCheckBox.setSelected(false);
        nonBloodRelativeCheckBox.setSelected(false);
        noMoreFindableCheckBox.setSelected(false);
        noMoreFindableCheckBox.setTooltip(new javafx.scene.control.Tooltip("Marks a likely incomplete person whose remaining information is no longer findable."));

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

        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(8);
        ordinanceNotesArea.setWrapText(true);
        ordinanceNotesArea.setPrefRowCount(4);
    }

    private void showNotesDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Person Notes");
        dialog.setHeaderText(existingPerson == null ? "Add general notes." : "Edit general notes for " + nullSafe(existingPerson.getDisplayName()) + ".");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea dialogNotesArea = new TextArea(nullSafe(notesArea.getText()));
        dialogNotesArea.setWrapText(true);
        dialogNotesArea.setPrefRowCount(10);
        dialogNotesArea.setPrefColumnCount(48);
        dialog.getDialogPane().setContent(dialogNotesArea);
        DialogThemeSupport.apply(dialog);

        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? dialogNotesArea.getText()
                : null);

        dialog.showAndWait().ifPresent(notesArea::setText);
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
        confirmedNoChildrenCheckBox.setSelected(existingPerson.isConfirmedNoChildren());
        confirmedNoSpouseCheckBox.setSelected(existingPerson.isConfirmedNoSpouse());
        nonBloodRelativeCheckBox.setSelected(existingPerson.isNonBloodRelative());
        noMoreFindableCheckBox.setSelected(existingPerson.isNoMoreFindable());
        if (existingOrdinanceStatus != null) {
            baptismStatusComboBox.setValue(OrdinanceStatusChoice.of(existingOrdinanceStatus.getBaptismStatus(), existingOrdinanceStatus.isBaptismReserved()));
            confirmationStatusComboBox.setValue(OrdinanceStatusChoice.of(existingOrdinanceStatus.getConfirmationStatus(), existingOrdinanceStatus.isConfirmationReserved()));
            initiatoryStatusComboBox.setValue(OrdinanceStatusChoice.of(existingOrdinanceStatus.getInitiatoryStatus(), existingOrdinanceStatus.isInitiatoryReserved()));
            endowmentStatusComboBox.setValue(OrdinanceStatusChoice.of(existingOrdinanceStatus.getEndowmentStatus(), existingOrdinanceStatus.isEndowmentReserved()));
            sealedToParentsStatusComboBox.setValue(OrdinanceStatusChoice.of(existingOrdinanceStatus.getSealedToParentsStatus(), existingOrdinanceStatus.isSealedToParentsReserved()));
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

    private void configureOrdinanceShortcut(ComboBox<OrdinanceStatusChoice> comboBox) {
        comboBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            OrdinanceStatusChoice choice = mapOrdinanceShortcut(event.getCode());
            if (choice != null) {
                comboBox.setValue(choice);
                event.consume();
            }
        });
    }

    private OrdinanceStatusChoice mapOrdinanceShortcut(KeyCode keyCode) {
        return switch (keyCode) {
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
