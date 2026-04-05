package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.AncestorLineSummary;
import family.balling.descendencytracker.domain.enums.LineStewardshipStatus;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

final class AncestorLinesPane {
    private final ObservableList<LineWorkbenchRow> allLineWorkbenchRows = FXCollections.observableArrayList();
    private final FilteredList<LineWorkbenchRow> filteredLineWorkbenchRows = new FilteredList<>(allLineWorkbenchRows, row -> true);
    private final SortedList<LineWorkbenchRow> sortedLineWorkbenchRows = new SortedList<>(filteredLineWorkbenchRows);

    private final TableView<AncestorLineSummary> ancestorLineTable = new TableView<>();
    private final TableView<LineWorkbenchRow> lineWorkbenchTable = new TableView<>();
    private final ComboBox<String> lineWorkbenchFilterCombo = new ComboBox<>();
    private final Label ancestorStewardshipAncestorValue = new Label("Select an ancestor line to manage stewardship.");
    private final Label lineWorkbenchAncestorValue = new Label("Select an ancestor line to open the workbench.");
    private final ComboBox<LineStewardshipStatus> ancestorStewardshipStatusCombo = new ComboBox<>();
    private final TextArea ancestorStewardshipNotesArea = new TextArea();
    private final VBox content;

    AncestorLinesPane(
            Runnable onOpenAncestor,
            Runnable onRefreshLines,
            Runnable onSaveStewardship,
            Runnable onReloadStewardship,
            Runnable onOpenWorkbenchPerson,
            Consumer<OrdinanceStatus> onUpdateWorkbenchStatus,
            Runnable onRefreshWorkbench,
            Consumer<AncestorLineSummary> onSelectedAncestorChanged
    ) {
        configureAncestorLineTable(onOpenAncestor, onSelectedAncestorChanged);
        configureLineWorkbenchControls();
        configureLineWorkbenchTable(onOpenWorkbenchPerson, onUpdateWorkbenchStatus);
        configureStewardshipControls();

        sortedLineWorkbenchRows.comparatorProperty().bind(lineWorkbenchTable.comparatorProperty());
        lineWorkbenchTable.setItems(sortedLineWorkbenchRows);

        Button openAncestorButton = new Button("Open Selected Ancestor");
        Button refreshButton = new Button("Refresh Ancestor Lines");
        Button saveStewardshipButton = new Button("Save Stewardship");
        Button reloadStewardshipButton = new Button("Reload Stewardship");
        Button openWorkbenchPersonButton = new Button("Open Selected Person");
        Button markWorkbenchCompleteButton = new Button("Complete");
        Button markWorkbenchOpenButton = new Button("Open");
        Button markWorkbenchUnknownButton = new Button("Unknown");
        Button refreshWorkbenchButton = new Button("Refresh Workbench");

        openAncestorButton.setOnAction(event -> onOpenAncestor.run());
        refreshButton.setOnAction(event -> onRefreshLines.run());
        saveStewardshipButton.setOnAction(event -> onSaveStewardship.run());
        reloadStewardshipButton.setOnAction(event -> onReloadStewardship.run());
        openWorkbenchPersonButton.setOnAction(event -> onOpenWorkbenchPerson.run());
        markWorkbenchCompleteButton.setOnAction(event -> onUpdateWorkbenchStatus.accept(OrdinanceStatus.COMPLETE));
        markWorkbenchOpenButton.setOnAction(event -> onUpdateWorkbenchStatus.accept(OrdinanceStatus.OPEN));
        markWorkbenchUnknownButton.setOnAction(event -> onUpdateWorkbenchStatus.accept(OrdinanceStatus.UNKNOWN));
        refreshWorkbenchButton.setOnAction(event -> onRefreshWorkbench.run());

        ToolBar toolbar = new ToolBar(
                openAncestorButton,
                refreshButton,
                saveStewardshipButton,
                reloadStewardshipButton
        );

        GridPane stewardshipGrid = new GridPane();
        stewardshipGrid.setHgap(10);
        stewardshipGrid.setVgap(10);
        stewardshipGrid.setPadding(new Insets(10));
        stewardshipGrid.add(new Label("Selected Ancestor"), 0, 0);
        stewardshipGrid.add(ancestorStewardshipAncestorValue, 1, 0);
        stewardshipGrid.add(new Label("Stewardship"), 0, 1);
        stewardshipGrid.add(ancestorStewardshipStatusCombo, 1, 1);
        stewardshipGrid.add(new Label("Notes"), 0, 2);
        stewardshipGrid.add(ancestorStewardshipNotesArea, 1, 2);

        TitledPane stewardshipPane = new TitledPane("Line Stewardship", stewardshipGrid);
        stewardshipPane.setCollapsible(false);

        ToolBar workbenchToolbar = new ToolBar(
                openWorkbenchPersonButton,
                markWorkbenchCompleteButton,
                markWorkbenchOpenButton,
                markWorkbenchUnknownButton,
                refreshWorkbenchButton,
                new Label("Show"),
                lineWorkbenchFilterCombo
        );

        VBox workbenchContent = new VBox(8, lineWorkbenchAncestorValue, workbenchToolbar, lineWorkbenchTable);
        TitledPane workbenchPane = new TitledPane("Line Workbench", workbenchContent);
        workbenchPane.setCollapsible(false);

        content = new VBox(8, toolbar, ancestorLineTable, workbenchPane, stewardshipPane);
        content.setPadding(new Insets(10));
    }

    VBox getContent() {
        return content;
    }

    AncestorLineSummary getSelectedAncestorLine() {
        return ancestorLineTable.getSelectionModel().getSelectedItem();
    }

    LineWorkbenchRow getSelectedWorkbenchRow() {
        return lineWorkbenchTable.getSelectionModel().getSelectedItem();
    }

    LineStewardshipStatus getStewardshipStatus() {
        return ancestorStewardshipStatusCombo.getValue();
    }

    String getStewardshipNotes() {
        return ancestorStewardshipNotesArea.getText();
    }

    void setAncestorLines(List<AncestorLineSummary> summaries) {
        ancestorLineTable.setItems(FXCollections.observableArrayList(summaries));
    }

    void clearAncestorLines() {
        ancestorLineTable.setItems(FXCollections.observableArrayList());
        clearWorkbench();
        loadStewardship(null);
    }

    boolean reselectAncestorLine(Long ancestorPersonId) {
        if (ancestorPersonId == null) {
            return false;
        }

        for (AncestorLineSummary row : ancestorLineTable.getItems()) {
            if (ancestorPersonId.equals(row.getAncestorPersonId())) {
                ancestorLineTable.getSelectionModel().select(row);
                ancestorLineTable.scrollTo(row);
                return true;
            }
        }

        return false;
    }

    void selectFirstAncestorLine() {
        if (!ancestorLineTable.getItems().isEmpty()) {
            ancestorLineTable.getSelectionModel().selectFirst();
        }
    }

    void showWorkbench(AncestorLineSummary summary, List<LineWorkbenchRow> rows) {
        if (summary == null || summary.getAncestorPersonId() == null) {
            clearWorkbench();
            return;
        }

        lineWorkbenchAncestorValue.setText("Workbench for " + nullSafe(summary.getAncestorDisplayName()));
        allLineWorkbenchRows.setAll(rows);
        applyLineWorkbenchFilter();
        if (lineWorkbenchTable.getSelectionModel().getSelectedItem() == null && !lineWorkbenchTable.getItems().isEmpty()) {
            lineWorkbenchTable.getSelectionModel().selectFirst();
        }
    }

    void clearWorkbench() {
        lineWorkbenchAncestorValue.setText("Select an ancestor line to open the workbench.");
        allLineWorkbenchRows.clear();
        applyLineWorkbenchFilter();
    }

    void reselectWorkbenchRow(LineWorkbenchRow previousSelection) {
        if (previousSelection == null) {
            return;
        }

        for (LineWorkbenchRow row : lineWorkbenchTable.getItems()) {
            if (!row.ordinanceName().equals(previousSelection.ordinanceName())
                    || !row.personId().equals(previousSelection.personId())) {
                continue;
            }

            if (row.spouseLink() == null && previousSelection.spouseLink() == null) {
                lineWorkbenchTable.getSelectionModel().select(row);
                return;
            }

            if (row.spouseLink() != null
                    && previousSelection.spouseLink() != null
                    && row.spouseLink().getSpouseLinkId() != null
                    && row.spouseLink().getSpouseLinkId().equals(previousSelection.spouseLink().getSpouseLinkId())) {
                lineWorkbenchTable.getSelectionModel().select(row);
                return;
            }
        }
    }

    void loadStewardship(AncestorLineSummary summary) {
        if (summary == null || summary.getAncestorPersonId() == null) {
            ancestorStewardshipAncestorValue.setText("Select an ancestor line to manage stewardship.");
            ancestorStewardshipStatusCombo.setValue(LineStewardshipStatus.UNASSIGNED);
            ancestorStewardshipNotesArea.setText("");
            ancestorStewardshipStatusCombo.setDisable(true);
            ancestorStewardshipNotesArea.setDisable(true);
            return;
        }

        ancestorStewardshipAncestorValue.setText(nullSafe(summary.getAncestorDisplayName()));
        ancestorStewardshipStatusCombo.setDisable(false);
        ancestorStewardshipNotesArea.setDisable(false);
        ancestorStewardshipStatusCombo.setValue(
                summary.getStewardshipStatus() == null ? LineStewardshipStatus.UNASSIGNED : summary.getStewardshipStatus()
        );
        ancestorStewardshipNotesArea.setText(nullSafe(summary.getStewardshipNotes()));
    }

    void refreshAncestorLineTable() {
        ancestorLineTable.refresh();
    }

    private void configureAncestorLineTable(Runnable onOpenAncestor, Consumer<AncestorLineSummary> onSelectedAncestorChanged) {
        TableColumn<AncestorLineSummary, String> ancestorColumn = new TableColumn<>("Ancestor");
        ancestorColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getAncestorDisplayName()))
        );
        ancestorColumn.setPrefWidth(220);

        TableColumn<AncestorLineSummary, String> stewardshipColumn = new TableColumn<>("Stewardship");
        stewardshipColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getStewardshipStatus() == null ? "" : data.getValue().getStewardshipStatus().name())
        );
        stewardshipColumn.setPrefWidth(150);

        TableColumn<AncestorLineSummary, String> badgeColumn = new TableColumn<>("Badge");
        badgeColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getBadgeStatus() == null ? "" : data.getValue().getBadgeStatus().name())
        );
        badgeColumn.setPrefWidth(130);

        TableColumn<AncestorLineSummary, String> openColumn = new TableColumn<>("Open");
        openColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getOpenCount())));
        openColumn.setPrefWidth(70);

        TableColumn<AncestorLineSummary, String> soonColumn = new TableColumn<>("Soon");
        soonColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getOpeningSoonCount())));
        soonColumn.setPrefWidth(70);

        TableColumn<AncestorLineSummary, String> waitingColumn = new TableColumn<>("Waiting 110");
        waitingColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getWaiting110Count())));
        waitingColumn.setPrefWidth(100);

        TableColumn<AncestorLineSummary, String> unresolvedColumn = new TableColumn<>("Unresolved");
        unresolvedColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().getUnresolvedCount())));
        unresolvedColumn.setPrefWidth(100);

        TableColumn<AncestorLineSummary, String> nextColumn = new TableColumn<>("Next Available");
        nextColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(formatSummaryDate(data.getValue().getNextAvailableDate()))
        );
        nextColumn.setPrefWidth(130);

        TableColumn<AncestorLineSummary, String> reasonColumn = new TableColumn<>("Reason");
        reasonColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getSummaryReason()))
        );
        reasonColumn.setPrefWidth(430);

        ancestorLineTable.getColumns().addAll(
                ancestorColumn,
                stewardshipColumn,
                badgeColumn,
                openColumn,
                soonColumn,
                waitingColumn,
                unresolvedColumn,
                nextColumn,
                reasonColumn
        );

        ancestorLineTable.setRowFactory(table -> {
            TableRow<AncestorLineSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onOpenAncestor.run();
                }
            });
            return row;
        });

        ancestorLineTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                onSelectedAncestorChanged.accept(newSelection)
        );
    }

    private void configureLineWorkbenchControls() {
        lineWorkbenchFilterCombo.getItems().setAll(
                "All Items",
                "Open Now",
                "Opening Soon",
                "Unresolved",
                "Waiting 110",
                "Complete / N.A."
        );
        lineWorkbenchFilterCombo.setValue("All Items");
        lineWorkbenchFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyLineWorkbenchFilter());
    }

    private void configureLineWorkbenchTable(Runnable onOpenWorkbenchPerson, Consumer<OrdinanceStatus> onUpdateWorkbenchStatus) {
        TableColumn<LineWorkbenchRow, String> lineageColumn = new TableColumn<>("Line");
        lineageColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().lineageLabel()))
        );
        lineageColumn.setPrefWidth(80);

        TableColumn<LineWorkbenchRow, String> generationColumn = new TableColumn<>("Gen");
        generationColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().generation()))
        );
        generationColumn.setPrefWidth(60);

        TableColumn<LineWorkbenchRow, String> personColumn = new TableColumn<>("Person");
        personColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().personName()))
        );
        personColumn.setPrefWidth(200);

        TableColumn<LineWorkbenchRow, String> ordinanceColumn = new TableColumn<>("Item");
        ordinanceColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().ordinanceName()))
        );
        ordinanceColumn.setPrefWidth(160);

        TableColumn<LineWorkbenchRow, String> relatedColumn = new TableColumn<>("Related");
        relatedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().relatedPersonName()))
        );
        relatedColumn.setPrefWidth(180);

        TableColumn<LineWorkbenchRow, String> recordedColumn = new TableColumn<>("Recorded");
        recordedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().recordedStatus() == null ? "" : data.getValue().recordedStatus().name())
        );
        recordedColumn.setPrefWidth(140);

        TableColumn<LineWorkbenchRow, String> suggestedColumn = new TableColumn<>("Suggested");
        suggestedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().suggestedStatus() == null ? "" : data.getValue().suggestedStatus().name())
        );
        suggestedColumn.setPrefWidth(140);

        TableColumn<LineWorkbenchRow, String> reasonColumn = new TableColumn<>("Reason");
        reasonColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().reason()))
        );
        reasonColumn.setPrefWidth(420);

        lineWorkbenchTable.getColumns().addAll(
                lineageColumn,
                generationColumn,
                personColumn,
                ordinanceColumn,
                relatedColumn,
                recordedColumn,
                suggestedColumn,
                reasonColumn
        );

        lineWorkbenchTable.setRowFactory(table -> {
            TableRow<LineWorkbenchRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onOpenWorkbenchPerson.run();
                }
            });
            return row;
        });

        lineWorkbenchTable.setFocusTraversable(true);
        lineWorkbenchTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onOpenWorkbenchPerson.run();
                event.consume();
                return;
            }

            OrdinanceStatus status = mapStatusShortcut(event.getCode());
            if (status != null) {
                onUpdateWorkbenchStatus.accept(status);
                event.consume();
            }
        });
    }

    private void configureStewardshipControls() {
        ancestorStewardshipStatusCombo.getItems().setAll(LineStewardshipStatus.values());
        ancestorStewardshipStatusCombo.setValue(LineStewardshipStatus.UNASSIGNED);
        ancestorStewardshipNotesArea.setWrapText(true);
        ancestorStewardshipNotesArea.setPrefRowCount(4);
        loadStewardship(null);
    }

    private void applyLineWorkbenchFilter() {
        String activeFilter = lineWorkbenchFilterCombo.getValue();

        filteredLineWorkbenchRows.setPredicate(row -> {
            if (row == null) {
                return false;
            }

            if (activeFilter == null || "All Items".equals(activeFilter)) {
                return true;
            }

            return switch (activeFilter) {
                case "Open Now" -> row.suggestedStatus() == OrdinanceStatus.OPEN;
                case "Opening Soon" -> isOpeningSoon(row.suggestedStatus());
                case "Unresolved" -> row.suggestedStatus() == OrdinanceStatus.UNKNOWN;
                case "Waiting 110" -> row.suggestedStatus() == OrdinanceStatus.BLOCKED_110;
                case "Complete / N.A." -> row.suggestedStatus() == OrdinanceStatus.COMPLETE
                        || row.suggestedStatus() == OrdinanceStatus.NOT_APPLICABLE;
                default -> true;
            };
        });
    }

    private boolean isOpeningSoon(OrdinanceStatus status) {
        return status == OrdinanceStatus.SOON_1Y
                || status == OrdinanceStatus.SOON_2Y
                || status == OrdinanceStatus.SOON_5Y
                || status == OrdinanceStatus.SOON_10Y;
    }

    private OrdinanceStatus mapStatusShortcut(KeyCode keyCode) {
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

    private String formatSummaryDate(LocalDate value) {
        return value == null ? "" : DateTimeFormatter.ofPattern("MMM d, yyyy").format(value);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
