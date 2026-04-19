package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class OrdinancePane {
    private final Label headerLabel = new Label("Select a person to edit ordinances.");
    private final ComboBox<OrdinanceStatusChoice> baptismStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> confirmationStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> initiatoryStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> endowmentStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatusChoice> sealedToParentsStatusCombo = new ComboBox<>();
    private final TextArea ordinanceNotesArea = new TextArea();
    private final VBox spouseSealingEditorBox = new VBox(8);
    private final TableView<OrdinanceEditorRow> ordinanceTable = new TableView<>();
    private final Map<Long, ComboBox<OrdinanceStatusChoice>> spouseSealingStatusEditors = new HashMap<>();
    private final VBox content;

    OrdinancePane(
            Runnable onSave,
            Runnable onReload,
            Consumer<OrdinanceStatus> onSelectedStatusShortcut
    ) {
        configureStatusCombo(baptismStatusCombo);
        configureStatusCombo(confirmationStatusCombo);
        configureStatusCombo(initiatoryStatusCombo);
        configureStatusCombo(endowmentStatusCombo);
        configureStatusCombo(sealedToParentsStatusCombo);

        ordinanceNotesArea.setWrapText(true);
        ordinanceNotesArea.setPrefRowCount(6);
        ordinanceNotesArea.setPrefWidth(360);
        spouseSealingEditorBox.setPadding(new Insets(10));

        configureOrdinanceTable(onSelectedStatusShortcut);

        Button markCompleteButton = new Button("Mark Complete");
        Button markOpenButton = new Button("Mark Open");
        Button markUnknownButton = new Button("Mark Unknown");
        Button saveButton = new Button("Save Ordinances");
        Button refreshButton = new Button("Reload Ordinances");

        markCompleteButton.setOnAction(event -> onSelectedStatusShortcut.accept(OrdinanceStatus.COMPLETE));
        markOpenButton.setOnAction(event -> onSelectedStatusShortcut.accept(OrdinanceStatus.OPEN));
        markUnknownButton.setOnAction(event -> onSelectedStatusShortcut.accept(OrdinanceStatus.UNKNOWN));
        saveButton.setOnAction(event -> onSave.run());
        refreshButton.setOnAction(event -> onReload.run());

        ToolBar toolbar = new ToolBar(
                markCompleteButton,
                markOpenButton,
                markUnknownButton,
                saveButton,
                refreshButton
        );
        toolbar.getStyleClass().add("section-toolbar");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("editor-grid");

        int row = 0;
        grid.add(new Label("Baptism"), 0, row);
        grid.add(baptismStatusCombo, 1, row++);
        grid.add(new Label("Confirmation"), 0, row);
        grid.add(confirmationStatusCombo, 1, row++);
        grid.add(new Label("Initiatory"), 0, row);
        grid.add(initiatoryStatusCombo, 1, row++);
        grid.add(new Label("Endowment"), 0, row);
        grid.add(endowmentStatusCombo, 1, row++);
        grid.add(new Label("Sealed to Parents"), 0, row);
        grid.add(sealedToParentsStatusCombo, 1, row++);
        grid.add(new Label("Ordinance Notes"), 0, row);
        grid.add(ordinanceNotesArea, 1, row);

        TitledPane editorPane = new TitledPane("Person Ordinance Editor", grid);
        editorPane.getStyleClass().add("section-pane");
        editorPane.setCollapsible(false);

        TitledPane spouseSealingPane = new TitledPane("Spouse Sealings", spouseSealingEditorBox);
        spouseSealingPane.getStyleClass().add("section-pane");
        spouseSealingPane.setCollapsible(false);

        content = new VBox(8, headerLabel, toolbar, ordinanceTable, editorPane, spouseSealingPane);
        content.setPadding(new Insets(10));
        content.getStyleClass().add("panel-surface");
        headerLabel.getStyleClass().add("section-title");
        ordinanceTable.getStyleClass().add("compact-table");
    }

    VBox getContent() {
        return content;
    }

    void clear() {
        headerLabel.setText("Select a person to edit ordinances.");
        baptismStatusCombo.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        confirmationStatusCombo.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        initiatoryStatusCombo.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        endowmentStatusCombo.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        sealedToParentsStatusCombo.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        ordinanceNotesArea.setText("");
        ordinanceTable.setItems(FXCollections.observableArrayList());
        refreshSpouseSealingEditor(null, List.of());
    }

    void populate(Person person, PersonOrdinanceStatus ordinanceStatus, List<SpouseLink> spouseLinks) {
        if (person == null || ordinanceStatus == null) {
            clear();
            return;
        }

        headerLabel.setText("Ordinances for " + person.getDisplayName());
        baptismStatusCombo.setValue(OrdinanceStatusChoice.of(ordinanceStatus.getBaptismStatus(), ordinanceStatus.isBaptismReserved()));
        confirmationStatusCombo.setValue(OrdinanceStatusChoice.of(ordinanceStatus.getConfirmationStatus(), ordinanceStatus.isConfirmationReserved()));
        initiatoryStatusCombo.setValue(OrdinanceStatusChoice.of(ordinanceStatus.getInitiatoryStatus(), ordinanceStatus.isInitiatoryReserved()));
        endowmentStatusCombo.setValue(OrdinanceStatusChoice.of(ordinanceStatus.getEndowmentStatus(), ordinanceStatus.isEndowmentReserved()));
        sealedToParentsStatusCombo.setValue(OrdinanceStatusChoice.of(ordinanceStatus.getSealedToParentsStatus(), ordinanceStatus.isSealedToParentsReserved()));
        ordinanceNotesArea.setText(nullSafe(ordinanceStatus.getOrdinanceNotes()));
        ordinanceTable.setItems(FXCollections.observableArrayList(buildOrdinanceRows(person, ordinanceStatus, spouseLinks)));
        refreshSpouseSealingEditor(person, spouseLinks);
    }

    OrdinanceEditorRow getSelectedOrdinanceRow() {
        return ordinanceTable.getSelectionModel().getSelectedItem();
    }

    void reselectOrdinanceRow(OrdinanceEditorRow previousSelection) {
        if (previousSelection == null) {
            return;
        }

        for (OrdinanceEditorRow row : ordinanceTable.getItems()) {
            if (!row.ordinanceName().equals(previousSelection.ordinanceName())) {
                continue;
            }

            if (row.spouseLink() == null && previousSelection.spouseLink() == null) {
                ordinanceTable.getSelectionModel().select(row);
                return;
            }

            if (row.spouseLink() != null
                    && previousSelection.spouseLink() != null
                    && row.spouseLink().getSpouseLinkId() != null
                    && row.spouseLink().getSpouseLinkId().equals(previousSelection.spouseLink().getSpouseLinkId())) {
                ordinanceTable.getSelectionModel().select(row);
                return;
            }
        }
    }

    void applySuggestedStatus(String ordinanceName, OrdinanceStatus status) {
        switch (ordinanceName) {
            case "Baptism" -> baptismStatusCombo.setValue(OrdinanceStatusChoice.fromShortcut(status));
            case "Confirmation" -> confirmationStatusCombo.setValue(OrdinanceStatusChoice.fromShortcut(status));
            case "Initiatory" -> initiatoryStatusCombo.setValue(OrdinanceStatusChoice.fromShortcut(status));
            case "Endowment" -> endowmentStatusCombo.setValue(OrdinanceStatusChoice.fromShortcut(status));
            case "Sealed to Parents" -> sealedToParentsStatusCombo.setValue(OrdinanceStatusChoice.fromShortcut(status));
            default -> {
            }
        }
    }

    PersonOrdinanceStatus buildPersonOrdinanceStatus(Long personId) {
        PersonOrdinanceStatus status = new PersonOrdinanceStatus();
        status.setPersonId(personId);
        status.setBaptismStatus(selectedStatus(baptismStatusCombo));
        status.setBaptismReserved(selectedReserved(baptismStatusCombo));
        status.setConfirmationStatus(selectedStatus(confirmationStatusCombo));
        status.setConfirmationReserved(selectedReserved(confirmationStatusCombo));
        status.setInitiatoryStatus(selectedStatus(initiatoryStatusCombo));
        status.setInitiatoryReserved(selectedReserved(initiatoryStatusCombo));
        status.setEndowmentStatus(selectedStatus(endowmentStatusCombo));
        status.setEndowmentReserved(selectedReserved(endowmentStatusCombo));
        status.setSealedToParentsStatus(selectedStatus(sealedToParentsStatusCombo));
        status.setSealedToParentsReserved(selectedReserved(sealedToParentsStatusCombo));
        status.setOrdinanceNotes(ordinanceNotesArea.getText());
        return status;
    }

    Map<Long, OrdinanceStatusChoice> getSpouseSealingSelections() {
        Map<Long, OrdinanceStatusChoice> selections = new HashMap<>();
        for (Map.Entry<Long, ComboBox<OrdinanceStatusChoice>> entry : spouseSealingStatusEditors.entrySet()) {
            selections.put(entry.getKey(), entry.getValue().getValue());
        }
        return selections;
    }

    private void configureStatusCombo(ComboBox<OrdinanceStatusChoice> comboBox) {
        comboBox.getItems().setAll(OrdinanceStatusChoice.all());
        comboBox.setValue(OrdinanceStatusChoice.of(OrdinanceStatus.UNKNOWN, false));
        comboBox.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            OrdinanceStatusChoice choice = mapStatusShortcut(event.getCode());
            if (choice != null) {
                comboBox.setValue(choice);
                event.consume();
            }
        });
    }

    private void configureOrdinanceTable(Consumer<OrdinanceStatus> onSelectedStatusShortcut) {
        TableColumn<OrdinanceEditorRow, String> ordinanceColumn = new TableColumn<>("Ordinance");
        ordinanceColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().ordinanceName()))
        );
        ordinanceColumn.setPrefWidth(180);

        TableColumn<OrdinanceEditorRow, String> relatedColumn = new TableColumn<>("Related Person");
        relatedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().relatedPersonName()))
        );
        relatedColumn.setPrefWidth(180);

        TableColumn<OrdinanceEditorRow, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().status() == null ? "" : data.getValue().status().name())
        );
        statusColumn.setPrefWidth(140);

        TableColumn<OrdinanceEditorRow, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().details()))
        );
        detailsColumn.setPrefWidth(520);

        ordinanceTable.getColumns().addAll(
                ordinanceColumn,
                relatedColumn,
                statusColumn,
                detailsColumn
        );
        ordinanceTable.setPlaceholder(new Label("No ordinance rows for the selected person."));
        ordinanceTable.setFixedCellSize(23);

        ordinanceTable.setFocusTraversable(true);
        ordinanceTable.setOnKeyPressed(event -> {
            OrdinanceStatusChoice choice = mapStatusShortcut(event.getCode());
            if (choice != null) {
                onSelectedStatusShortcut.accept(choice.status());
                event.consume();
            }
        });
    }

    private List<OrdinanceEditorRow> buildOrdinanceRows(
            Person person,
            PersonOrdinanceStatus ordinanceStatus,
            List<SpouseLink> spouseLinks
    ) {
        List<OrdinanceEditorRow> rows = FXCollections.observableArrayList();
        rows.add(new OrdinanceEditorRow("Baptism", "", ordinanceStatus.getBaptismStatus(), buildPersonOrdinanceDetails(ordinanceStatus), null));
        rows.add(new OrdinanceEditorRow("Confirmation", "", ordinanceStatus.getConfirmationStatus(), buildPersonOrdinanceDetails(ordinanceStatus), null));
        rows.add(new OrdinanceEditorRow("Initiatory", "", ordinanceStatus.getInitiatoryStatus(), buildPersonOrdinanceDetails(ordinanceStatus), null));
        rows.add(new OrdinanceEditorRow("Endowment", "", ordinanceStatus.getEndowmentStatus(), buildPersonOrdinanceDetails(ordinanceStatus), null));
        rows.add(new OrdinanceEditorRow("Sealed to Parents", "", ordinanceStatus.getSealedToParentsStatus(), buildPersonOrdinanceDetails(ordinanceStatus), null));

        for (SpouseLink spouseLink : spouseLinks) {
            rows.add(new OrdinanceEditorRow(
                    "Sealed to Spouse",
                    spouseLink.getOtherPersonDisplayName(person.getPersonId()),
                    spouseLink.getSealingToSpouseStatus(),
                    buildSpouseOrdinanceDetails(spouseLink),
                    spouseLink
            ));
        }

        return rows;
    }

    private String buildPersonOrdinanceDetails(PersonOrdinanceStatus ordinanceStatus) {
        String notes = nullSafe(ordinanceStatus.getOrdinanceNotes()).trim();
        String reserved = buildReservedSummary(ordinanceStatus);
        if (notes.isBlank()) {
            return reserved;
        }
        return reserved.isBlank() ? "Notes: " + notes : reserved + " | Notes: " + notes;
    }

    private void refreshSpouseSealingEditor(Person person, List<SpouseLink> spouseLinks) {
        spouseSealingEditorBox.getChildren().clear();
        spouseSealingStatusEditors.clear();

        if (person == null || person.getPersonId() == null) {
            spouseSealingEditorBox.getChildren().add(new Label("Select a person to edit spouse sealings."));
            return;
        }

        if (spouseLinks == null || spouseLinks.isEmpty()) {
            spouseSealingEditorBox.getChildren().add(new Label("No spouse relationships are recorded for this person."));
            return;
        }

        for (SpouseLink spouseLink : spouseLinks) {
            Label spouseLabel = new Label(nullSafe(spouseLink.getOtherPersonDisplayName(person.getPersonId())));
            spouseLabel.setMinWidth(220);

            ComboBox<OrdinanceStatusChoice> sealingStatusCombo = new ComboBox<>();
            configureStatusCombo(sealingStatusCombo);
            sealingStatusCombo.setValue(OrdinanceStatusChoice.of(
                    spouseLink.getSealingToSpouseStatus(),
                    spouseLink.isSealedToSpouseReserved()
            ));
            sealingStatusCombo.setPrefWidth(180);
            spouseSealingStatusEditors.put(spouseLink.getSpouseLinkId(), sealingStatusCombo);

            Label detailsLabel = new Label(buildSpouseOrdinanceDetails(spouseLink));
            detailsLabel.setWrapText(true);
            detailsLabel.setMaxWidth(420);
            detailsLabel.getStyleClass().add("muted-text");

            HBox row = new HBox(10, spouseLabel, sealingStatusCombo, detailsLabel);
            row.setFillHeight(true);
            row.getStyleClass().add("inline-editor-row");
            spouseSealingEditorBox.getChildren().add(row);
        }
    }

    private String buildSpouseOrdinanceDetails(SpouseLink spouseLink) {
        StringBuilder details = new StringBuilder();
        appendDetail(details, "Marriage", spouseLink.getMarriageDateText());
        appendDetail(details, "Sealing Date", spouseLink.getSealingStatusDate());
        if (spouseLink.isSealedToSpouseReserved()) {
            appendDetail(details, null, "Reserved");
        }

        String marriageNotes = nullSafe(spouseLink.getMarriageNotes()).trim();
        String sealingNotes = nullSafe(spouseLink.getSealingNotes()).trim();
        if (!marriageNotes.isBlank() || !sealingNotes.isBlank()) {
            String notes = "";
            if (!marriageNotes.isBlank()) {
                notes = "Marriage notes: " + marriageNotes;
            }
            if (!sealingNotes.isBlank()) {
                notes = notes.isBlank() ? "Sealing notes: " + sealingNotes : notes + " | Sealing notes: " + sealingNotes;
            }
            appendDetail(details, null, notes);
        }

        return details.toString();
    }

    private String buildReservedSummary(PersonOrdinanceStatus ordinanceStatus) {
        List<String> reserved = FXCollections.observableArrayList();
        if (ordinanceStatus.isBaptismReserved()) {
            reserved.add("Baptism reserved");
        }
        if (ordinanceStatus.isConfirmationReserved()) {
            reserved.add("Confirmation reserved");
        }
        if (ordinanceStatus.isInitiatoryReserved()) {
            reserved.add("Initiatory reserved");
        }
        if (ordinanceStatus.isEndowmentReserved()) {
            reserved.add("Endowment reserved");
        }
        if (ordinanceStatus.isSealedToParentsReserved()) {
            reserved.add("Sealed to Parents reserved");
        }
        return String.join(", ", reserved);
    }

    private void appendDetail(StringBuilder builder, String label, String value) {
        String cleanedValue = nullSafe(value).trim();
        if (cleanedValue.isBlank()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(" | ");
        }

        if (label != null && !label.isBlank()) {
            builder.append(label).append(": ");
        }
        builder.append(cleanedValue);
    }

    private OrdinanceStatusChoice mapStatusShortcut(KeyCode keyCode) {
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

    private OrdinanceStatus selectedStatus(ComboBox<OrdinanceStatusChoice> comboBox) {
        return comboBox.getValue() == null ? OrdinanceStatus.UNKNOWN : comboBox.getValue().status();
    }

    private boolean selectedReserved(ComboBox<OrdinanceStatusChoice> comboBox) {
        return comboBox.getValue() != null && comboBox.getValue().isReserved();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    record OrdinanceEditorRow(
            String ordinanceName,
            String relatedPersonName,
            OrdinanceStatus status,
            String details,
            SpouseLink spouseLink
    ) {
    }
}
