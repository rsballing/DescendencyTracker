package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.application.BackupService;
import family.balling.descendencytracker.application.AncestorLineSummaryService;
import family.balling.descendencytracker.application.LineStewardshipService;
import family.balling.descendencytracker.application.OrdinanceEligibilityService;
import family.balling.descendencytracker.application.OrdinanceService;
import family.balling.descendencytracker.application.PersonService;
import family.balling.descendencytracker.application.RelationshipService;
import family.balling.descendencytracker.application.WorkQueueService;
import family.balling.descendencytracker.domain.AncestorLineSummary;
import family.balling.descendencytracker.domain.LineStewardship;
import family.balling.descendencytracker.domain.OrdinanceEligibilityRow;
import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.WorkQueueRow;
import family.balling.descendencytracker.domain.enums.LineStewardshipStatus;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MainView extends BorderPane {
    private static final DateTimeFormatter BACKUP_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter SUMMARY_DATE_FORMAT =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final PersonService personService;
    private final RelationshipService relationshipService;
    private final OrdinanceService ordinanceService;
    private final AncestorLineSummaryService ancestorLineSummaryService;
    private final LineStewardshipService lineStewardshipService;
    private final OrdinanceEligibilityService ordinanceEligibilityService;
    private final BackupService backupService;
    private final WorkQueueService workQueueService;

    private final ObservableList<Person> allPeople = FXCollections.observableArrayList();

    private final ObservableList<WorkQueueRow> allWorkQueueRows = FXCollections.observableArrayList();
    private final FilteredList<WorkQueueRow> filteredWorkQueueRows = new FilteredList<>(allWorkQueueRows, row -> true);
    private final SortedList<WorkQueueRow> sortedWorkQueueRows = new SortedList<>(filteredWorkQueueRows);
    private final ObservableList<AncestorLineSummary> allReportRows = FXCollections.observableArrayList();
    private final FilteredList<AncestorLineSummary> filteredReportRows = new FilteredList<>(allReportRows, row -> true);
    private final SortedList<AncestorLineSummary> sortedReportRows = new SortedList<>(filteredReportRows);
    private final TableView<Person> personTable;
    private final TableView<ParentChildLink> parentsTable = new TableView<>();
    private final TableView<ParentChildLink> childrenTable = new TableView<>();
    private final TableView<SpouseLink> spousesTable = new TableView<>();
    private final TableView<OrdinanceEligibilityRow> eligibilityTable = new TableView<>();
    private final TableView<WorkQueueRow> workQueueTable = new TableView<>();
    private final TableView<AncestorLineSummary> reportsTable = new TableView<>();

    private final TreeView<TreePersonNode> pedigreeTree = new TreeView<>();
    private final TreeView<TreePersonNode> descendancyTree = new TreeView<>();

    private final TextArea detailArea = new TextArea();
    private final Label statusLabel = new Label();

    private final TextField workQueueSearchField = new TextField();
    private final CheckBox workQueueActionableOnlyCheckBox = new CheckBox("Open/Soon only");
    private final ComboBox<String> workQueueBucketFilterCombo = new ComboBox<>();
    private final TextField reportSearchField = new TextField();
    private final ComboBox<String> reportTypeFilterCombo = new ComboBox<>();
    private final Label summaryPreferredNameValue = new Label();
    private final Label summaryFsPidValue = new Label();
    private final Label summaryRootValue = new Label();
    private final Label summaryParentsValue = new Label();
    private final Label summaryChildrenValue = new Label();
    private final Label summarySpousesValue = new Label();
    private final Label summaryAncestorsValue = new Label();
    private final Label summaryDescendantsValue = new Label();
    private final Label summaryLineBadgeValue = new Label();
    private final Label summaryLineNextAvailableValue = new Label();
    private final Label summaryLineOpenValue = new Label();
    private final Label summaryLineSoonValue = new Label();
    private final Label summaryLineWaitingValue = new Label();
    private final Label summaryLineUnresolvedValue = new Label();
    private final Label summaryLineCompleteValue = new Label();
    private final Label summaryLineStewardshipValue = new Label();
    private final Label summaryLineReasonValue = new Label();

    private final Map<Long, AncestorLineSummary> ancestorSummaryCache = new HashMap<>();
    private final PeopleNavigatorPane peopleNavigatorPane;
    private final OrdinancePane ordinancePane;
    private final AncestorLinesPane ancestorLinesPane;

    public MainView(PersonService personService,
                    RelationshipService relationshipService,
                    OrdinanceService ordinanceService,
                    AncestorLineSummaryService ancestorLineSummaryService,
                    LineStewardshipService lineStewardshipService,
                    OrdinanceEligibilityService ordinanceEligibilityService,
                    BackupService backupService,
                    WorkQueueService workQueueService) {
        this.personService = personService;
        this.relationshipService = relationshipService;
        this.ordinanceService = ordinanceService;
        this.ancestorLineSummaryService = ancestorLineSummaryService;
        this.lineStewardshipService = lineStewardshipService;
        this.ordinanceEligibilityService = ordinanceEligibilityService;
        this.backupService = backupService;
        this.workQueueService = workQueueService;
        this.peopleNavigatorPane = new PeopleNavigatorPane(
                this::editSelectedPerson,
                this::handleSelectedPersonChanged,
                this::hasIncompleteTrackedOrdinances
        );
        this.personTable = peopleNavigatorPane.getPersonTable();
        this.ancestorLinesPane = new AncestorLinesPane(
                this::openSelectedAncestorLine,
                () -> refreshAncestorLineTable(getSelectedPerson()),
                this::saveSelectedAncestorStewardship,
                this::reloadSelectedAncestorStewardship,
                this::openSelectedLineWorkbenchPerson,
                this::updateSelectedLineWorkbenchStatus,
                this::refreshSelectedAncestorLineWorkbench,
                this::handleSelectedAncestorLineChanged
        );
        this.ordinancePane = new OrdinancePane(
                this::saveOrdinancesForSelectedPerson,
                () -> updateOrdinanceEditor(getSelectedPerson()),
                this::updateSelectedOrdinanceStatus
        );
        buildUi();
        refreshPeople();
    }

    private void buildUi() {
        setPadding(new Insets(10));

        Button addButton = new Button("Add Person");
        Button editButton = new Button("Edit Person");
        Button deleteButton = new Button("Delete Person");
        Button setRootButton = new Button("Set Root");
        Button refreshButton = new Button("Refresh");
        Button exportBackupButton = new Button("Export Backup");
        Button importBackupButton = new Button("Import Backup");

        addButton.setOnAction(event -> addPerson());
        editButton.setOnAction(event -> editSelectedPerson());
        deleteButton.setOnAction(event -> deleteSelectedPerson());
        setRootButton.setOnAction(event -> setSelectedAsRoot());
        refreshButton.setOnAction(event -> refreshPeople());
        exportBackupButton.setOnAction(event -> exportBackup());
        importBackupButton.setOnAction(event -> importBackup());

        ToolBar personToolBar = new ToolBar(
                addButton,
                editButton,
                deleteButton,
                setRootButton,
                refreshButton,
                exportBackupButton,
                importBackupButton
        );

        ToolBar filterToolBar = peopleNavigatorPane.getFilterToolBar();

        setTop(new VBox(6, personToolBar, filterToolBar));

        configureParentsTable();
        configureChildrenTable();
        configureSpousesTable();
        configureEligibilityTable();
        configureWorkQueueControls();
        configureWorkQueueTable();
        configureReportControls();
        configureReportsTable();
        configurePedigreeTree();
        configureDescendancyTree();

        sortedWorkQueueRows.comparatorProperty().bind(workQueueTable.comparatorProperty());
        workQueueTable.setItems(sortedWorkQueueRows);
        sortedReportRows.comparatorProperty().bind(reportsTable.comparatorProperty());
        reportsTable.setItems(sortedReportRows);

        detailArea.setEditable(false);
        detailArea.setWrapText(true);

        TitledPane summaryPane = buildSummaryPane();

        VBox detailPane = new VBox(
                8,
                summaryPane,
                new Label("Selected Person Details"),
                detailArea
        );
        detailPane.setPadding(new Insets(10));

        TabPane relationshipTabs = new TabPane();
        relationshipTabs.getTabs().add(new Tab("Details", detailPane));
        relationshipTabs.getTabs().add(new Tab("Work Queue", buildWorkQueueTabContent()));
        relationshipTabs.getTabs().add(new Tab("Eligibility", buildEligibilityTabContent()));
        relationshipTabs.getTabs().add(new Tab("Ordinances", buildOrdinancesTabContent()));
        relationshipTabs.getTabs().add(new Tab("Ancestor Lines", buildAncestorLinesTabContent()));
        relationshipTabs.getTabs().add(new Tab("Reports", buildReportsTabContent()));
        relationshipTabs.getTabs().add(new Tab("Pedigree", buildPedigreeTabContent()));
        relationshipTabs.getTabs().add(new Tab("Descendancy", buildDescendancyTabContent()));
        relationshipTabs.getTabs().add(new Tab("Parents", buildParentsTabContent()));
        relationshipTabs.getTabs().add(new Tab("Children", buildChildrenTabContent()));
        relationshipTabs.getTabs().add(new Tab("Spouses", buildSpousesTabContent()));
        relationshipTabs.getTabs().forEach(tab -> tab.setClosable(false));

        SplitPane splitPane = new SplitPane(peopleNavigatorPane.getContent(), relationshipTabs);
        splitPane.setDividerPositions(0.60);

        setCenter(splitPane);
        setBottom(statusLabel);
    }

    private VBox buildWorkQueueTabContent() {
        Button openSelectedButton = new Button("Open Selected Person");
        Button refreshQueueButton = new Button("Refresh Queue");
        Button clearQueueFiltersButton = new Button("Clear Queue Filters");

        openSelectedButton.setOnAction(event -> openSelectedWorkQueuePerson());
        refreshQueueButton.setOnAction(event -> refreshWorkQueue());
        clearQueueFiltersButton.setOnAction(event -> clearWorkQueueFilters());

        ToolBar toolbar = new ToolBar(
                openSelectedButton,
                refreshQueueButton,
                new Label("Find"),
                workQueueSearchField,
                clearQueueFiltersButton,
                workQueueActionableOnlyCheckBox,
                new Label("Bucket"),
                workQueueBucketFilterCombo
        );

        VBox box = new VBox(8, toolbar, workQueueTable);
        box.setPadding(new Insets(10));
        return box;
    }

    private void exportBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Backup");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQLite Backup Files", "*.db", "*.sqlite", "*.sqlite3")
        );
        chooser.setInitialFileName("descendencytracker-backup-" + BACKUP_TIME_FORMAT.format(LocalDateTime.now()) + ".db");

        File selectedFile = chooser.showSaveDialog(getCurrentWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            Path exported = backupService.exportBackup(selectedFile.toPath());
            showInfo("Backup Exported", "Backup saved to:\n" + exported);
        } catch (Exception ex) {
            showError("Could not export backup.", ex);
        }
    }

    private void importBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Backup");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQLite Backup Files", "*.db", "*.sqlite", "*.sqlite3")
        );

        File selectedFile = chooser.showOpenDialog(getCurrentWindow());
        if (selectedFile == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Import Backup");
        confirm.setHeaderText("Import the selected backup?");
        confirm.setContentText(
                "This will replace the current database contents.\n\n" +
                        "A safety backup of the current database will be created automatically first."
        );

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        try {
            Path safetyBackup = backupService.importBackup(selectedFile.toPath());
            clearPersonFilters();
            clearWorkQueueFilters();
            refreshPeople();

            showInfo(
                    "Backup Imported",
                    "Imported from:\n" + selectedFile.toPath().toAbsolutePath() +
                            "\n\nA safety backup of the previous database was created at:\n" + safetyBackup
            );
        } catch (Exception ex) {
            showError("Could not import backup.", ex);
        }
    }

    private Window getCurrentWindow() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Person getSelectedPerson() {
        return peopleNavigatorPane.getSelectedPerson();
    }

    private void clearPersonFilters() {
        peopleNavigatorPane.clearFilters();
    }

    private void handleSelectedPersonChanged(Person newSelection) {
        updateSummaryCard(newSelection);
        updateDetailArea(newSelection);
        refreshRelationshipTables(newSelection);
        refreshAncestorLineTable(newSelection);
        refreshReportsTable(newSelection);
        refreshPedigreeTree(newSelection);
        refreshDescendancyTree(newSelection);
        updateOrdinanceEditor(newSelection);
        refreshEligibilityTable(newSelection);
        updateStatus();
    }

    private void handleSelectedAncestorLineChanged(AncestorLineSummary newSelection) {
        ancestorLinesPane.loadStewardship(newSelection);
        refreshLineWorkbench(newSelection);
    }

    private void configureWorkQueueControls() {
        workQueueSearchField.setPromptText("Search queue...");
        workQueueSearchField.setPrefWidth(260);
        workQueueSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyWorkQueueFilter());

        workQueueActionableOnlyCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> applyWorkQueueFilter());

        workQueueBucketFilterCombo.getItems().setAll(
                "All",
                "OPEN",
                "SOON_1Y",
                "SOON_2Y",
                "SOON_5Y",
                "SOON_10Y",
                "UNKNOWN",
                "BLOCKED_110"
        );
        workQueueBucketFilterCombo.setValue("All");
        workQueueBucketFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyWorkQueueFilter());
    }

    private void configureReportControls() {
        reportSearchField.setPromptText("Search reports...");
        reportSearchField.setPrefWidth(260);
        reportSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyReportFilter());

        reportTypeFilterCombo.getItems().setAll(
                "All Reports",
                "Open Now Report",
                "Opening Soon Report",
                "Waiting on 110 Report",
                "Unresolved Data Report",
                "Complete For Now Report",
                "Not Reviewed Report"
        );
        reportTypeFilterCombo.setValue("All Reports");
        reportTypeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyReportFilter());
    }

    private void clearWorkQueueFilters() {
        workQueueSearchField.clear();
        workQueueActionableOnlyCheckBox.setSelected(false);
        workQueueBucketFilterCombo.setValue("All");
    }

    private void clearReportFilters() {
        reportSearchField.clear();
        reportTypeFilterCombo.setValue("All Reports");
    }

    private void applyWorkQueueFilter() {
        String searchText = workQueueSearchField.getText() == null ? "" : workQueueSearchField.getText().trim().toLowerCase();
        boolean actionableOnly = workQueueActionableOnlyCheckBox.isSelected();
        String bucketFilter = workQueueBucketFilterCombo.getValue();

        filteredWorkQueueRows.setPredicate(row -> {
            if (row == null) {
                return false;
            }

            if (actionableOnly && !workQueueService.isActionable(row.getQueueBucket())) {
                return false;
            }

            if (bucketFilter != null && !"All".equals(bucketFilter)) {
                if (row.getQueueBucket() == null || !bucketFilter.equals(row.getQueueBucket().name())) {
                    return false;
                }
            }

            if (searchText.isBlank()) {
                return true;
            }

            return containsIgnoreCase(row.getDisplayName(), searchText)
                    || containsIgnoreCase(row.getFsPid(), searchText)
                    || containsIgnoreCase(row.getTriggerLabel(), searchText)
                    || containsIgnoreCase(row.getReason(), searchText);
        });
    }

    private void applyReportFilter() {
        String searchText = reportSearchField.getText() == null ? "" : reportSearchField.getText().trim().toLowerCase();
        String reportType = reportTypeFilterCombo.getValue();

        filteredReportRows.setPredicate(row -> {
            if (row == null) {
                return false;
            }

            if (reportType != null && !"All Reports".equals(reportType) && !matchesReportType(row, reportType)) {
                return false;
            }

            if (searchText.isBlank()) {
                return true;
            }

            return containsIgnoreCase(row.getAncestorDisplayName(), searchText)
                    || containsIgnoreCase(row.getSummaryReason(), searchText)
                    || containsIgnoreCase(row.getStewardshipNotes(), searchText)
                    || (row.getStewardshipStatus() != null && row.getStewardshipStatus().name().toLowerCase().contains(searchText))
                    || (row.getBadgeStatus() != null && row.getBadgeStatus().name().toLowerCase().contains(searchText));
        });
    }

    private boolean containsIgnoreCase(String value, String searchTextLower) {
        return value != null && value.toLowerCase().contains(searchTextLower);
    }

    private boolean matchesReportType(AncestorLineSummary row, String reportType) {
        return switch (reportType) {
            case "Open Now Report" -> row.getOpenCount() > 0;
            case "Opening Soon Report" -> row.getOpeningSoonCount() > 0;
            case "Waiting on 110 Report" -> row.getWaiting110Count() > 0;
            case "Unresolved Data Report" -> row.getUnresolvedCount() > 0;
            case "Complete For Now Report" -> row.getBadgeStatus() != null && row.getBadgeStatus().name().equals("COMPLETE_FOR_NOW");
            case "Not Reviewed Report" -> row.getBadgeStatus() != null && row.getBadgeStatus().name().equals("NOT_REVIEWED");
            default -> true;
        };
    }

    private TitledPane buildSummaryPane() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));

        int row = 0;

        grid.add(new Label("Preferred Name"), 0, row);
        grid.add(summaryPreferredNameValue, 1, row);
        grid.add(new Label("FamilySearch PID"), 2, row);
        grid.add(summaryFsPidValue, 3, row++);
        grid.add(new Label("Root Person"), 0, row);
        grid.add(summaryRootValue, 1, row);
        grid.add(new Label("Parents"), 2, row);
        grid.add(summaryParentsValue, 3, row++);
        grid.add(new Label("Children"), 0, row);
        grid.add(summaryChildrenValue, 1, row);
        grid.add(new Label("Spouses"), 2, row);
        grid.add(summarySpousesValue, 3, row++);
        grid.add(new Label("Ancestors"), 0, row);
        grid.add(summaryAncestorsValue, 1, row);
        grid.add(new Label("Descendants"), 2, row);
        grid.add(summaryDescendantsValue, 3, row++);

        grid.add(new Label("Line Badge"), 0, row);
        grid.add(summaryLineBadgeValue, 1, row);
        grid.add(new Label("Next Available"), 2, row);
        grid.add(summaryLineNextAvailableValue, 3, row++);

        grid.add(new Label("Open Now"), 0, row);
        grid.add(summaryLineOpenValue, 1, row);
        grid.add(new Label("Opening Soon"), 2, row);
        grid.add(summaryLineSoonValue, 3, row++);

        grid.add(new Label("Waiting 110"), 0, row);
        grid.add(summaryLineWaitingValue, 1, row);
        grid.add(new Label("Unresolved"), 2, row);
        grid.add(summaryLineUnresolvedValue, 3, row++);

        grid.add(new Label("Complete"), 0, row);
        grid.add(summaryLineCompleteValue, 1, row);
        grid.add(new Label("Stewardship"), 2, row);
        grid.add(summaryLineStewardshipValue, 3, row++);

        grid.add(new Label("Line Reason"), 0, row);
        grid.add(summaryLineReasonValue, 1, row, 3, 1);

        TitledPane titledPane = new TitledPane("Quick Summary", grid);
        titledPane.setCollapsible(true);
        titledPane.setExpanded(false);
        return titledPane;
    }

    private VBox buildEligibilityTabContent() {
        Button refreshButton = new Button("Refresh Eligibility");
        Button copySuggestedButton = new Button("Copy Suggested Person Buckets");

        refreshButton.setOnAction(event -> refreshEligibilityTable(personTable.getSelectionModel().getSelectedItem()));
        copySuggestedButton.setOnAction(event -> copySuggestedPersonBucketsToOrdinanceEditor());

        ToolBar toolbar = new ToolBar(refreshButton, copySuggestedButton);

        VBox box = new VBox(8, toolbar, eligibilityTable);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildOrdinancesTabContent() {
        return ordinancePane.getContent();
    }

    private VBox buildAncestorLinesTabContent() {
        return ancestorLinesPane.getContent();
    }

    private VBox buildReportsTabContent() {
        Button openSelectedButton = new Button("Open Selected Ancestor");
        Button refreshButton = new Button("Refresh Reports");
        Button clearFiltersButton = new Button("Clear Report Filters");

        openSelectedButton.setOnAction(event -> openSelectedReportAncestor());
        refreshButton.setOnAction(event -> refreshReportsTable(personTable.getSelectionModel().getSelectedItem()));
        clearFiltersButton.setOnAction(event -> clearReportFilters());

        ToolBar toolbar = new ToolBar(
                openSelectedButton,
                refreshButton,
                new Label("Report"),
                reportTypeFilterCombo,
                new Label("Find"),
                reportSearchField,
                clearFiltersButton
        );

        VBox box = new VBox(8, toolbar, reportsTable);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildPedigreeTabContent() {
        Button openAncestorButton = new Button("Open Selected Ancestor");
        Button refreshPedigreeButton = new Button("Refresh Pedigree");

        openAncestorButton.setOnAction(event -> openSelectedPersonFromTree(pedigreeTree));
        refreshPedigreeButton.setOnAction(event ->
                refreshPedigreeTree(personTable.getSelectionModel().getSelectedItem())
        );

        ToolBar toolbar = new ToolBar(openAncestorButton, refreshPedigreeButton);

        VBox box = new VBox(8, toolbar, pedigreeTree);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildDescendancyTabContent() {
        Button openDescendantButton = new Button("Open Selected Descendant");
        Button refreshDescendancyButton = new Button("Refresh Descendancy");

        openDescendantButton.setOnAction(event -> openSelectedPersonFromTree(descendancyTree));
        refreshDescendancyButton.setOnAction(event ->
                refreshDescendancyTree(personTable.getSelectionModel().getSelectedItem())
        );

        ToolBar toolbar = new ToolBar(openDescendantButton, refreshDescendancyButton);

        VBox box = new VBox(8, toolbar, descendancyTree);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildParentsTabContent() {
        Button addParentButton = new Button("Add Parent");
        Button editParentButton = new Button("Edit Parent Link");
        Button removeParentButton = new Button("Remove Parent Link");

        addParentButton.setOnAction(event -> addParentToSelectedPerson());
        editParentButton.setOnAction(event -> editSelectedParentLink());
        removeParentButton.setOnAction(event -> removeSelectedParentLink());

        ToolBar toolbar = new ToolBar(addParentButton, editParentButton, removeParentButton);

        VBox box = new VBox(8, toolbar, parentsTable);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildChildrenTabContent() {
        Button addChildButton = new Button("Add Child");
        Button editChildButton = new Button("Edit Child Link");
        Button removeChildButton = new Button("Remove Child Link");

        addChildButton.setOnAction(event -> addChildToSelectedPerson());
        editChildButton.setOnAction(event -> editSelectedChildLink());
        removeChildButton.setOnAction(event -> removeSelectedChildLink());

        ToolBar toolbar = new ToolBar(addChildButton, editChildButton, removeChildButton);

        VBox box = new VBox(8, toolbar, childrenTable);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox buildSpousesTabContent() {
        Button addSpouseButton = new Button("Add Spouse");
        Button editSpouseButton = new Button("Edit Spouse Link");
        Button removeSpouseButton = new Button("Remove Spouse Link");

        addSpouseButton.setOnAction(event -> addSpouseToSelectedPerson());
        editSpouseButton.setOnAction(event -> editSelectedSpouseLink());
        removeSpouseButton.setOnAction(event -> removeSelectedSpouseLink());

        ToolBar toolbar = new ToolBar(addSpouseButton, editSpouseButton, removeSpouseButton);

        VBox box = new VBox(8, toolbar, spousesTable);
        box.setPadding(new Insets(10));
        return box;
    }

    private void configureEligibilityTable() {
        TableColumn<OrdinanceEligibilityRow, String> ordinanceColumn = new TableColumn<>("Ordinance");
        ordinanceColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getOrdinanceName()))
        );
        ordinanceColumn.setPrefWidth(180);

        TableColumn<OrdinanceEligibilityRow, String> relatedColumn = new TableColumn<>("Related Person");
        relatedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getRelatedPersonName()))
        );
        relatedColumn.setPrefWidth(180);

        TableColumn<OrdinanceEligibilityRow, String> recordedColumn = new TableColumn<>("Recorded");
        recordedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getRecordedStatus() == null
                        ? ""
                        : data.getValue().getRecordedStatus().name())
        );
        recordedColumn.setPrefWidth(140);

        TableColumn<OrdinanceEligibilityRow, String> suggestedColumn = new TableColumn<>("Suggested");
        suggestedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getSuggestedStatus() == null
                        ? ""
                        : data.getValue().getSuggestedStatus().name())
        );
        suggestedColumn.setPrefWidth(140);

        TableColumn<OrdinanceEligibilityRow, String> reasonColumn = new TableColumn<>("Reason");
        reasonColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getReason()))
        );
        reasonColumn.setPrefWidth(520);

        eligibilityTable.getColumns().addAll(
                ordinanceColumn,
                relatedColumn,
                recordedColumn,
                suggestedColumn,
                reasonColumn
        );
    }

    private void configureWorkQueueTable() {
        TableColumn<WorkQueueRow, String> bucketColumn = new TableColumn<>("Bucket");
        bucketColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getQueueBucket() == null
                        ? ""
                        : data.getValue().getQueueBucket().name())
        );
        bucketColumn.setPrefWidth(120);

        TableColumn<WorkQueueRow, String> nameColumn = new TableColumn<>("Person");
        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getDisplayName()))
        );
        nameColumn.setPrefWidth(220);

        TableColumn<WorkQueueRow, String> fsPidColumn = new TableColumn<>("FS PID");
        fsPidColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getFsPid()))
        );
        fsPidColumn.setPrefWidth(140);

        TableColumn<WorkQueueRow, String> triggerColumn = new TableColumn<>("Trigger");
        triggerColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getTriggerLabel()))
        );
        triggerColumn.setPrefWidth(220);

        TableColumn<WorkQueueRow, String> parentsColumn = new TableColumn<>("Parents");
        parentsColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getParentCount()))
        );
        parentsColumn.setPrefWidth(80);

        TableColumn<WorkQueueRow, String> childrenColumn = new TableColumn<>("Children");
        childrenColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getChildCount()))
        );
        childrenColumn.setPrefWidth(80);

        TableColumn<WorkQueueRow, String> spousesColumn = new TableColumn<>("Spouses");
        spousesColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(String.valueOf(data.getValue().getSpouseCount()))
        );
        spousesColumn.setPrefWidth(80);

        TableColumn<WorkQueueRow, String> reasonColumn = new TableColumn<>("Reason");
        reasonColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getReason()))
        );
        reasonColumn.setPrefWidth(460);

        workQueueTable.getColumns().addAll(
                bucketColumn,
                nameColumn,
                fsPidColumn,
                triggerColumn,
                parentsColumn,
                childrenColumn,
                spousesColumn,
                reasonColumn
        );

        workQueueTable.setRowFactory(table -> {
            TableRow<WorkQueueRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openSelectedWorkQueuePerson();
                }
            });
            return row;
        });
    }

    private void configureReportsTable() {
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

        reportsTable.getColumns().addAll(
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

        reportsTable.setRowFactory(table -> {
            TableRow<AncestorLineSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openSelectedReportAncestor();
                }
            });
            return row;
        });
    }

    private void openSelectedWorkQueuePerson() {
        WorkQueueRow selected = workQueueTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getPersonId() == null) {
            showWarning("Please select a work queue row.");
            return;
        }

        selectPersonInTable(selected.getPersonId());
    }

    private void openSelectedAncestorLine() {
        AncestorLineSummary selected = ancestorLinesPane.getSelectedAncestorLine();
        if (selected == null || selected.getAncestorPersonId() == null) {
            showWarning("Please select an ancestor line.");
            return;
        }

        selectPersonInTable(selected.getAncestorPersonId());
    }

    private void openSelectedLineWorkbenchPerson() {
        LineWorkbenchRow selected = ancestorLinesPane.getSelectedWorkbenchRow();
        if (selected == null || selected.personId() == null) {
            showWarning("Please select a work item.");
            return;
        }

        selectPersonInTable(selected.personId());
    }

    private void openSelectedReportAncestor() {
        AncestorLineSummary selected = reportsTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getAncestorPersonId() == null) {
            showWarning("Please select a report row.");
            return;
        }

        selectPersonInTable(selected.getAncestorPersonId());
    }

    private void configurePedigreeTree() {
        pedigreeTree.setShowRoot(true);
        pedigreeTree.setRoot(new TreeItem<>(new TreePersonNode(-1L, "Select a person to view pedigree.")));
        pedigreeTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openSelectedPersonFromTree(pedigreeTree);
            }
        });
    }

    private void configureDescendancyTree() {
        descendancyTree.setShowRoot(true);
        descendancyTree.setRoot(new TreeItem<>(new TreePersonNode(-1L, "Select a person to view descendancy.")));
        descendancyTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openSelectedPersonFromTree(descendancyTree);
            }
        });
    }

    private void configurePersonTable() {
        TableColumn<Person, String> rootColumn = new TableColumn<>("Root");
        rootColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isRoot() ? "★" : "")
        );
        rootColumn.setPrefWidth(60);

        TableColumn<Person, String> preferredNameColumn = new TableColumn<>("Preferred Name");
        preferredNameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getPreferredName()))
        );
        preferredNameColumn.setPrefWidth(180);

        TableColumn<Person, String> fsPidColumn = new TableColumn<>("FS PID");
        fsPidColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getFsPid()))
        );
        fsPidColumn.setPrefWidth(140);

        TableColumn<Person, String> givenNamesColumn = new TableColumn<>("Given Names");
        givenNamesColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getGivenNames()))
        );
        givenNamesColumn.setPrefWidth(180);

        TableColumn<Person, String> surnameColumn = new TableColumn<>("Surname");
        surnameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getSurname()))
        );
        surnameColumn.setPrefWidth(160);

        TableColumn<Person, String> sexColumn = new TableColumn<>("Sex");
        sexColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getSex().name())
        );
        sexColumn.setPrefWidth(100);

        TableColumn<Person, String> livingColumn = new TableColumn<>("Living");
        livingColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isLiving() ? "Yes" : "No")
        );
        livingColumn.setPrefWidth(100);

        TableColumn<Person, String> birthColumn = new TableColumn<>("Birth");
        birthColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getBirthDateText()))
        );
        birthColumn.setPrefWidth(140);

        TableColumn<Person, String> deathColumn = new TableColumn<>("Death");
        deathColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getDeathDateText()))
        );
        deathColumn.setPrefWidth(140);

        TableColumn<Person, String> reviewedColumn = new TableColumn<>("Reviewed");
        reviewedColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getReviewedStatus().name())
        );
        reviewedColumn.setPrefWidth(140);

        personTable.getColumns().addAll(
                rootColumn,
                preferredNameColumn,
                fsPidColumn,
                givenNamesColumn,
                surnameColumn,
                sexColumn,
                livingColumn,
                birthColumn,
                deathColumn,
                reviewedColumn
        );

        personTable.setRowFactory(table -> {
            TableRow<Person> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedPerson();
                }
            });
            return row;
        });
    }

    private void configureParentsTable() {
        TableColumn<ParentChildLink, String> parentColumn = new TableColumn<>("Parent");
        parentColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getParentDisplayName()))
        );
        parentColumn.setPrefWidth(220);

        TableColumn<ParentChildLink, String> orderColumn = new TableColumn<>("Child Order");
        orderColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getChildOrder() == null ? "" : String.valueOf(data.getValue().getChildOrder()))
        );
        orderColumn.setPrefWidth(100);

        TableColumn<ParentChildLink, String> notesColumn = new TableColumn<>("Notes");
        notesColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getNotes()))
        );
        notesColumn.setPrefWidth(300);

        parentsTable.getColumns().addAll(parentColumn, orderColumn, notesColumn);

        parentsTable.setRowFactory(table -> {
            TableRow<ParentChildLink> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedParentLink();
                }
            });
            return row;
        });
    }

    private void configureChildrenTable() {
        TableColumn<ParentChildLink, String> childColumn = new TableColumn<>("Child");
        childColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getChildDisplayName()))
        );
        childColumn.setPrefWidth(220);

        TableColumn<ParentChildLink, String> orderColumn = new TableColumn<>("Child Order");
        orderColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getChildOrder() == null ? "" : String.valueOf(data.getValue().getChildOrder()))
        );
        orderColumn.setPrefWidth(100);

        TableColumn<ParentChildLink, String> notesColumn = new TableColumn<>("Notes");
        notesColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getNotes()))
        );
        notesColumn.setPrefWidth(300);

        childrenTable.getColumns().addAll(childColumn, orderColumn, notesColumn);

        childrenTable.setRowFactory(table -> {
            TableRow<ParentChildLink> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedChildLink();
                }
            });
            return row;
        });
    }

    private void configureSpousesTable() {
        TableColumn<SpouseLink, String> spouseColumn = new TableColumn<>("Spouse");
        spouseColumn.setCellValueFactory(data -> {
            Person selected = personTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(nullSafe(data.getValue().getOtherPersonDisplayName(selected.getPersonId())));
        });
        spouseColumn.setPrefWidth(220);

        TableColumn<SpouseLink, String> marriageDateColumn = new TableColumn<>("Marriage Date");
        marriageDateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getMarriageDateText()))
        );
        marriageDateColumn.setPrefWidth(140);

        TableColumn<SpouseLink, String> sealingStatusColumn = new TableColumn<>("Sealing");
        sealingStatusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getSealingToSpouseStatus() == null
                        ? ""
                        : data.getValue().getSealingToSpouseStatus().name())
        );
        sealingStatusColumn.setPrefWidth(140);

        TableColumn<SpouseLink, String> sealingDateColumn = new TableColumn<>("Sealing Date");
        sealingDateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getSealingStatusDate()))
        );
        sealingDateColumn.setPrefWidth(140);

        TableColumn<SpouseLink, String> notesColumn = new TableColumn<>("Notes");
        notesColumn.setCellValueFactory(data -> {
            String marriageNotes = nullSafe(data.getValue().getMarriageNotes());
            String sealingNotes = nullSafe(data.getValue().getSealingNotes());

            if (!marriageNotes.isBlank() && !sealingNotes.isBlank()) {
                return new ReadOnlyStringWrapper("Marriage: " + marriageNotes + " | Sealing: " + sealingNotes);
            }
            if (!marriageNotes.isBlank()) {
                return new ReadOnlyStringWrapper("Marriage: " + marriageNotes);
            }
            if (!sealingNotes.isBlank()) {
                return new ReadOnlyStringWrapper("Sealing: " + sealingNotes);
            }
            return new ReadOnlyStringWrapper("");
        });
        notesColumn.setPrefWidth(320);

        spousesTable.getColumns().addAll(
                spouseColumn,
                marriageDateColumn,
                sealingStatusColumn,
                sealingDateColumn,
                notesColumn
        );

        spousesTable.setRowFactory(table -> {
            TableRow<SpouseLink> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedSpouseLink();
                }
            });
            return row;
        });
    }

    private void refreshPeople() {
        try {
            ancestorSummaryCache.clear();
            Long selectedPersonId = null;
            Person currentlySelected = personTable.getSelectionModel().getSelectedItem();
            if (currentlySelected != null) {
                selectedPersonId = currentlySelected.getPersonId();
            }

            allPeople.setAll(personService.getAllPeople());
            peopleNavigatorPane.setPeople(allPeople);
            refreshWorkQueue();

            if (selectedPersonId != null) {
                reselectPerson(selectedPersonId);
            }

            if (personTable.getSelectionModel().getSelectedItem() == null && !personTable.getItems().isEmpty()) {
                peopleNavigatorPane.selectFirstVisiblePerson();
            }

            updateStatus();

            Person selected = personTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                clearSelectionDependentViews();
            } else {
                updateSummaryCard(selected);
                updateDetailArea(selected);
                refreshRelationshipTables(selected);
                refreshAncestorLineTable(selected);
                refreshReportsTable(selected);
                refreshPedigreeTree(selected);
                refreshDescendancyTree(selected);
                updateOrdinanceEditor(selected);
                refreshEligibilityTable(selected);
            }
        } catch (Exception ex) {
            showError("Could not refresh people.", ex);
        }
    }

    private void refreshWorkQueue() {
        try {
            allWorkQueueRows.setAll(workQueueService.buildWorkQueue(allPeople));
            applyWorkQueueFilter();
        } catch (Exception ex) {
            showError("Could not refresh the work queue.", ex);
        }
    }

    private void refreshAncestorLineTable(Person person) {
        if (person == null) {
            ancestorLinesPane.clearAncestorLines();
            return;
        }

        try {
            Long selectedAncestorId = getSelectedAncestorLinePersonId();
            List<AncestorLineSummary> summaries = ancestorLineSummaryService.buildSummaries(collectAncestorsForPerson(person), allPeople);
            applyLineStewardship(summaries);
            ancestorLinesPane.setAncestorLines(summaries);
            if (!reselectAncestorLine(selectedAncestorId)) {
                ancestorLinesPane.selectFirstAncestorLine();
            }
            if (ancestorLinesPane.getSelectedAncestorLine() == null) {
                ancestorLinesPane.clearWorkbench();
                ancestorLinesPane.loadStewardship(null);
            }
        } catch (Exception ex) {
            showError("Could not refresh ancestor lines.", ex);
        }
    }

    private void refreshLineWorkbench(AncestorLineSummary summary) {
        if (summary == null || summary.getAncestorPersonId() == null) {
            ancestorLinesPane.clearWorkbench();
            return;
        }

        Person ancestor = findPersonById(summary.getAncestorPersonId());

        if (ancestor == null) {
            ancestorLinesPane.clearWorkbench();
            return;
        }

        List<LineWorkbenchRow> rows = FXCollections.observableArrayList();
        collectLineWorkbenchRows(ancestor, 0, "A", new HashSet<>(), rows);
        ancestorLinesPane.showWorkbench(summary, rows);
    }

    private void refreshSelectedAncestorLineWorkbench() {
        refreshLineWorkbench(ancestorLinesPane.getSelectedAncestorLine());
    }

    private void collectLineWorkbenchRows(
            Person person,
            int generation,
            String lineageLabel,
            Set<Long> path,
            List<LineWorkbenchRow> rows
    ) {
        if (person == null || person.getPersonId() == null || !path.add(person.getPersonId())) {
            return;
        }

        PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(person.getPersonId());
        List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
        List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());
        List<OrdinanceEligibilityRow> dashboard = ordinanceEligibilityService.buildDashboard(
                person,
                ordinanceStatus,
                parents,
                spouses,
                allPeople
        );

        int spouseIndex = 0;
        for (OrdinanceEligibilityRow row : dashboard) {
            SpouseLink spouseLink = row.isPersonLevel() || spouseIndex >= spouses.size() ? null : spouses.get(spouseIndex++);
            rows.add(new LineWorkbenchRow(
                    lineageLabel,
                    generation,
                    person.getPersonId(),
                    person.getDisplayName(),
                    person.getFsPid(),
                    row.getOrdinanceName(),
                    row.getRelatedPersonName(),
                    row.getRecordedStatus(),
                    row.getSuggestedStatus(),
                    row.getReason(),
                    spouseLink
            ));
        }

        List<ParentChildLink> children = relationshipService.getChildrenForPerson(person.getPersonId());
        for (int index = 0; index < children.size(); index++) {
            ParentChildLink childLink = children.get(index);
            Person child = findPersonById(childLink.getChildPersonId());
            if (child == null) {
                continue;
            }

            String childLineage = generation == 0
                    ? String.valueOf(index + 1)
                    : lineageLabel + "." + (index + 1);

            collectLineWorkbenchRows(
                    child,
                    generation + 1,
                    childLineage,
                    new HashSet<>(path),
                    rows
            );
        }
    }

    private void refreshReportsTable(Person person) {
        if (person == null) {
            allReportRows.clear();
            applyReportFilter();
            return;
        }

        try {
            List<AncestorLineSummary> summaries = ancestorLineSummaryService.buildSummaries(collectAncestorsForPerson(person), allPeople);
            applyLineStewardship(summaries);
            allReportRows.setAll(summaries);
            applyReportFilter();
        } catch (Exception ex) {
            showError("Could not refresh reports.", ex);
        }
    }

    private void refreshRelationshipTables(Person person) {
        if (person == null) {
            parentsTable.setItems(FXCollections.observableArrayList());
            childrenTable.setItems(FXCollections.observableArrayList());
            spousesTable.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            parentsTable.setItems(FXCollections.observableArrayList(
                    relationshipService.getParentsForPerson(person.getPersonId())
            ));
            childrenTable.setItems(FXCollections.observableArrayList(
                    relationshipService.getChildrenForPerson(person.getPersonId())
            ));
            spousesTable.setItems(FXCollections.observableArrayList(
                    relationshipService.getSpousesForPerson(person.getPersonId())
            ));
        } catch (Exception ex) {
            showError("Could not refresh relationships.", ex);
        }
    }

    private void refreshEligibilityTable(Person person) {
        if (person == null) {
            eligibilityTable.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(person.getPersonId());
            List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
            List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());

            List<OrdinanceEligibilityRow> rows = ordinanceEligibilityService.buildDashboard(
                    person,
                    ordinanceStatus,
                    parents,
                    spouses,
                    allPeople
            );

            eligibilityTable.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception ex) {
            showError("Could not refresh eligibility.", ex);
        }
    }

    private void copySuggestedPersonBucketsToOrdinanceEditor() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }

        try {
            PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(selected.getPersonId());
            List<ParentChildLink> parents = relationshipService.getParentsForPerson(selected.getPersonId());
            List<SpouseLink> spouses = relationshipService.getSpousesForPerson(selected.getPersonId());

            List<OrdinanceEligibilityRow> rows = ordinanceEligibilityService.buildDashboard(
                    selected,
                    ordinanceStatus,
                    parents,
                    spouses,
                    allPeople
            );

            for (OrdinanceEligibilityRow row : rows) {
                if (!row.isPersonLevel()) {
                    continue;
                }

                switch (row.getOrdinanceName()) {
                    case "Baptism", "Confirmation", "Initiatory", "Endowment", "Sealed to Parents" ->
                            ordinancePane.applySuggestedStatus(row.getOrdinanceName(), row.getSuggestedStatus());
                    default -> {
                    }
                }
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Copied");
            alert.setHeaderText(null);
            alert.setContentText("Suggested person-level buckets were copied into the Ordinances editor.");
            alert.showAndWait();
        } catch (Exception ex) {
            showError("Could not copy suggested buckets.", ex);
        }
    }

    private void refreshPedigreeTree(Person person) {
        if (person == null) {
            pedigreeTree.setRoot(new TreeItem<>(new TreePersonNode(-1L, "Select a person to view pedigree.")));
            return;
        }

        TreeItem<TreePersonNode> root = buildAncestorBranch(
                person.getPersonId(),
                buildTreeLabel(person),
                new HashSet<>()
        );

        pedigreeTree.setRoot(root);
        expandFirstTwoLevels(root);
    }

    private void refreshDescendancyTree(Person person) {
        if (person == null) {
            descendancyTree.setRoot(new TreeItem<>(new TreePersonNode(-1L, "Select a person to view descendancy.")));
            return;
        }

        TreeItem<TreePersonNode> root = buildDescendantBranch(
                person.getPersonId(),
                buildTreeLabel(person),
                new HashSet<>()
        );

        descendancyTree.setRoot(root);
        expandFirstTwoLevels(root);
    }

    private TreeItem<TreePersonNode> buildAncestorBranch(long personId, String label, Set<Long> path) {
        TreeItem<TreePersonNode> item = new TreeItem<>(new TreePersonNode(personId, label));

        if (path.contains(personId)) {
            item.getChildren().add(new TreeItem<>(new TreePersonNode(-1L, "(cycle detected)")));
            return item;
        }

        Set<Long> nextPath = new HashSet<>(path);
        nextPath.add(personId);

        List<ParentChildLink> parents = relationshipService.getParentsForPerson(personId);
        for (ParentChildLink parentLink : parents) {
            String parentLabel = buildTreeLabel(parentLink.getParentPersonId(), parentLink.getParentDisplayName());

            TreeItem<TreePersonNode> parentItem = buildAncestorBranch(
                    parentLink.getParentPersonId(),
                    parentLabel,
                    nextPath
            );
            item.getChildren().add(parentItem);
        }

        return item;
    }

    private TreeItem<TreePersonNode> buildDescendantBranch(long personId, String label, Set<Long> path) {
        TreeItem<TreePersonNode> item = new TreeItem<>(new TreePersonNode(personId, label));

        if (path.contains(personId)) {
            item.getChildren().add(new TreeItem<>(new TreePersonNode(-1L, "(cycle detected)")));
            return item;
        }

        Set<Long> nextPath = new HashSet<>(path);
        nextPath.add(personId);

        List<ParentChildLink> children = relationshipService.getChildrenForPerson(personId);
        for (ParentChildLink childLink : children) {
            String childLabel = buildTreeLabel(childLink.getChildPersonId(), childLink.getChildDisplayName());

            TreeItem<TreePersonNode> childItem = buildDescendantBranch(
                    childLink.getChildPersonId(),
                    childLabel,
                    nextPath
            );
            item.getChildren().add(childItem);
        }

        return item;
    }

    private void expandFirstTwoLevels(TreeItem<TreePersonNode> root) {
        if (root == null) {
            return;
        }

        root.setExpanded(true);
        for (TreeItem<TreePersonNode> child : root.getChildren()) {
            child.setExpanded(true);
        }
    }

    private String buildTreeLabel(Person person) {
        String label = person.getDisplayName();
        if (person.getFsPid() != null && !person.getFsPid().isBlank()) {
            label += " [" + person.getFsPid() + "]";
        }
        AncestorLineSummary summary = getAncestorSummary(person);
        if (summary != null && summary.getBadgeStatus() != null) {
            label += " {" + summary.getBadgeStatus().name() + "}";
        }
        return label;
    }

    private String buildTreeLabel(Long personId, String fallbackName) {
        for (Person person : allPeople) {
            if (person.getPersonId().equals(personId)) {
                return buildTreeLabel(person);
            }
        }

        return fallbackName == null || fallbackName.isBlank() ? "(Unnamed Person)" : fallbackName;
    }

    private void openSelectedPersonFromTree(TreeView<TreePersonNode> treeView) {
        TreeItem<TreePersonNode> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getValue() == null) {
            showWarning("Please select a person in the tree.");
            return;
        }

        long personId = selectedItem.getValue().personId();
        if (personId <= 0) {
            return;
        }

        selectPersonInTable(personId);
    }

    private void selectPersonInTable(long personId) {
        if (!peopleNavigatorPane.selectPersonById(personId, true)) {
            showWarning("That person is not available in the active table.");
        }
    }

    private Long getSelectedAncestorLinePersonId() {
        AncestorLineSummary selected = ancestorLinesPane.getSelectedAncestorLine();
        return selected == null ? null : selected.getAncestorPersonId();
    }

    private boolean reselectAncestorLine(Long ancestorPersonId) {
        return ancestorLinesPane.reselectAncestorLine(ancestorPersonId);
    }

    private void reselectPerson(Long personId) {
        peopleNavigatorPane.reselectPerson(personId);
    }

    private Person findPersonById(Long personId) {
        if (personId == null) {
            return null;
        }

        for (Person person : allPeople) {
            if (personId.equals(person.getPersonId())) {
                return person;
            }
        }

        return null;
    }

    private boolean isBornMoreThan110YearsAgo(Person person) {
        Integer birthYear = extractYear(person == null ? null : person.getBirthDateText());
        if (birthYear == null) {
            return false;
        }

        return birthYear <= LocalDate.now().minusYears(110).getYear();
    }

    private boolean hasIncompleteTrackedOrdinances(Person person) {
        if (person == null || person.getPersonId() == null) {
            return false;
        }

        PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(person.getPersonId());
        List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
        List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());
        List<OrdinanceEligibilityRow> rows = ordinanceEligibilityService.buildDashboard(
                person,
                ordinanceStatus,
                parents,
                spouses,
                allPeople
        );

        for (OrdinanceEligibilityRow row : rows) {
            if (row.getRecordedStatus() != OrdinanceStatus.COMPLETE
                    && row.getRecordedStatus() != OrdinanceStatus.NOT_APPLICABLE) {
                return true;
            }
        }

        return false;
    }

    private Integer extractYear(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        for (int i = 0; i <= text.length() - 4; i++) {
            String candidate = text.substring(i, i + 4);
            if (candidate.chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(candidate);
            }
        }

        return null;
    }

    private void addPerson() {
        PersonEditorDialog dialog = new PersonEditorDialog(null, null);
        Optional<PersonEditorDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                Person saved = personService.savePerson(input.getPerson());
                PersonOrdinanceStatus ordinanceStatus = input.getOrdinanceStatus();
                ordinanceStatus.setPersonId(saved.getPersonId());
                ordinanceService.save(ordinanceStatus);
                refreshPeople();
            } catch (Exception ex) {
                showError("Could not save the new person.", ex);
            }
        });
    }

    private void editSelectedPerson() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person to edit.");
            return;
        }

        PersonEditorDialog dialog = new PersonEditorDialog(
                selected,
                ordinanceService.getOrCreateForPerson(selected.getPersonId())
        );
        Optional<PersonEditorDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                Person saved = personService.savePerson(input.getPerson());
                PersonOrdinanceStatus ordinanceStatus = input.getOrdinanceStatus();
                ordinanceStatus.setPersonId(saved.getPersonId());
                ordinanceService.save(ordinanceStatus);
                refreshPeople();
            } catch (Exception ex) {
                showError("Could not save changes.", ex);
            }
        });
    }

    private void deleteSelectedPerson() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Person");
        confirm.setHeaderText("Soft delete the selected person?");
        confirm.setContentText("This marks the person as deleted but does not remove the row from the database.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            try {
                personService.deletePerson(selected.getPersonId());
                refreshPeople();
            } catch (Exception ex) {
                showError("Could not delete the person.", ex);
            }
        }
    }

    private void setSelectedAsRoot() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person to set as root.");
            return;
        }

        try {
            personService.setRootPerson(selected.getPersonId());
            refreshPeople();
        } catch (Exception ex) {
            showError("Could not set the root person.", ex);
        }
    }

    private void addParentToSelectedPerson() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }

        List<Person> candidates = getOtherPeople(selected);
        ParentChildDialog dialog = new ParentChildDialog(
                true,
                selected,
                candidates,
                buildSpouseCandidatesByPersonId(candidates),
                null
        );
        Optional<ParentChildDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                long parentPersonId = resolveRelatedPersonId(input);

                relationshipService.addParent(
                        selected.getPersonId(),
                        parentPersonId,
                        input.getChildOrder(),
                        input.getNotes()
                );

                if (input.getMirrorSpousePersonId() != null) {
                    syncMirroredParentLink(
                            selected.getPersonId(),
                            input.getMirrorSpousePersonId(),
                            input.getChildOrder(),
                            input.getNotes()
                    );
                }

                refreshPeople();
                reselectPerson(selected.getPersonId());
            } catch (Exception ex) {
                showError("Could not add the parent relationship.", ex);
            }
        });
    }

    private void editSelectedParentLink() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        ParentChildLink selectedLink = parentsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }
        if (selectedLink == null) {
            showWarning("Please select a parent link to edit.");
            return;
        }

        List<Person> candidates = getOtherPeople(selected);
        ParentChildDialog dialog = new ParentChildDialog(
                true,
                selected,
                candidates,
                buildSpouseCandidatesByPersonId(candidates),
                selectedLink
        );
        Optional<ParentChildDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                long parentPersonId = resolveRelatedPersonId(input);

                relationshipService.updateParentLink(
                        selectedLink.getLinkId(),
                        selected.getPersonId(),
                        parentPersonId,
                        input.getChildOrder(),
                        input.getNotes()
                );

                if (input.getMirrorSpousePersonId() != null) {
                    syncMirroredParentLink(
                            selected.getPersonId(),
                            input.getMirrorSpousePersonId(),
                            input.getChildOrder(),
                            input.getNotes()
                    );
                }

                refreshPeople();
                reselectPerson(selected.getPersonId());
            } catch (Exception ex) {
                showError("Could not update the parent relationship.", ex);
            }
        });
    }

    private void removeSelectedParentLink() {
        ParentChildLink selectedLink = parentsTable.getSelectionModel().getSelectedItem();
        if (selectedLink == null) {
            showWarning("Please select a parent link to remove.");
            return;
        }

        if (!confirmRelationshipDelete("Remove Parent Link", "Remove the selected parent relationship?")) {
            return;
        }

        try {
            relationshipService.deleteParentLink(selectedLink.getLinkId());
            refreshRelationshipTables(personTable.getSelectionModel().getSelectedItem());
            refreshPedigreeTree(personTable.getSelectionModel().getSelectedItem());
            refreshDescendancyTree(personTable.getSelectionModel().getSelectedItem());
            updateSummaryCard(personTable.getSelectionModel().getSelectedItem());
            refreshEligibilityTable(personTable.getSelectionModel().getSelectedItem());
            refreshWorkQueue();
        } catch (Exception ex) {
            showError("Could not remove the parent relationship.", ex);
        }
    }

    private void addChildToSelectedPerson() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }

        List<Person> candidates = getEligibleChildrenForParentSelection(selected, null);
        ParentChildDialog dialog = new ParentChildDialog(false, selected, candidates);
        Optional<ParentChildDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                long childPersonId = resolveRelatedPersonId(input);

                relationshipService.addChild(
                        selected.getPersonId(),
                        childPersonId,
                        input.getChildOrder(),
                        input.getNotes()
                );

                refreshPeople();
                reselectPerson(selected.getPersonId());
            } catch (Exception ex) {
                showError("Could not add the child relationship.", ex);
            }
        });
    }

    private void editSelectedChildLink() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        ParentChildLink selectedLink = childrenTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }
        if (selectedLink == null) {
            showWarning("Please select a child link to edit.");
            return;
        }

        List<Person> candidates = getEligibleChildrenForParentSelection(selected, selectedLink);
        ParentChildDialog dialog = new ParentChildDialog(false, selected, candidates, selectedLink);
        Optional<ParentChildDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                long childPersonId = resolveRelatedPersonId(input);

                relationshipService.updateChildLink(
                        selectedLink.getLinkId(),
                        selected.getPersonId(),
                        childPersonId,
                        input.getChildOrder(),
                        input.getNotes()
                );

                refreshPeople();
                reselectPerson(selected.getPersonId());
            } catch (Exception ex) {
                showError("Could not update the child relationship.", ex);
            }
        });
    }

    private void removeSelectedChildLink() {
        ParentChildLink selectedLink = childrenTable.getSelectionModel().getSelectedItem();
        if (selectedLink == null) {
            showWarning("Please select a child link to remove.");
            return;
        }

        if (!confirmRelationshipDelete("Remove Child Link", "Remove the selected child relationship?")) {
            return;
        }

        try {
            relationshipService.deleteChildLink(selectedLink.getLinkId());
            refreshRelationshipTables(personTable.getSelectionModel().getSelectedItem());
            refreshPedigreeTree(personTable.getSelectionModel().getSelectedItem());
            refreshDescendancyTree(personTable.getSelectionModel().getSelectedItem());
            updateSummaryCard(personTable.getSelectionModel().getSelectedItem());
            refreshEligibilityTable(personTable.getSelectionModel().getSelectedItem());
            refreshWorkQueue();
        } catch (Exception ex) {
            showError("Could not remove the child relationship.", ex);
        }
    }

    private long resolveRelatedPersonId(ParentChildDialog.Result input) {
        if (input.getRelatedPersonId() != null) {
            return input.getRelatedPersonId();
        }

        if (input.getNewPerson() != null) {
            Person saved = personService.savePerson(input.getNewPerson());
            return saved.getPersonId();
        }

        throw new IllegalArgumentException("No related person was selected or created.");
    }

    private void addSpouseToSelectedPerson() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }

        List<Person> candidates = getOtherPeople(selected);
        SpouseLinkDialog dialog = new SpouseLinkDialog(selected, candidates, getChildrenForSpouseCopySelection(selected));
        Optional<SpouseLinkDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                long spousePersonId = resolveSpousePersonId(input);

                relationshipService.addSpouse(
                        selected.getPersonId(),
                        spousePersonId,
                        input.getMarriageDateText(),
                        input.getMarriageNotes(),
                        input.getSealingStatus(),
                        input.getSealingStatusDate(),
                        input.getSealingNotes()
                );

                copyChildrenToSpouse(input.getChildPersonIdsToCopy(), spousePersonId);

                refreshPeople();
                reselectPerson(selected.getPersonId());
            } catch (Exception ex) {
                showError("Could not add the spouse relationship.", ex);
            }
        });
    }

    private void editSelectedSpouseLink() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        SpouseLink selectedLink = spousesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }
        if (selectedLink == null) {
            showWarning("Please select a spouse link to edit.");
            return;
        }

        List<Person> candidates = getOtherPeople(selected);
        SpouseLinkDialog dialog = new SpouseLinkDialog(selected, candidates, List.of(), selectedLink);
        Optional<SpouseLinkDialog.Result> result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                long spousePersonId = resolveSpousePersonId(input);

                relationshipService.updateSpouseLink(
                        selectedLink.getSpouseLinkId(),
                        selected.getPersonId(),
                        spousePersonId,
                        input.getMarriageDateText(),
                        input.getMarriageNotes(),
                        input.getSealingStatus(),
                        input.getSealingStatusDate(),
                        input.getSealingNotes()
                );

                refreshPeople();
                reselectPerson(selected.getPersonId());
            } catch (Exception ex) {
                showError("Could not update the spouse relationship.", ex);
            }
        });
    }

    private void removeSelectedSpouseLink() {
        SpouseLink selectedLink = spousesTable.getSelectionModel().getSelectedItem();
        if (selectedLink == null) {
            showWarning("Please select a spouse link to remove.");
            return;
        }

        if (!confirmRelationshipDelete("Remove Spouse Link", "Remove the selected spouse relationship?")) {
            return;
        }

        try {
            relationshipService.deleteSpouseLink(selectedLink.getSpouseLinkId());
            refreshRelationshipTables(personTable.getSelectionModel().getSelectedItem());
            updateSummaryCard(personTable.getSelectionModel().getSelectedItem());
            refreshEligibilityTable(personTable.getSelectionModel().getSelectedItem());
            refreshWorkQueue();
        } catch (Exception ex) {
            showError("Could not remove the spouse relationship.", ex);
        }
    }

    private long resolveSpousePersonId(SpouseLinkDialog.Result input) {
        if (input.getSpousePersonId() != null) {
            return input.getSpousePersonId();
        }

        if (input.getNewPerson() != null) {
            Person saved = personService.savePerson(input.getNewPerson());
            return saved.getPersonId();
        }

        throw new IllegalArgumentException("No spouse was selected or created.");
    }

    private void updateOrdinanceEditor(Person person) {
        if (person == null) {
            ordinancePane.clear();
            return;
        }

        try {
            PersonOrdinanceStatus status = ordinanceService.getOrCreateForPerson(person.getPersonId());
            ordinancePane.populate(person, status, relationshipService.getSpousesForPerson(person.getPersonId()));
        } catch (Exception ex) {
            showError("Could not load ordinances.", ex);
        }
    }

    private void updateSelectedOrdinanceStatus(OrdinanceStatus status) {
        Person selectedPerson = personTable.getSelectionModel().getSelectedItem();
        OrdinancePane.OrdinanceEditorRow selectedRow = ordinancePane.getSelectedOrdinanceRow();

        if (selectedPerson == null) {
            showWarning("Please select a person first.");
            return;
        }

        if (selectedRow == null) {
            showWarning("Please select an ordinance row first.");
            return;
        }

        try {
            updateOrdinanceStatus(selectedPerson.getPersonId(), selectedRow.ordinanceName(), selectedRow.spouseLink(), status);
            refreshPeople();
            ordinancePane.reselectOrdinanceRow(selectedRow);
        } catch (Exception ex) {
            showError("Could not update ordinance status.", ex);
        }
    }

    private void updateSelectedLineWorkbenchStatus(OrdinanceStatus status) {
        LineWorkbenchRow selectedRow = ancestorLinesPane.getSelectedWorkbenchRow();
        Long selectedAncestorId = getSelectedAncestorLinePersonId();

        if (selectedRow == null) {
            showWarning("Please select a work item first.");
            return;
        }

        try {
            updateOrdinanceStatus(selectedRow.personId(), selectedRow.ordinanceName(), selectedRow.spouseLink(), status);
            refreshPeople();
            reselectAncestorLine(selectedAncestorId);
            ancestorLinesPane.reselectWorkbenchRow(selectedRow);
        } catch (Exception ex) {
            showError("Could not update the line workbench item.", ex);
        }
    }

    private void updateOrdinanceStatus(
            Long personId,
            String ordinanceName,
            SpouseLink spouseLink,
            OrdinanceStatus status
    ) {
        if (personId == null) {
            throw new IllegalArgumentException("A person must be selected before updating ordinance status.");
        }

        if (spouseLink == null) {
            PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(personId);
            applyPersonLevelStatus(ordinanceStatus, ordinanceName, status);
            ordinanceService.save(ordinanceStatus);
            return;
        }

        relationshipService.updateSpouseLink(
                spouseLink.getSpouseLinkId(),
                spouseLink.getPersonAId(),
                spouseLink.getPersonBId(),
                spouseLink.getMarriageDateText(),
                spouseLink.getMarriageNotes(),
                status,
                spouseLink.getSealingStatusDate(),
                spouseLink.getSealingNotes()
        );
    }

    private void applyPersonLevelStatus(PersonOrdinanceStatus ordinanceStatus, String ordinanceName, OrdinanceStatus status) {
        switch (ordinanceName) {
            case "Baptism" -> ordinanceStatus.setBaptismStatus(status);
            case "Confirmation" -> ordinanceStatus.setConfirmationStatus(status);
            case "Initiatory" -> ordinanceStatus.setInitiatoryStatus(status);
            case "Endowment" -> ordinanceStatus.setEndowmentStatus(status);
            case "Sealed to Parents" -> ordinanceStatus.setSealedToParentsStatus(status);
            default -> throw new IllegalArgumentException("Unsupported person ordinance: " + ordinanceName);
        }
    }

    private void saveOrdinancesForSelectedPerson() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }

        try {
            PersonOrdinanceStatus status = ordinancePane.buildPersonOrdinanceStatus(selected.getPersonId());
            ordinanceService.save(status);
            Map<Long, OrdinanceStatus> spouseSelections = ordinancePane.getSpouseSealingSelections();
            for (SpouseLink spouseLink : relationshipService.getSpousesForPerson(selected.getPersonId())) {
                OrdinanceStatus spouseStatus = spouseSelections.get(spouseLink.getSpouseLinkId());
                if (spouseStatus == null) {
                    continue;
                }

                relationshipService.updateSpouseLink(
                        spouseLink.getSpouseLinkId(),
                        spouseLink.getPersonAId(),
                        spouseLink.getPersonBId(),
                        spouseLink.getMarriageDateText(),
                        spouseLink.getMarriageNotes(),
                        spouseStatus,
                        spouseLink.getSealingStatusDate(),
                        spouseLink.getSealingNotes()
                );
            }

            updateOrdinanceEditor(selected);
            refreshEligibilityTable(selected);
            refreshWorkQueue();
        } catch (Exception ex) {
            showError("Could not save ordinances.", ex);
        }
    }

    private boolean confirmRelationshipDelete(String title, String headerText) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(title);
        confirm.setHeaderText(headerText);
        confirm.setContentText("This marks the relationship as deleted but does not remove the row from the database.");

        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    private List<Person> getOtherPeople(Person selected) {
        return allPeople.stream()
                .filter(person -> !person.getPersonId().equals(selected.getPersonId()))
                .collect(Collectors.toList());
    }

    private List<Person> getEligibleChildrenForParentSelection(Person selectedParent, ParentChildLink existingLink) {
        Long existingChildId = existingLink == null ? null : existingLink.getChildPersonId();

        return allPeople.stream()
                .filter(person -> !person.getPersonId().equals(selectedParent.getPersonId()))
                .filter(person -> canAssignAdditionalParent(person, existingChildId))
                .collect(Collectors.toList());
    }

    private boolean canAssignAdditionalParent(Person candidateChild, Long existingChildId) {
        if (candidateChild == null || candidateChild.getPersonId() == null) {
            return false;
        }

        if (existingChildId != null && existingChildId.equals(candidateChild.getPersonId())) {
            return true;
        }

        return relationshipService.getParentsForPerson(candidateChild.getPersonId()).size() < 2;
    }

    private List<Person> getChildrenForSpouseCopySelection(Person selectedPerson) {
        return relationshipService.getChildrenForPerson(selectedPerson.getPersonId()).stream()
                .map(link -> findPersonById(link.getChildPersonId()))
                .filter(child -> child != null)
                .filter(child -> relationshipService.getParentsForPerson(child.getPersonId()).size() < 2)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<Long, List<Person>> buildSpouseCandidatesByPersonId(List<Person> people) {
        Map<Long, Person> peopleById = people.stream()
                .filter(person -> person.getPersonId() != null)
                .collect(Collectors.toMap(Person::getPersonId, person -> person));

        return people.stream()
                .filter(person -> person.getPersonId() != null)
                .collect(Collectors.toMap(
                        Person::getPersonId,
                        person -> relationshipService.getSpousesForPerson(person.getPersonId()).stream()
                                .map(link -> peopleById.get(link.getOtherPersonId(person.getPersonId())))
                                .filter(spouse -> spouse != null)
                                .collect(Collectors.toList())
                ));
    }

    private void syncMirroredParentLink(long childPersonId, long spousePersonId, Integer childOrder, String notes) {
        for (ParentChildLink link : relationshipService.getParentsForPerson(childPersonId)) {
            if (link.getParentPersonId() != null && link.getParentPersonId().equals(spousePersonId)) {
                relationshipService.updateParentLink(
                        link.getLinkId(),
                        childPersonId,
                        spousePersonId,
                        childOrder,
                        notes
                );
                return;
            }
        }

        relationshipService.addParent(
                childPersonId,
                spousePersonId,
                childOrder,
                notes
        );
    }

    private void copyChildrenToSpouse(List<Long> childPersonIds, long spousePersonId) {
        if (childPersonIds == null || childPersonIds.isEmpty()) {
            return;
        }

        for (Long childPersonId : childPersonIds) {
            if (childPersonId == null) {
                continue;
            }
            syncMirroredParentLink(childPersonId, spousePersonId, null, null);
        }
    }

    private void updateSummaryCard(Person person) {
        if (person == null) {
            summaryPreferredNameValue.setText("");
            summaryFsPidValue.setText("");
            summaryRootValue.setText("");
            summaryParentsValue.setText("");
            summaryChildrenValue.setText("");
            summarySpousesValue.setText("");
            summaryAncestorsValue.setText("");
            summaryDescendantsValue.setText("");
            summaryLineBadgeValue.setText("");
            summaryLineNextAvailableValue.setText("");
            summaryLineOpenValue.setText("");
            summaryLineSoonValue.setText("");
            summaryLineWaitingValue.setText("");
            summaryLineUnresolvedValue.setText("");
            summaryLineCompleteValue.setText("");
            summaryLineStewardshipValue.setText("");
            summaryLineReasonValue.setText("");
            return;
        }

        try {
            List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
            List<ParentChildLink> children = relationshipService.getChildrenForPerson(person.getPersonId());
            List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());

            Set<Long> ancestorIds = new HashSet<>();
            collectAncestorIds(person.getPersonId(), ancestorIds, new HashSet<>());

            Set<Long> descendantIds = new HashSet<>();
            collectDescendantIds(person.getPersonId(), descendantIds, new HashSet<>());

            summaryPreferredNameValue.setText(nullSafe(person.getDisplayName()));
            summaryFsPidValue.setText(nullSafe(person.getFsPid()));
            summaryRootValue.setText(person.isRoot() ? "Yes" : "No");
            summaryParentsValue.setText(String.valueOf(parents.size()));
            summaryChildrenValue.setText(String.valueOf(children.size()));
            summarySpousesValue.setText(String.valueOf(spouses.size()));
            summaryAncestorsValue.setText(String.valueOf(ancestorIds.size()));
            summaryDescendantsValue.setText(String.valueOf(descendantIds.size()));

            AncestorLineSummary summary = getAncestorSummary(person);
            summaryLineBadgeValue.setText(summary == null || summary.getBadgeStatus() == null ? "" : summary.getBadgeStatus().name());
            summaryLineNextAvailableValue.setText(summary == null ? "" : formatSummaryDate(summary.getNextAvailableDate()));
            summaryLineOpenValue.setText(summary == null ? "" : String.valueOf(summary.getOpenCount()));
            summaryLineSoonValue.setText(summary == null ? "" : String.valueOf(summary.getOpeningSoonCount()));
            summaryLineWaitingValue.setText(summary == null ? "" : String.valueOf(summary.getWaiting110Count()));
            summaryLineUnresolvedValue.setText(summary == null ? "" : String.valueOf(summary.getUnresolvedCount()));
            summaryLineCompleteValue.setText(summary == null ? "" : String.valueOf(summary.getCompleteCount()));
            summaryLineStewardshipValue.setText(summary == null || summary.getStewardshipStatus() == null ? "" : summary.getStewardshipStatus().name());
            summaryLineReasonValue.setText(summary == null ? "" : nullSafe(summary.getSummaryReason()));
        } catch (Exception ex) {
            showError("Could not update the summary card.", ex);
        }
    }

    private void collectAncestorIds(long personId, Set<Long> collected, Set<Long> path) {
        if (path.contains(personId)) {
            return;
        }

        Set<Long> nextPath = new HashSet<>(path);
        nextPath.add(personId);

        List<ParentChildLink> parents = relationshipService.getParentsForPerson(personId);
        for (ParentChildLink parentLink : parents) {
            Long parentId = parentLink.getParentPersonId();
            if (parentId != null && collected.add(parentId)) {
                collectAncestorIds(parentId, collected, nextPath);
            }
        }
    }

    private List<Person> collectAncestorsForPerson(Person person) {
        Set<Long> ancestorIds = new HashSet<>();
        collectAncestorIds(person.getPersonId(), ancestorIds, new HashSet<>());

        return allPeople.stream()
                .filter(candidate -> candidate.getPersonId() != null && ancestorIds.contains(candidate.getPersonId()))
                .collect(Collectors.toList());
    }

    private void collectDescendantIds(long personId, Set<Long> collected, Set<Long> path) {
        if (path.contains(personId)) {
            return;
        }

        Set<Long> nextPath = new HashSet<>(path);
        nextPath.add(personId);

        List<ParentChildLink> children = relationshipService.getChildrenForPerson(personId);
        for (ParentChildLink childLink : children) {
            Long childId = childLink.getChildPersonId();
            if (childId != null && collected.add(childId)) {
                collectDescendantIds(childId, collected, nextPath);
            }
        }
    }

    private void updateDetailArea(Person person) {
        if (person == null) {
            detailArea.setText("");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("ID: ").append(person.getPersonId()).append('\n');
        builder.append("UUID: ").append(nullSafe(person.getStableUuid())).append('\n');
        builder.append("FamilySearch PID: ").append(nullSafe(person.getFsPid())).append('\n');
        builder.append("Preferred Name: ").append(nullSafe(person.getPreferredName())).append('\n');
        builder.append("Given Names: ").append(nullSafe(person.getGivenNames())).append('\n');
        builder.append("Surname: ").append(nullSafe(person.getSurname())).append('\n');
        builder.append("Sex: ").append(person.getSex()).append('\n');
        builder.append("Living: ").append(person.isLiving() ? "Yes" : "No").append('\n');
        builder.append("Birth: ").append(nullSafe(person.getBirthDateText())).append('\n');
        builder.append("Birth Precision: ").append(person.getBirthDatePrecision()).append('\n');
        builder.append("Death: ").append(nullSafe(person.getDeathDateText())).append('\n');
        builder.append("Death Precision: ").append(person.getDeathDatePrecision()).append('\n');
        builder.append("Reviewed Status: ").append(person.getReviewedStatus()).append('\n');
        builder.append("Root Person: ").append(person.isRoot() ? "Yes" : "No").append('\n');
        builder.append("Created At: ").append(nullSafe(person.getCreatedAt())).append('\n');
        builder.append("Updated At: ").append(nullSafe(person.getUpdatedAt())).append('\n');
        builder.append('\n');
        builder.append("Notes:\n").append(nullSafe(person.getNotes()));

        detailArea.setText(builder.toString());
    }

    private void updateStatus() {
        int visibleCount = personTable.getItems().size();
        int totalCount = allPeople.size();
        String rootName = personService.getRootPerson()
                .map(Person::getDisplayName)
                .orElse("(none)");

        statusLabel.setText("Visible people: " + visibleCount + " / " + totalCount + "    Root person: " + rootName);
    }

    private void clearSelectionDependentViews() {
        ancestorSummaryCache.clear();
        updateSummaryCard(null);
        updateDetailArea(null);
        refreshRelationshipTables(null);
        refreshReportsTable(null);
        refreshAncestorLineTable(null);
        refreshPedigreeTree(null);
        refreshDescendancyTree(null);
        updateOrdinanceEditor(null);
        refreshEligibilityTable(null);
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
        ex.printStackTrace();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private AncestorLineSummary getAncestorSummary(Person person) {
        if (person == null || person.getPersonId() == null) {
            return null;
        }

        return ancestorSummaryCache.computeIfAbsent(
                person.getPersonId(),
                ignored -> applyLineStewardship(ancestorLineSummaryService.buildSummary(person, allPeople))
        );
    }

    private void applyLineStewardship(List<AncestorLineSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return;
        }

        List<Long> ancestorIds = summaries.stream()
                .map(AncestorLineSummary::getAncestorPersonId)
                .filter(id -> id != null)
                .toList();

        Map<Long, LineStewardship> stewardshipByAncestorId = lineStewardshipService.getByAncestorIds(ancestorIds);
        for (AncestorLineSummary summary : summaries) {
            applyLineStewardship(summary, stewardshipByAncestorId.get(summary.getAncestorPersonId()));
        }
    }

    private AncestorLineSummary applyLineStewardship(AncestorLineSummary summary) {
        if (summary == null || summary.getAncestorPersonId() == null) {
            return summary;
        }

        applyLineStewardship(summary, lineStewardshipService.getOrCreateForAncestor(summary.getAncestorPersonId()));
        return summary;
    }

    private void applyLineStewardship(AncestorLineSummary summary, LineStewardship stewardship) {
        if (summary == null) {
            return;
        }

        summary.setStewardshipStatus(
                stewardship == null ? LineStewardshipStatus.UNASSIGNED : stewardship.getStewardshipStatus()
        );
        summary.setStewardshipNotes(stewardship == null ? null : stewardship.getNotes());
    }

    private void reloadSelectedAncestorStewardship() {
        AncestorLineSummary selected = ancestorLinesPane.getSelectedAncestorLine();
        if (selected == null || selected.getAncestorPersonId() == null) {
            ancestorLinesPane.loadStewardship(null);
            return;
        }

        LineStewardship stewardship = lineStewardshipService.getOrCreateForAncestor(selected.getAncestorPersonId());
        applyLineStewardship(selected, stewardship);
        updateMatchingReportStewardship(stewardship);
        ancestorLinesPane.loadStewardship(selected);
    }

    private void saveSelectedAncestorStewardship() {
        AncestorLineSummary selected = ancestorLinesPane.getSelectedAncestorLine();
        if (selected == null || selected.getAncestorPersonId() == null) {
            showWarning("Please select an ancestor line first.");
            return;
        }

        try {
            LineStewardship stewardship = new LineStewardship();
            stewardship.setAncestorPersonId(selected.getAncestorPersonId());
            stewardship.setStewardshipStatus(ancestorLinesPane.getStewardshipStatus());
            stewardship.setNotes(ancestorLinesPane.getStewardshipNotes());

            LineStewardship saved = lineStewardshipService.save(stewardship);

            applyLineStewardship(selected, saved);
            updateMatchingReportStewardship(saved);
            ancestorSummaryCache.clear();
            updateSummaryCard(personTable.getSelectionModel().getSelectedItem());
            ancestorLinesPane.loadStewardship(selected);
        } catch (Exception ex) {
            showError("Could not save line stewardship.", ex);
        }
    }

    private void updateMatchingReportStewardship(LineStewardship stewardship) {
        if (stewardship == null || stewardship.getAncestorPersonId() == null) {
            return;
        }

        for (AncestorLineSummary row : allReportRows) {
            if (row.getAncestorPersonId() != null && row.getAncestorPersonId().equals(stewardship.getAncestorPersonId())) {
                applyLineStewardship(row, stewardship);
            }
        }

        reportsTable.refresh();
        ancestorLinesPane.refreshAncestorLineTable();
    }

    private String formatSummaryDate(LocalDate date) {
        return date == null ? "" : SUMMARY_DATE_FORMAT.format(date);
    }

    private record TreePersonNode(long personId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
