package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.application.BackupService;
import family.balling.descendencytracker.application.OrdinanceEligibilityService;
import family.balling.descendencytracker.application.OrdinanceService;
import family.balling.descendencytracker.application.PersonService;
import family.balling.descendencytracker.application.RelationshipService;
import family.balling.descendencytracker.application.WorkQueueService;
import family.balling.descendencytracker.domain.OrdinanceEligibilityRow;
import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.WorkQueueRow;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MainView extends BorderPane {
    private static final DateTimeFormatter BACKUP_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final PersonService personService;
    private final RelationshipService relationshipService;
    private final OrdinanceService ordinanceService;
    private final OrdinanceEligibilityService ordinanceEligibilityService;
    private final BackupService backupService;
    private final WorkQueueService workQueueService;

    private final ObservableList<Person> allPeople = FXCollections.observableArrayList();
    private final FilteredList<Person> filteredPeople = new FilteredList<>(allPeople, person -> true);
    private final SortedList<Person> sortedPeople = new SortedList<>(filteredPeople);

    private final ObservableList<WorkQueueRow> allWorkQueueRows = FXCollections.observableArrayList();
    private final FilteredList<WorkQueueRow> filteredWorkQueueRows = new FilteredList<>(allWorkQueueRows, row -> true);
    private final SortedList<WorkQueueRow> sortedWorkQueueRows = new SortedList<>(filteredWorkQueueRows);

    private final TableView<Person> personTable = new TableView<>();
    private final TableView<ParentChildLink> parentsTable = new TableView<>();
    private final TableView<ParentChildLink> childrenTable = new TableView<>();
    private final TableView<SpouseLink> spousesTable = new TableView<>();
    private final TableView<OrdinanceEligibilityRow> eligibilityTable = new TableView<>();
    private final TableView<WorkQueueRow> workQueueTable = new TableView<>();

    private final TreeView<TreePersonNode> pedigreeTree = new TreeView<>();
    private final TreeView<TreePersonNode> descendancyTree = new TreeView<>();

    private final TextArea detailArea = new TextArea();
    private final Label statusLabel = new Label();

    private final TextField searchField = new TextField();
    private final CheckBox rootOnlyCheckBox = new CheckBox("Only root");

    private final TextField workQueueSearchField = new TextField();
    private final CheckBox workQueueActionableOnlyCheckBox = new CheckBox("Open/Soon only");
    private final ComboBox<String> workQueueBucketFilterCombo = new ComboBox<>();

    private final Label summaryPreferredNameValue = new Label();
    private final Label summaryFsPidValue = new Label();
    private final Label summaryRootValue = new Label();
    private final Label summaryParentsValue = new Label();
    private final Label summaryChildrenValue = new Label();
    private final Label summarySpousesValue = new Label();
    private final Label summaryAncestorsValue = new Label();
    private final Label summaryDescendantsValue = new Label();

    private final Label ordinancesHeaderLabel = new Label("Select a person to edit ordinances.");
    private final ComboBox<OrdinanceStatus> baptismStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> confirmationStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> initiatoryStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> endowmentStatusCombo = new ComboBox<>();
    private final ComboBox<OrdinanceStatus> sealedToParentsStatusCombo = new ComboBox<>();
    private final TextArea ordinanceNotesArea = new TextArea();

    public MainView(PersonService personService,
                    RelationshipService relationshipService,
                    OrdinanceService ordinanceService,
                    OrdinanceEligibilityService ordinanceEligibilityService,
                    BackupService backupService,
                    WorkQueueService workQueueService) {
        this.personService = personService;
        this.relationshipService = relationshipService;
        this.ordinanceService = ordinanceService;
        this.ordinanceEligibilityService = ordinanceEligibilityService;
        this.backupService = backupService;
        this.workQueueService = workQueueService;
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

        configurePersonSearchBar();
        ToolBar filterToolBar = buildFilterToolBar();

        setTop(new VBox(6, personToolBar, filterToolBar));

        configurePersonTable();
        configureParentsTable();
        configureChildrenTable();
        configureSpousesTable();
        configureEligibilityTable();
        configureWorkQueueControls();
        configureWorkQueueTable();
        configurePedigreeTree();
        configureDescendancyTree();
        configureOrdinanceControls();

        sortedPeople.comparatorProperty().bind(personTable.comparatorProperty());
        personTable.setItems(sortedPeople);

        sortedWorkQueueRows.comparatorProperty().bind(workQueueTable.comparatorProperty());
        workQueueTable.setItems(sortedWorkQueueRows);

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
        relationshipTabs.getTabs().add(new Tab("Pedigree", buildPedigreeTabContent()));
        relationshipTabs.getTabs().add(new Tab("Descendancy", buildDescendancyTabContent()));
        relationshipTabs.getTabs().add(new Tab("Parents", buildParentsTabContent()));
        relationshipTabs.getTabs().add(new Tab("Children", buildChildrenTabContent()));
        relationshipTabs.getTabs().add(new Tab("Spouses", buildSpousesTabContent()));
        relationshipTabs.getTabs().forEach(tab -> tab.setClosable(false));

        SplitPane splitPane = new SplitPane(personTable, relationshipTabs);
        splitPane.setDividerPositions(0.60);

        setCenter(splitPane);
        setBottom(statusLabel);

        personTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            updateSummaryCard(newSelection);
            updateDetailArea(newSelection);
            refreshRelationshipTables(newSelection);
            refreshPedigreeTree(newSelection);
            refreshDescendancyTree(newSelection);
            updateOrdinanceEditor(newSelection);
            refreshEligibilityTable(newSelection);
            updateStatus();
        });
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

    private void configurePersonSearchBar() {
        searchField.setPromptText("Search name, FamilySearch PID, dates, notes...");
        searchField.setPrefWidth(320);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
        rootOnlyCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
    }

    private ToolBar buildFilterToolBar() {
        Button clearFiltersButton = new Button("Clear Filters");
        clearFiltersButton.setOnAction(event -> clearPersonFilters());

        return new ToolBar(
                new Label("Find"),
                searchField,
                clearFiltersButton,
                rootOnlyCheckBox
        );
    }

    private void clearPersonFilters() {
        searchField.clear();
        rootOnlyCheckBox.setSelected(false);
    }

    private void applyPersonFilter() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean rootOnly = rootOnlyCheckBox.isSelected();

        filteredPeople.setPredicate(person -> {
            if (person == null) {
                return false;
            }

            if (rootOnly && !person.isRoot()) {
                return false;
            }

            if (searchText.isBlank()) {
                return true;
            }

            return containsIgnoreCase(person.getDisplayName(), searchText)
                    || containsIgnoreCase(person.getPreferredName(), searchText)
                    || containsIgnoreCase(person.getFsPid(), searchText)
                    || containsIgnoreCase(person.getGivenNames(), searchText)
                    || containsIgnoreCase(person.getSurname(), searchText)
                    || containsIgnoreCase(person.getBirthDateText(), searchText)
                    || containsIgnoreCase(person.getDeathDateText(), searchText)
                    || containsIgnoreCase(person.getNotes(), searchText);
        });

        reconcileSelectionAfterFilter();
        updateStatus();
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

    private void clearWorkQueueFilters() {
        workQueueSearchField.clear();
        workQueueActionableOnlyCheckBox.setSelected(false);
        workQueueBucketFilterCombo.setValue("All");
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

    private boolean containsIgnoreCase(String value, String searchTextLower) {
        return value != null && value.toLowerCase().contains(searchTextLower);
    }

    private void reconcileSelectionAfterFilter() {
        Person selected = personTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            if (!personTable.getItems().isEmpty()) {
                personTable.getSelectionModel().selectFirst();
            } else {
                clearSelectionDependentViews();
            }
            return;
        }

        boolean stillVisible = personTable.getItems().stream()
                .anyMatch(person -> person.getPersonId().equals(selected.getPersonId()));

        if (!stillVisible) {
            if (!personTable.getItems().isEmpty()) {
                personTable.getSelectionModel().selectFirst();
            } else {
                personTable.getSelectionModel().clearSelection();
                clearSelectionDependentViews();
            }
        }
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
        grid.add(summaryDescendantsValue, 3, row);

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
        Button saveButton = new Button("Save Ordinances");
        Button refreshButton = new Button("Reload Ordinances");

        saveButton.setOnAction(event -> saveOrdinancesForSelectedPerson());
        refreshButton.setOnAction(event -> updateOrdinanceEditor(personTable.getSelectionModel().getSelectedItem()));

        ToolBar toolbar = new ToolBar(saveButton, refreshButton);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

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

        VBox box = new VBox(8, ordinancesHeaderLabel, toolbar, grid);
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

    private void configureOrdinanceControls() {
        baptismStatusCombo.getItems().setAll(OrdinanceStatus.values());
        confirmationStatusCombo.getItems().setAll(OrdinanceStatus.values());
        initiatoryStatusCombo.getItems().setAll(OrdinanceStatus.values());
        endowmentStatusCombo.getItems().setAll(OrdinanceStatus.values());
        sealedToParentsStatusCombo.getItems().setAll(OrdinanceStatus.values());

        baptismStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
        confirmationStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
        initiatoryStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
        endowmentStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
        sealedToParentsStatusCombo.setValue(OrdinanceStatus.UNKNOWN);

        ordinanceNotesArea.setWrapText(true);
        ordinanceNotesArea.setPrefRowCount(6);
        ordinanceNotesArea.setPrefWidth(360);
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

    private void openSelectedWorkQueuePerson() {
        WorkQueueRow selected = workQueueTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getPersonId() == null) {
            showWarning("Please select a work queue row.");
            return;
        }

        selectPersonInTable(selected.getPersonId());
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
            Long selectedPersonId = null;
            Person currentlySelected = personTable.getSelectionModel().getSelectedItem();
            if (currentlySelected != null) {
                selectedPersonId = currentlySelected.getPersonId();
            }

            allPeople.setAll(personService.getAllPeople());
            applyPersonFilter();
            refreshWorkQueue();

            if (selectedPersonId != null) {
                reselectPerson(selectedPersonId);
            }

            if (personTable.getSelectionModel().getSelectedItem() == null && !personTable.getItems().isEmpty()) {
                personTable.getSelectionModel().selectFirst();
            }

            updateStatus();

            Person selected = personTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                clearSelectionDependentViews();
            } else {
                updateSummaryCard(selected);
                updateDetailArea(selected);
                refreshRelationshipTables(selected);
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
                    case "Baptism" -> baptismStatusCombo.setValue(row.getSuggestedStatus());
                    case "Confirmation" -> confirmationStatusCombo.setValue(row.getSuggestedStatus());
                    case "Initiatory" -> initiatoryStatusCombo.setValue(row.getSuggestedStatus());
                    case "Endowment" -> endowmentStatusCombo.setValue(row.getSuggestedStatus());
                    case "Sealed to Parents" -> sealedToParentsStatusCombo.setValue(row.getSuggestedStatus());
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
        clearPersonFilters();

        for (Person person : personTable.getItems()) {
            if (person.getPersonId().equals(personId)) {
                personTable.getSelectionModel().select(person);
                personTable.scrollTo(person);
                return;
            }
        }

        showWarning("That person is not available in the active table.");
    }

    private void reselectPerson(Long personId) {
        for (Person person : personTable.getItems()) {
            if (person.getPersonId().equals(personId)) {
                personTable.getSelectionModel().select(person);
                return;
            }
        }
    }

    private void addPerson() {
        PersonEditorDialog dialog = new PersonEditorDialog(null);
        Optional<Person> result = dialog.showAndWait();

        result.ifPresent(person -> {
            try {
                personService.savePerson(person);
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

        PersonEditorDialog dialog = new PersonEditorDialog(selected);
        Optional<Person> result = dialog.showAndWait();

        result.ifPresent(person -> {
            try {
                personService.savePerson(person);
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
        ParentChildDialog dialog = new ParentChildDialog(true, selected, candidates);
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
        ParentChildDialog dialog = new ParentChildDialog(true, selected, candidates, selectedLink);
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

        List<Person> candidates = getOtherPeople(selected);
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

        List<Person> candidates = getOtherPeople(selected);
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
        SpouseLinkDialog dialog = new SpouseLinkDialog(selected, candidates);
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
        SpouseLinkDialog dialog = new SpouseLinkDialog(selected, candidates, selectedLink);
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
            ordinancesHeaderLabel.setText("Select a person to edit ordinances.");
            baptismStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
            confirmationStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
            initiatoryStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
            endowmentStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
            sealedToParentsStatusCombo.setValue(OrdinanceStatus.UNKNOWN);
            ordinanceNotesArea.setText("");
            return;
        }

        try {
            ordinancesHeaderLabel.setText("Ordinances for " + person.getDisplayName());

            PersonOrdinanceStatus status = ordinanceService.getOrCreateForPerson(person.getPersonId());
            baptismStatusCombo.setValue(status.getBaptismStatus());
            confirmationStatusCombo.setValue(status.getConfirmationStatus());
            initiatoryStatusCombo.setValue(status.getInitiatoryStatus());
            endowmentStatusCombo.setValue(status.getEndowmentStatus());
            sealedToParentsStatusCombo.setValue(status.getSealedToParentsStatus());
            ordinanceNotesArea.setText(status.getOrdinanceNotes() == null ? "" : status.getOrdinanceNotes());
        } catch (Exception ex) {
            showError("Could not load ordinances.", ex);
        }
    }

    private void saveOrdinancesForSelectedPerson() {
        Person selected = personTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a person first.");
            return;
        }

        try {
            PersonOrdinanceStatus status = new PersonOrdinanceStatus();
            status.setPersonId(selected.getPersonId());
            status.setBaptismStatus(baptismStatusCombo.getValue());
            status.setConfirmationStatus(confirmationStatusCombo.getValue());
            status.setInitiatoryStatus(initiatoryStatusCombo.getValue());
            status.setEndowmentStatus(endowmentStatusCombo.getValue());
            status.setSealedToParentsStatus(sealedToParentsStatusCombo.getValue());
            status.setOrdinanceNotes(ordinanceNotesArea.getText());

            ordinanceService.save(status);
            refreshEligibilityTable(selected);
            refreshWorkQueue();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Saved");
            alert.setHeaderText(null);
            alert.setContentText("Ordinance status saved.");
            alert.showAndWait();
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
        updateSummaryCard(null);
        updateDetailArea(null);
        refreshRelationshipTables(null);
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

    private record TreePersonNode(long personId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}