package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.application.BackupService;
import family.balling.descendencytracker.application.AncestorLineSummaryService;
import family.balling.descendencytracker.application.OrdinanceEligibilityService;
import family.balling.descendencytracker.application.OrdinanceService;
import family.balling.descendencytracker.application.PersonService;
import family.balling.descendencytracker.application.RelationshipService;
import family.balling.descendencytracker.application.WorkQueueService;
import family.balling.descendencytracker.domain.AncestorLineSummary;
import family.balling.descendencytracker.domain.OrdinanceEligibilityRow;
import family.balling.descendencytracker.domain.ParentChildLink;
import family.balling.descendencytracker.domain.Person;
import family.balling.descendencytracker.domain.PersonOrdinanceStatus;
import family.balling.descendencytracker.domain.SpouseLink;
import family.balling.descendencytracker.domain.WorkQueueRow;
import family.balling.descendencytracker.domain.enums.OrdinanceStatus;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MainView extends BorderPane {
    private static final KeyCodeCombination ROOT_SHORTCUT =
            new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCodeCombination DETAILS_SHORTCUT =
            new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCodeCombination EDIT_SHORTCUT =
            new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCodeCombination DELETE_SHORTCUT =
            new KeyCodeCombination(KeyCode.DELETE, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCodeCombination HIDE_DETAILS_SHORTCUT =
            new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCodeCombination DISABLED_NEW_PERSON_SHORTCUT =
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
    private static final DateTimeFormatter BACKUP_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter SUMMARY_DATE_FORMAT =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private static final String DATA_ACTIONS_PLACEHOLDER = "Data Actions...";

    private final PersonService personService;
    private final RelationshipService relationshipService;
    private final OrdinanceService ordinanceService;
    private final AncestorLineSummaryService ancestorLineSummaryService;
    private final OrdinanceEligibilityService ordinanceEligibilityService;
    private final BackupService backupService;
    private final WorkQueueService workQueueService;

    private final ObservableList<Person> allPeople = FXCollections.observableArrayList();

    private final ObservableList<WorkQueueRow> allWorkQueueRows = FXCollections.observableArrayList();
    private final FilteredList<WorkQueueRow> filteredWorkQueueRows = new FilteredList<>(allWorkQueueRows, row -> true);
    private final SortedList<WorkQueueRow> sortedWorkQueueRows = new SortedList<>(filteredWorkQueueRows);
    private final TableView<Person> personTable;
    private final TableView<ParentChildLink> parentsTable = new TableView<>();
    private final TableView<ParentChildLink> childrenTable = new TableView<>();
    private final TableView<SpouseLink> spousesTable = new TableView<>();
    private final TableView<OrdinanceEligibilityRow> eligibilityTable = new TableView<>();
    private final TableView<WorkQueueRow> workQueueTable = new TableView<>();
    private final TabPane workspaceTabs = new TabPane();

    private final TreeView<TreePersonNode> pedigreeTree = new TreeView<>();
    private final TreeView<TreePersonNode> descendancyTree = new TreeView<>();

    private final TextArea detailArea = new TextArea();
    private final Label activeRootLabel = new Label("Current Root: (none)");
    private final Label selectedPersonNameLabel = new Label("None selected");
    private final Label selectedPersonFsPidLabel = new Label();
    private final Label relationshipToRootValue = new Label();
    private final Label statusLabel = new Label();
    private final Button shortcutsHelpButton = new Button("?");
    private final ComboBox<String> dataActionsComboBox = new ComboBox<>();

    private final TextField workQueueSearchField = new TextField();
    private final ComboBox<String> workQueueAttributeFilterCombo = new ComboBox<>();
    private final Label summaryPreferredNameValue = new Label();
    private final Label summaryFsPidValue = new Label();
    private final Label summaryRootValue = new Label();
    private final Label summaryParentsValue = new Label();
    private final Label summaryChildrenValue = new Label();
    private final Label summarySpousesValue = new Label();
    private final Label summaryAncestorsValue = new Label();
    private final Label summaryDescendantsValue = new Label();
    private final Label summaryLineStatusValue = new Label();
    private final Label summaryLineNextAvailableValue = new Label();
    private final Label summaryLineOpenValue = new Label();
    private final Label summaryLineSoonValue = new Label();
    private final Label summaryLineWaitingValue = new Label();
    private final Label summaryLineUnresolvedValue = new Label();
    private final Label summaryLineCompleteValue = new Label();
    private final Label summaryLineReasonValue = new Label();

    private final Map<Long, AncestorLineSummary> ancestorSummaryCache = new HashMap<>();
    private final Map<Long, List<AncestorLineSummary>> ancestorSummariesByPersonIdCache = new HashMap<>();
    private final Map<Long, OrdinanceTabData> ordinanceTabDataByPersonIdCache = new HashMap<>();
    private final PeopleNavigatorPane peopleNavigatorPane;
    private final OrdinancePane ordinancePane;
    private final AncestorLinesPane ancestorLinesPane;
    private final PauseTransition ancestorLinesWarmupDelay = new PauseTransition(Duration.millis(180));
    private final PauseTransition ordinancesWarmupDelay = new PauseTransition(Duration.millis(220));
    private Stage personDetailsStage;
    private Stage shortcutsStage;
    private boolean syncingTreeSelection;
    private boolean resettingDataActionsComboBox;
    private Long renderedTreeRootPersonId;

    public MainView(PersonService personService,
                    RelationshipService relationshipService,
                    OrdinanceService ordinanceService,
                    AncestorLineSummaryService ancestorLineSummaryService,
                    OrdinanceEligibilityService ordinanceEligibilityService,
                    BackupService backupService,
                    WorkQueueService workQueueService) {
        this.personService = personService;
        this.relationshipService = relationshipService;
        this.ordinanceService = ordinanceService;
        this.ancestorLineSummaryService = ancestorLineSummaryService;
        this.ordinanceEligibilityService = ordinanceEligibilityService;
        this.backupService = backupService;
        this.workQueueService = workQueueService;
        this.peopleNavigatorPane = new PeopleNavigatorPane(
                this::addPerson,
                this::editSelectedPerson,
                this::showPersonDetailsDialog,
                this::setSelectedAsRoot,
                this::handleSelectedPersonChanged,
                this::hasIncompleteTrackedOrdinances
        );
        this.personTable = peopleNavigatorPane.getPersonTable();
        this.ancestorLinesPane = new AncestorLinesPane(
                this::openSelectedAncestorLine,
                () -> refreshAncestorLineTable(getSelectedPerson()),
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
        ancestorLinesWarmupDelay.setOnFinished(event -> warmAncestorLinesForCurrentSelection());
        ordinancesWarmupDelay.setOnFinished(event -> warmOrdinancesForCurrentSelection());
        buildUi();
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.getAccelerators().remove(ROOT_SHORTCUT);
                oldScene.getAccelerators().remove(DETAILS_SHORTCUT);
                oldScene.getAccelerators().remove(EDIT_SHORTCUT);
                oldScene.getAccelerators().remove(DELETE_SHORTCUT);
                oldScene.getAccelerators().remove(HIDE_DETAILS_SHORTCUT);
                oldScene.getAccelerators().remove(DISABLED_NEW_PERSON_SHORTCUT);
            }
            if (newScene != null) {
                newScene.getAccelerators().put(ROOT_SHORTCUT, this::setSelectedAsRoot);
                newScene.getAccelerators().put(DETAILS_SHORTCUT, this::showPersonDetailsDialog);
                newScene.getAccelerators().put(EDIT_SHORTCUT, this::editSelectedOrRootPerson);
                newScene.getAccelerators().put(DELETE_SHORTCUT, this::deleteSelectedPerson);
                newScene.getAccelerators().put(HIDE_DETAILS_SHORTCUT, this::hidePersonDetailsDialog);
                newScene.getAccelerators().put(DISABLED_NEW_PERSON_SHORTCUT, () -> {
                });
            }
        });
        refreshPeople();
    }

    private void buildUi() {
        getStyleClass().add("app-root");
        setPadding(new Insets(10));

        activeRootLabel.getStyleClass().add("root-chip");
        setTop(activeRootLabel);

        configureParentsTable();
        configureChildrenTable();
        configureSpousesTable();
        configureEligibilityTable();
        configureWorkQueueControls();
        configureWorkQueueTable();
        configurePedigreeTree();
        configureDescendancyTree();

        sortedWorkQueueRows.comparatorProperty().bind(workQueueTable.comparatorProperty());
        workQueueTable.setItems(sortedWorkQueueRows);

        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefRowCount(18);
        buildPersonDetailsStage();
        buildShortcutsStage();

        workspaceTabs.getTabs().add(new Tab("Work Queue", buildWorkQueueTabContent()));
        workspaceTabs.getTabs().add(new Tab("Ordinances", buildOrdinancesTabContent()));
        workspaceTabs.getTabs().add(new Tab("Ancestor Lines", buildAncestorLinesTabContent()));
        workspaceTabs.getTabs().add(new Tab("Family", buildFamilyTabContent()));
        workspaceTabs.getStyleClass().add("workspace-tabs");
        workspaceTabs.getTabs().forEach(tab -> tab.setClosable(false));
        workspaceTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) ->
                refreshVisibleWorkspaceTab(getSelectedPerson())
        );
        selectedPersonNameLabel.getStyleClass().add("selected-person-name");
        selectedPersonFsPidLabel.getStyleClass().add("selected-person-meta");

        SplitPane treesSplit = new SplitPane(
                buildTreePane("Ancestry", pedigreeTree),
                buildTreePane("Descendancy", descendancyTree)
        );
        treesSplit.getStyleClass().add("workspace-split");
        treesSplit.setDividerPositions(0.50);

        SplitPane leftSplit = new SplitPane(peopleNavigatorPane.getContent(), treesSplit);
        leftSplit.setOrientation(Orientation.VERTICAL);
        leftSplit.getStyleClass().add("workspace-split");
        leftSplit.setDividerPositions(0.34);

        HBox selectedPersonHeader = buildSelectedPersonHeader();
        VBox workspacePane = new VBox(8, selectedPersonHeader, workspaceTabs);
        workspacePane.getStyleClass().add("workspace-shell");
        VBox.setVgrow(workspaceTabs, Priority.ALWAYS);

        SplitPane mainSplit = new SplitPane(leftSplit, workspacePane);
        mainSplit.getStyleClass().add("workspace-split");
        mainSplit.setDividerPositions(0.42);

        shortcutsHelpButton.setFocusTraversable(false);
        shortcutsHelpButton.setMinSize(28, 28);
        shortcutsHelpButton.setPrefSize(28, 28);
        shortcutsHelpButton.setOnAction(event -> showShortcutsDialog());
        shortcutsHelpButton.getStyleClass().addAll("icon-button", "utility-button");

        configureDataActionsComboBox();

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        HBox bottomBar = new HBox(8, statusLabel, bottomSpacer, dataActionsComboBox, shortcutsHelpButton);
        bottomBar.getStyleClass().add("status-bar");
        statusLabel.getStyleClass().add("status-text");

        setCenter(mainSplit);
        setBottom(bottomBar);
    }

    private HBox buildSelectedPersonHeader() {
        Label selectedPrefix = new Label("Selected");
        selectedPrefix.getStyleClass().add("selected-person-prefix");

        HBox header = new HBox(10, selectedPrefix, selectedPersonNameLabel, selectedPersonFsPidLabel);
        header.getStyleClass().add("selected-person-strip");
        return header;
    }

    private VBox buildWorkQueueTabContent() {
        Button clearQueueFiltersButton = new Button("Clear Queue Filters");

        clearQueueFiltersButton.setOnAction(event -> clearWorkQueueFilters());

        ToolBar toolbar = new ToolBar(
                new Label("Find"),
                workQueueSearchField,
                clearQueueFiltersButton,
                new Label("Flag"),
                workQueueAttributeFilterCombo
        );
        toolbar.getStyleClass().add("section-toolbar");

        VBox box = new VBox(8, toolbar, workQueueTable);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("panel-surface");
        workQueueTable.getStyleClass().add("compact-table");
        return box;
    }

    private void configureDataActionsComboBox() {
        dataActionsComboBox.getItems().setAll(
                DATA_ACTIONS_PLACEHOLDER,
                "Reload Data",
                "Refresh Queue",
                "Export Backup",
                "Import Backup"
        );
        dataActionsComboBox.setValue(DATA_ACTIONS_PLACEHOLDER);
        dataActionsComboBox.setPrefWidth(150);
        dataActionsComboBox.setVisibleRowCount(4);
        dataActionsComboBox.setFocusTraversable(false);
        dataActionsComboBox.getStyleClass().add("utility-combo");
        dataActionsComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (resettingDataActionsComboBox || newValue == null || DATA_ACTIONS_PLACEHOLDER.equals(newValue)) {
                return;
            }

            try {
                switch (newValue) {
                    case "Reload Data" -> refreshPeople();
                    case "Refresh Queue" -> refreshWorkQueue();
                    case "Export Backup" -> exportBackup();
                    case "Import Backup" -> importBackup();
                    default -> {
                    }
                }
            } finally {
                resetDataActionsComboBox();
            }
        });
    }

    private void resetDataActionsComboBox() {
        resettingDataActionsComboBox = true;
        try {
            dataActionsComboBox.setValue(DATA_ACTIONS_PLACEHOLDER);
        } finally {
            resettingDataActionsComboBox = false;
        }
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

    private String getThemeStylesheet() {
        return MainView.class
                .getResource("/family/balling/descendencytracker/ui/app-theme.css")
                .toExternalForm();
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
        applySelectedPersonToWorkspace(newSelection);
    }

    private void applySelectedPersonToWorkspace(Person newSelection) {
        if (personDetailsStage != null && personDetailsStage.isShowing()) {
            updateSummaryCard(newSelection);
            updateDetailArea(newSelection);
        }
        updateSelectedPersonHeader(newSelection);
        scheduleAncestorLinesWarmup(newSelection);
        scheduleOrdinancesWarmup(newSelection);
        refreshVisibleWorkspaceTab(newSelection);
        Person currentRoot = getCurrentRootPerson();
        refreshTreesIfRootChanged(currentRoot);
        updateStatus();
    }

    private void setCurrentRootPerson(Person person) {
        if (person == null || person.getPersonId() == null) {
            return;
        }

        try {
            personService.setRootPerson(person.getPersonId());
            markCurrentRootLocally(person.getPersonId());
            applySelectedPersonToWorkspace(person);
        } catch (Exception ex) {
            showError("Could not update the current root person.", ex);
        }
    }

    private Person getCurrentRootPerson() {
        for (Person candidate : allPeople) {
            if (candidate.isRoot()) {
                return candidate;
            }
        }

        return null;
    }

    private void markCurrentRootLocally(long rootPersonId) {
        for (Person candidate : allPeople) {
            if (candidate.getPersonId() == null) {
                continue;
            }
            candidate.setRoot(candidate.getPersonId() == rootPersonId);
        }

        personTable.refresh();
    }

    private void handleSelectedAncestorLineChanged(AncestorLineSummary newSelection) {
        refreshLineWorkbench(newSelection);
    }

    private void configureWorkQueueControls() {
        workQueueSearchField.setPromptText("Search queue...");
        workQueueSearchField.setPrefWidth(130);
        workQueueSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyWorkQueueFilter());

        workQueueAttributeFilterCombo.getItems().setAll(
                "All",
                "Open ordinances",
                "Reserved ordinances",
                "Has connected parents",
                "Has connected children",
                "Has connected spouses",
                "Confirmed no children",
                "Confirmed no spouse",
                "Missing spouse connection",
                "Missing child connection"
        );
        workQueueAttributeFilterCombo.setValue("All");
        workQueueAttributeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyWorkQueueFilter());
    }

    private void clearWorkQueueFilters() {
        workQueueSearchField.clear();
        workQueueAttributeFilterCombo.setValue("All");
    }

    private void applyWorkQueueFilter() {
        String searchText = workQueueSearchField.getText() == null ? "" : workQueueSearchField.getText().trim().toLowerCase();
        String attributeFilter = workQueueAttributeFilterCombo.getValue();

        filteredWorkQueueRows.setPredicate(row -> {
            if (row == null) {
                return false;
            }

            if (attributeFilter != null && !"All".equals(attributeFilter)) {
                boolean matchesAttribute = switch (attributeFilter) {
                    case "Open ordinances" -> row.hasOpenOrdinances();
                    case "Reserved ordinances" -> row.hasReservedOrdinances();
                    case "Has connected parents" -> row.hasConnectedParents();
                    case "Has connected children" -> row.hasConnectedChildren();
                    case "Has connected spouses" -> row.hasConnectedSpouses();
                    case "Confirmed no children" -> row.isConfirmedNoChildren();
                    case "Confirmed no spouse" -> row.isConfirmedNoSpouse();
                    case "Missing spouse connection" -> !row.hasConnectedSpouses() && !row.isConfirmedNoSpouse();
                    case "Missing child connection" -> !row.hasConnectedChildren() && !row.isConfirmedNoChildren();
                    default -> true;
                };

                if (!matchesAttribute) {
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

        HBox lineStatusLabelRow = new HBox(6, new Label("Line Status"), buildLineStatusHelpButton());
        grid.add(lineStatusLabelRow, 0, row);
        grid.add(summaryLineStatusValue, 1, row);
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
        grid.add(summaryLineCompleteValue, 1, row++);

        grid.add(new Label("Line Reason"), 0, row);
        grid.add(summaryLineReasonValue, 1, row, 3, 1);

        TitledPane titledPane = new TitledPane("Quick Summary", grid);
        titledPane.getStyleClass().add("section-pane");
        titledPane.setCollapsible(true);
        titledPane.setExpanded(false);
        return titledPane;
    }

    private TitledPane buildRelationshipPane() {
        Label helpLabel = new Label("Selection updates the workspace. Use Ctrl+R or right-click Set As Root to reroot.");
        helpLabel.setWrapText(true);
        helpLabel.getStyleClass().add("muted-text");

        VBox box = new VBox(
                6,
                new HBox(8, new Label("Relationship To Root"), relationshipToRootValue),
                helpLabel
        );
        box.setPadding(new Insets(8));
        box.getStyleClass().add("detail-card");

        TitledPane titledPane = new TitledPane("Root Context", box);
        titledPane.getStyleClass().add("section-pane");
        titledPane.setCollapsible(false);
        return titledPane;
    }

    private Button buildLineStatusHelpButton() {
        Button button = new Button("?");
        button.setFocusTraversable(false);
        button.setMinSize(24, 24);
        button.getStyleClass().addAll("icon-button", "utility-button");
        button.setOnAction(event -> showLineStatusHelp());
        return button;
    }

    private VBox buildTreePane(String title, TreeView<TreePersonNode> treeView) {
        VBox box = new VBox(treeView);
        box.setPadding(new Insets(6));
        box.getStyleClass().add("panel-surface");
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeView.getStyleClass().add("compact-tree");

        TitledPane titledPane = new TitledPane(title, box);
        titledPane.getStyleClass().add("section-pane");
        titledPane.setCollapsible(true);
        titledPane.setExpanded(true);
        titledPane.setTooltip(new Tooltip("Single-click selects. Right-click or Ctrl+R sets the current root."));

        VBox container = new VBox(titledPane);
        VBox.setVgrow(titledPane, Priority.ALWAYS);
        return container;
    }

    private void buildPersonDetailsStage() {
        TitledPane summaryPane = buildSummaryPane();
        TitledPane relationshipPane = buildRelationshipPane();
        Label detailHeading = new Label("Person Details");
        detailHeading.getStyleClass().add("section-title");

        VBox detailPane = new VBox(
                8,
                summaryPane,
                relationshipPane,
                detailHeading,
                detailArea
        );
        detailPane.setPadding(new Insets(10));
        detailPane.getStyleClass().addAll("app-root", "dialog-surface");
        detailArea.getStyleClass().add("details-area");
        VBox.setVgrow(detailArea, Priority.ALWAYS);

        personDetailsStage = new Stage();
        personDetailsStage.initModality(Modality.NONE);
        personDetailsStage.setTitle("Person Details");
        javafx.scene.Scene scene = new javafx.scene.Scene(detailPane, 520, 720);
        scene.getStylesheets().add(getThemeStylesheet());
        personDetailsStage.setScene(scene);
    }

    private void showPersonDetailsDialog() {
        if (personDetailsStage == null) {
            buildPersonDetailsStage();
        }

        if (getScene() != null && getScene().getWindow() != null && personDetailsStage.getOwner() == null) {
            personDetailsStage.initOwner(getScene().getWindow());
        }

        Person selected = getSelectedPerson();
        updateSummaryCard(selected);
        updateDetailArea(selected);
        personDetailsStage.show();
        personDetailsStage.toFront();
        personDetailsStage.requestFocus();
    }

    private void hidePersonDetailsDialog() {
        if (personDetailsStage != null && personDetailsStage.isShowing()) {
            personDetailsStage.hide();
        }
    }

    private void buildShortcutsStage() {
        Label shortcutsLabel = new Label("""
                Keyboard Shortcuts

                Ctrl+R  Set the selected person as the current root
                Ctrl+D  Show the person details window
                Ctrl+H  Hide the person details window
                Ctrl+E  Edit the selected person, or the root if none is selected
                Ctrl+Delete  Delete the selected person after confirmation
                """);
        shortcutsLabel.setWrapText(true);
        shortcutsLabel.getStyleClass().add("dialog-text");

        VBox content = new VBox(10, shortcutsLabel);
        content.setPadding(new Insets(12));
        content.getStyleClass().addAll("app-root", "dialog-surface");

        shortcutsStage = new Stage();
        shortcutsStage.initModality(Modality.NONE);
        shortcutsStage.setTitle("Keyboard Shortcuts");
        javafx.scene.Scene scene = new javafx.scene.Scene(content, 420, 220);
        scene.getStylesheets().add(getThemeStylesheet());
        shortcutsStage.setScene(scene);
    }

    private void showShortcutsDialog() {
        if (shortcutsStage == null) {
            buildShortcutsStage();
        }

        if (getScene() != null && getScene().getWindow() != null && shortcutsStage.getOwner() == null) {
            shortcutsStage.initOwner(getScene().getWindow());
        }

        shortcutsStage.show();
        shortcutsStage.toFront();
        shortcutsStage.requestFocus();
    }

    private void showLineStatusHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Line Status");
        alert.setHeaderText("Line status meanings");
        alert.setContentText(
                "OPEN_NOW: at least one tracked item in the line is actionable now.\n\n" +
                "OPENING_SOON: items in the line are expected to open soon.\n\n" +
                "WAITING_110: the line is mainly waiting on the 110-year rule.\n\n" +
                "UNRESOLVED: more information is needed before eligibility can be determined.\n\n" +
                "NOT_REVIEWED: one or more people in the line still need review.\n\n" +
                "COMPLETE_FOR_NOW: nothing in the line needs immediate attention."
        );
        alert.showAndWait();
    }

    private VBox buildOrdinancesTabContent() {
        Button refreshEligibilityButton = new Button("Refresh Eligibility");
        Button copySuggestedButton = new Button("Copy Suggested Person Buckets");

        refreshEligibilityButton.setOnAction(event -> refreshEligibilityTable(personTable.getSelectionModel().getSelectedItem()));
        copySuggestedButton.setOnAction(event -> copySuggestedPersonBucketsToOrdinanceEditor());

        ToolBar eligibilityToolbar = new ToolBar(refreshEligibilityButton, copySuggestedButton);
        eligibilityToolbar.getStyleClass().add("section-toolbar");
        VBox eligibilityBox = new VBox(8, eligibilityToolbar, eligibilityTable);
        eligibilityBox.getStyleClass().add("section-body");
        TitledPane eligibilityPane = new TitledPane("Eligibility", eligibilityBox);
        eligibilityPane.getStyleClass().add("section-pane");
        eligibilityPane.setCollapsible(false);
        eligibilityTable.getStyleClass().add("compact-table");

        VBox box = new VBox(8, ordinancePane.getContent(), eligibilityPane);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("panel-surface");
        return box;
    }

    private VBox buildAncestorLinesTabContent() {
        return ancestorLinesPane.getContent();
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

    private VBox buildFamilyTabContent() {
        TitledPane parentsPane = new TitledPane("Parents", buildParentsSection());
        parentsPane.getStyleClass().add("section-pane");
        parentsPane.setCollapsible(false);

        TitledPane childrenPane = new TitledPane("Children", buildChildrenSection());
        childrenPane.getStyleClass().add("section-pane");
        childrenPane.setCollapsible(false);

        TitledPane spousesPane = new TitledPane("Spouses", buildSpousesSection());
        spousesPane.getStyleClass().add("section-pane");
        spousesPane.setCollapsible(false);

        VBox box = new VBox(8, parentsPane, childrenPane, spousesPane);
        box.setPadding(new Insets(10));
        box.getStyleClass().add("panel-surface");
        return box;
    }

    private VBox buildParentsSection() {
        Button addParentButton = new Button("Add Parent");
        Button editParentButton = new Button("Edit Parent Link");
        Button removeParentButton = new Button("Remove Parent Link");

        addParentButton.setOnAction(event -> addParentToSelectedPerson());
        editParentButton.setOnAction(event -> editSelectedParentLink());
        removeParentButton.setOnAction(event -> removeSelectedParentLink());

        ToolBar toolbar = new ToolBar(addParentButton, editParentButton, removeParentButton);
        toolbar.getStyleClass().add("section-toolbar");
        parentsTable.getStyleClass().add("compact-table");
        VBox box = new VBox(8, toolbar, parentsTable);
        box.getStyleClass().add("section-body");
        return box;
    }

    private VBox buildChildrenSection() {
        Button addChildButton = new Button("Add Child");
        Button editChildButton = new Button("Edit Child Link");
        Button removeChildButton = new Button("Remove Child Link");

        addChildButton.setOnAction(event -> addChildToSelectedPerson());
        editChildButton.setOnAction(event -> editSelectedChildLink());
        removeChildButton.setOnAction(event -> removeSelectedChildLink());

        ToolBar toolbar = new ToolBar(addChildButton, editChildButton, removeChildButton);
        toolbar.getStyleClass().add("section-toolbar");
        childrenTable.getStyleClass().add("compact-table");
        VBox box = new VBox(8, toolbar, childrenTable);
        box.getStyleClass().add("section-body");
        return box;
    }

    private VBox buildSpousesSection() {
        Button addSpouseButton = new Button("Add Spouse");
        Button editSpouseButton = new Button("Edit Spouse Link");
        Button removeSpouseButton = new Button("Remove Spouse Link");

        addSpouseButton.setOnAction(event -> addSpouseToSelectedPerson());
        editSpouseButton.setOnAction(event -> editSelectedSpouseLink());
        removeSpouseButton.setOnAction(event -> removeSelectedSpouseLink());

        ToolBar toolbar = new ToolBar(addSpouseButton, editSpouseButton, removeSpouseButton);
        toolbar.getStyleClass().add("section-toolbar");
        spousesTable.getStyleClass().add("compact-table");
        VBox box = new VBox(8, toolbar, spousesTable);
        box.getStyleClass().add("section-body");
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
        eligibilityTable.setPlaceholder(new Label("No eligibility rows."));
        eligibilityTable.setFixedCellSize(23);
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
        workQueueTable.setPlaceholder(new Label("No work queue items."));
        workQueueTable.setFixedCellSize(23);

        workQueueTable.setRowFactory(table -> {
            TableRow<WorkQueueRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (row.isEmpty() || row.getItem() == null || row.getItem().getPersonId() == null) {
                    return;
                }

                Long personId = row.getItem().getPersonId();
                selectPersonInTable(personId);

                if (event.getClickCount() == 2) {
                    Person person = findPersonById(personId);
                    if (person != null) {
                        editPerson(person);
                    }
                }
            });
            return row;
        });
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

    private void configurePedigreeTree() {
        pedigreeTree.setShowRoot(true);
        pedigreeTree.setRoot(new TreeItem<>(TreePersonNode.placeholder("Select a root person to view ancestry.")));
        pedigreeTree.setFixedCellSize(24);
        pedigreeTree.setCellFactory(tree -> createTreeCell());
        attachTreeContextMenu(pedigreeTree);
        pedigreeTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (!syncingTreeSelection) {
                handleTreeSelectionChanged(newSelection);
            }
        });
    }

    private void configureDescendancyTree() {
        descendancyTree.setShowRoot(true);
        descendancyTree.setRoot(new TreeItem<>(TreePersonNode.placeholder("Select a root person to view descendancy.")));
        descendancyTree.setFixedCellSize(24);
        descendancyTree.setCellFactory(tree -> createTreeCell());
        attachTreeContextMenu(descendancyTree);
        descendancyTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (!syncingTreeSelection) {
                handleTreeSelectionChanged(newSelection);
            }
        });
    }

    private void attachTreeContextMenu(TreeView<TreePersonNode> treeView) {
        MenuItem editPersonItem = new MenuItem("Edit Person");
        editPersonItem.setOnAction(event -> editSelectedOrRootPerson());
        MenuItem personDetailsItem = new MenuItem("Person Details");
        personDetailsItem.setOnAction(event -> showPersonDetailsDialog());
        MenuItem setRootItem = new MenuItem("Set As Root");
        setRootItem.setOnAction(event -> {
            TreeItem<TreePersonNode> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem == null || selectedItem.getValue() == null || selectedItem.getValue().personId() <= 0) {
                return;
            }

            Person person = findPersonById(selectedItem.getValue().personId());
            if (person != null) {
                setCurrentRootPerson(person);
            }
        });
        treeView.setContextMenu(new ContextMenu(editPersonItem, personDetailsItem, setRootItem));
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
        parentsTable.setPlaceholder(new Label("No parents linked."));
        parentsTable.setFixedCellSize(23);

        parentsTable.setRowFactory(table -> {
            TableRow<ParentChildLink> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openSelectedParentPerson();
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
        childrenTable.setPlaceholder(new Label("No children linked."));
        childrenTable.setFixedCellSize(23);

        childrenTable.setRowFactory(table -> {
            TableRow<ParentChildLink> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openSelectedChildPerson();
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
        spousesTable.setPlaceholder(new Label("No spouses linked."));
        spousesTable.setFixedCellSize(23);

        spousesTable.setRowFactory(table -> {
            TableRow<SpouseLink> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openSelectedSpousePerson();
                }
            });
            return row;
        });
    }

    private void openSelectedParentPerson() {
        ParentChildLink selected = parentsTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getParentPersonId() == null) {
            showWarning("Please select a parent link.");
            return;
        }

        selectPersonInTable(selected.getParentPersonId());
    }

    private void openSelectedChildPerson() {
        ParentChildLink selected = childrenTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getChildPersonId() == null) {
            showWarning("Please select a child link.");
            return;
        }

        selectPersonInTable(selected.getChildPersonId());
    }

    private void openSelectedSpousePerson() {
        Person current = personTable.getSelectionModel().getSelectedItem();
        SpouseLink selected = spousesTable.getSelectionModel().getSelectedItem();
        if (current == null || selected == null) {
            showWarning("Please select a spouse link.");
            return;
        }

        Long spousePersonId = selected.getOtherPersonId(current.getPersonId());
        if (spousePersonId == null) {
            showWarning("That spouse could not be opened.");
            return;
        }

        selectPersonInTable(spousePersonId);
    }

    private void refreshPeople() {
        try {
            ancestorSummaryCache.clear();
            ancestorSummariesByPersonIdCache.clear();
            ordinanceTabDataByPersonIdCache.clear();
            renderedTreeRootPersonId = null;
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
            } else {
                personService.getRootPerson()
                        .map(Person::getPersonId)
                        .ifPresent(this::reselectPerson);
            }

            if (personTable.getSelectionModel().getSelectedItem() == null && !personTable.getItems().isEmpty()) {
                peopleNavigatorPane.selectFirstVisiblePerson();
            }

            updateStatus();

            Person selected = personTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                clearSelectionDependentViews();
            } else {
                if (personDetailsStage != null && personDetailsStage.isShowing()) {
                    updateSummaryCard(selected);
                    updateDetailArea(selected);
                }
                refreshVisibleWorkspaceTab(selected);
                refreshTreesIfRootChanged(getCurrentRootPerson());
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

    private void refreshVisibleWorkspaceTab(Person person) {
        Tab selectedTab = workspaceTabs.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return;
        }

        switch (selectedTab.getText()) {
            case "Ordinances" -> {
                ordinancesWarmupDelay.stop();
                updateOrdinanceEditor(person);
                refreshEligibilityTable(person);
            }
            case "Ancestor Lines" -> {
                ancestorLinesWarmupDelay.stop();
                refreshAncestorLineTable(person);
            }
            case "Family" -> refreshRelationshipTables(person);
            default -> {
            }
        }
    }

    private void scheduleAncestorLinesWarmup(Person person) {
        ancestorLinesWarmupDelay.stop();
        if (person == null || person.getPersonId() == null) {
            return;
        }

        Tab selectedTab = workspaceTabs.getSelectionModel().getSelectedItem();
        if (selectedTab != null && "Ancestor Lines".equals(selectedTab.getText())) {
            return;
        }

        if (ancestorSummariesByPersonIdCache.containsKey(person.getPersonId())) {
            return;
        }

        ancestorLinesWarmupDelay.playFromStart();
    }

    private void warmAncestorLinesForCurrentSelection() {
        Person selected = getSelectedPerson();
        if (selected == null || selected.getPersonId() == null) {
            return;
        }

        if (ancestorSummariesByPersonIdCache.containsKey(selected.getPersonId())) {
            return;
        }

        try {
            getAncestorSummariesForPerson(selected);
        } catch (Exception ex) {
            showError("Could not warm ancestor lines.", ex);
        }
    }

    private void scheduleOrdinancesWarmup(Person person) {
        ordinancesWarmupDelay.stop();
        if (person == null || person.getPersonId() == null) {
            return;
        }

        Tab selectedTab = workspaceTabs.getSelectionModel().getSelectedItem();
        if (selectedTab != null && "Ordinances".equals(selectedTab.getText())) {
            return;
        }

        if (ordinanceTabDataByPersonIdCache.containsKey(person.getPersonId())) {
            return;
        }

        ordinancesWarmupDelay.playFromStart();
    }

    private void warmOrdinancesForCurrentSelection() {
        Person selected = getSelectedPerson();
        if (selected == null || selected.getPersonId() == null) {
            return;
        }

        if (ordinanceTabDataByPersonIdCache.containsKey(selected.getPersonId())) {
            return;
        }

        try {
            getOrdinanceTabData(selected);
        } catch (Exception ex) {
            showError("Could not warm ordinances.", ex);
        }
    }

    private void refreshAncestorLineTable(Person person) {
        if (person == null) {
            ancestorLinesPane.clearAncestorLines();
            return;
        }

        try {
            Long selectedAncestorId = getSelectedAncestorLinePersonId();
            List<AncestorLineSummary> summaries = getAncestorSummariesForPerson(person);
            ancestorLinesPane.setAncestorLines(summaries);
            if (!reselectAncestorLine(selectedAncestorId)) {
                ancestorLinesPane.selectFirstAncestorLine();
            }
            if (ancestorLinesPane.getSelectedAncestorLine() == null) {
                ancestorLinesPane.clearWorkbench();
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
            eligibilityTable.setItems(FXCollections.observableArrayList(getOrdinanceTabData(person).eligibilityRows()));
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
            setTreeRoot(pedigreeTree, new TreeItem<>(TreePersonNode.placeholder("Select a root person to view ancestry.")));
            return;
        }

        TreeItem<TreePersonNode> root = createPersonTreeItem(person, TreeBranchType.ANCESTRY, Set.of());
        setTreeRoot(pedigreeTree, root);
    }

    private void refreshDescendancyTree(Person person) {
        if (person == null) {
            setTreeRoot(descendancyTree, new TreeItem<>(TreePersonNode.placeholder("Select a root person to view descendancy.")));
            return;
        }

        TreeItem<TreePersonNode> root = createPersonTreeItem(person, TreeBranchType.DESCENDANCY, Set.of());
        setTreeRoot(descendancyTree, root);
    }

    private void refreshTreesIfRootChanged(Person currentRoot) {
        Long currentRootId = currentRoot == null ? null : currentRoot.getPersonId();
        if (Objects.equals(renderedTreeRootPersonId, currentRootId)) {
            return;
        }

        refreshPedigreeTree(currentRoot);
        refreshDescendancyTree(currentRoot);
        renderedTreeRootPersonId = currentRootId;
    }

    private TreeItem<TreePersonNode> createPersonTreeItem(Person person, TreeBranchType branchType, Set<Long> path) {
        if (person == null || person.getPersonId() == null) {
            return new TreeItem<>(TreePersonNode.placeholder("(unknown person)"));
        }

        return createPersonTreeItem(person.getPersonId(), person.getDisplayName(), branchType, path);
    }

    private TreeItem<TreePersonNode> createPersonTreeItem(long personId, String fallbackName, TreeBranchType branchType, Set<Long> path) {
        if (path.contains(personId)) {
            return new TreeItem<>(TreePersonNode.placeholder("(cycle detected)"));
        }

        Person person = findPersonById(personId);
        String label = buildCompactTreeLabel(person, fallbackName);
        Set<Long> currentPath = new HashSet<>(path);
        currentPath.add(personId);

        TreeItem<TreePersonNode> item = new TreeItem<>(new TreePersonNode(
                personId,
                label,
                branchType,
                Set.copyOf(currentPath),
                branchType == TreeBranchType.DESCENDANCY ? resolveDescendantAttentionStatus(person) : DescendantAttentionStatus.NONE
        ));

        if (hasTreeChildren(personId, branchType)) {
            item.getChildren().add(new TreeItem<>(TreePersonNode.loading()));
            item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                if (isExpanded) {
                    ensureTreeChildrenLoaded(item);
                }
            });
        }

        return item;
    }

    private void ensureTreeChildrenLoaded(TreeItem<TreePersonNode> item) {
        if (item == null || item.getValue() == null || item.getChildren().isEmpty()) {
            return;
        }

        TreeItem<TreePersonNode> firstChild = item.getChildren().get(0);
        if (firstChild.getValue() == null || !firstChild.getValue().loadingNode()) {
            return;
        }

        TreePersonNode node = item.getValue();
        List<TreeItem<TreePersonNode>> children = switch (node.branchType()) {
            case ANCESTRY -> relationshipService.getParentsForPerson(node.personId()).stream()
                    .filter(link -> link.getParentPersonId() != null)
                    .map(link -> createPersonTreeItem(
                            link.getParentPersonId(),
                            link.getParentDisplayName(),
                            TreeBranchType.ANCESTRY,
                            node.path()
                    ))
                    .sorted(Comparator.comparing(child -> child.getValue().label(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
            case DESCENDANCY -> relationshipService.getChildrenForPerson(node.personId()).stream()
                    .filter(link -> link.getChildPersonId() != null)
                    .map(link -> createPersonTreeItem(
                            link.getChildPersonId(),
                            link.getChildDisplayName(),
                            TreeBranchType.DESCENDANCY,
                            node.path()
                    ))
                    .sorted(Comparator.comparing(child -> child.getValue().label(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        };

        item.getChildren().setAll(children);
    }

    private boolean hasTreeChildren(long personId, TreeBranchType branchType) {
        return switch (branchType) {
            case ANCESTRY -> !relationshipService.getParentsForPerson(personId).isEmpty();
            case DESCENDANCY -> !relationshipService.getChildrenForPerson(personId).isEmpty();
        };
    }

    private void setTreeRoot(TreeView<TreePersonNode> treeView, TreeItem<TreePersonNode> root) {
        syncingTreeSelection = true;
        try {
            treeView.setRoot(root);
            if (root != null) {
                root.setExpanded(true);
                ensureTreeChildrenLoaded(root);
                treeView.getSelectionModel().select(root);
            }
        } finally {
            syncingTreeSelection = false;
        }
    }

    private TreeCell<TreePersonNode> createTreeCell() {
        return new TreeCell<>() {
            @Override
            protected void updateItem(TreePersonNode item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTextFill(Color.BLACK);
                    return;
                }

                setText(item.label());
                setStyle(isSelected() ? "-fx-font-weight: bold;" : "");
                setTextFill(isSelected() ? Color.WHITE : resolveTreeTextColor(item));
            }
        };
    }

    private void handleTreeSelectionChanged(TreeItem<TreePersonNode> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null) {
            return;
        }

        long personId = selectedItem.getValue().personId();
        if (personId > 0) {
            selectPersonInTable(personId);
        }
    }

    private Color resolveTreeTextColor(TreePersonNode node) {
        if (node == null) {
            return Color.BLACK;
        }

        return switch (node.attentionStatus()) {
            case AVAILABLE -> Color.web("#1b7f3a");
            case POTENTIAL -> Color.web("#a16700");
            case NEEDS_INFO -> Color.web("#b42318");
            case COMPLETE, NONE -> Color.web("#1f1f1f");
        };
    }

    private String buildCompactTreeLabel(Person person, String fallbackName) {
        String label = person == null ? nullSafe(fallbackName) : nullSafe(person.getDisplayName());
        if (label.isBlank()) {
            label = "(Unnamed Person)";
        }

        if (person != null && person.getFsPid() != null && !person.getFsPid().isBlank()) {
            label += " [" + person.getFsPid() + "]";
        }
        return label;
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

        List<OrdinanceEligibilityRow> rows = getOrdinanceTabData(person).eligibilityRows();

        for (OrdinanceEligibilityRow row : rows) {
            if (row.getRecordedStatus() != OrdinanceStatus.COMPLETE
                    && row.getRecordedStatus() != OrdinanceStatus.NOT_APPLICABLE) {
                return true;
            }
        }

        return false;
    }

    private DescendantAttentionStatus resolveDescendantAttentionStatus(Person person) {
        if (person == null || person.getPersonId() == null) {
            return DescendantAttentionStatus.NONE;
        }

        List<OrdinanceEligibilityRow> rows = getOrdinanceTabData(person).eligibilityRows();
        boolean hasPotential = false;
        boolean allComplete = true;

        for (OrdinanceEligibilityRow row : rows) {
            OrdinanceStatus status = row.getSuggestedStatus();
            if (status == OrdinanceStatus.OPEN) {
                return DescendantAttentionStatus.AVAILABLE;
            }
            if (status == OrdinanceStatus.UNKNOWN) {
                return DescendantAttentionStatus.NEEDS_INFO;
            }
            if (status == OrdinanceStatus.SOON_1Y
                    || status == OrdinanceStatus.SOON_2Y
                    || status == OrdinanceStatus.SOON_5Y
                    || status == OrdinanceStatus.SOON_10Y
                    || status == OrdinanceStatus.BLOCKED_110) {
                hasPotential = true;
            }
            if (status != OrdinanceStatus.COMPLETE && status != OrdinanceStatus.NOT_APPLICABLE) {
                allComplete = false;
            }
        }

        if (hasPotential) {
            return DescendantAttentionStatus.POTENTIAL;
        }

        return allComplete ? DescendantAttentionStatus.COMPLETE : DescendantAttentionStatus.NONE;
    }

    private List<OrdinanceEligibilityRow> buildEligibilityRows(Person person) {
        return getOrdinanceTabData(person).eligibilityRows();
    }

    private List<AncestorLineSummary> getAncestorSummariesForPerson(Person person) {
        if (person == null || person.getPersonId() == null) {
            return List.of();
        }

        return ancestorSummariesByPersonIdCache.computeIfAbsent(person.getPersonId(), ignored -> {
            return ancestorLineSummaryService.buildSummaries(
                    collectAncestorsForPerson(person),
                    allPeople
            );
        });
    }

    private OrdinanceTabData getOrdinanceTabData(Person person) {
        if (person == null || person.getPersonId() == null) {
            return OrdinanceTabData.empty();
        }

        return ordinanceTabDataByPersonIdCache.computeIfAbsent(person.getPersonId(), ignored -> {
            PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(person.getPersonId());
            List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
            List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());

            List<OrdinanceEligibilityRow> eligibilityRows = ordinanceEligibilityService.buildDashboard(
                    person,
                    ordinanceStatus,
                    parents,
                    spouses,
                    allPeople
            );

            return new OrdinanceTabData(
                    ordinanceStatus,
                    List.copyOf(spouses),
                    List.copyOf(eligibilityRows)
            );
        });
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
        editPerson(selected);
    }

    private void editSelectedOrRootPerson() {
        Person target = personTable.getSelectionModel().getSelectedItem();
        if (target == null) {
            target = getCurrentRootPerson();
        }

        if (target == null) {
            showWarning("There is no selected person or current root person to edit.");
            return;
        }

        editPerson(target);
    }

    private void editPerson(Person person) {
        if (person == null || person.getPersonId() == null) {
            showWarning("Please select a person to edit.");
            return;
        }

        PersonEditorDialog dialog = new PersonEditorDialog(
                person,
                ordinanceService.getOrCreateForPerson(person.getPersonId())
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

        setCurrentRootPerson(selected);
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
                buildSpouseCandidatesByPersonId(allPeople),
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
                    ensureMirroredParentLinkExists(
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
                buildSpouseCandidatesByPersonId(allPeople),
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
                    ensureMirroredParentLinkExists(
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
            invalidateDerivedCaches();
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
        ParentChildDialog dialog = new ParentChildDialog(
                false,
                selected,
                candidates,
                buildSpouseCandidatesByPersonId(allPeople),
                null
        );
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

                if (input.getMirrorSpousePersonId() != null) {
                    ensureMirroredParentLinkExists(
                            childPersonId,
                            input.getMirrorSpousePersonId(),
                            input.getChildOrder(),
                            input.getNotes()
                    );
                }

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
            invalidateDerivedCaches();
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
                        input.isSealingReserved(),
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
                        input.isSealingReserved(),
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
            invalidateDerivedCaches();
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
            OrdinanceTabData data = getOrdinanceTabData(person);
            ordinancePane.populate(person, data.ordinanceStatus(), data.spouseLinks());
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
            invalidateDerivedCaches();
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
            invalidateDerivedCaches();
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
                spouseLink.isSealedToSpouseReserved(),
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
            Map<Long, OrdinanceStatusChoice> spouseSelections = ordinancePane.getSpouseSealingSelections();
            for (SpouseLink spouseLink : relationshipService.getSpousesForPerson(selected.getPersonId())) {
                OrdinanceStatusChoice spouseSelection = spouseSelections.get(spouseLink.getSpouseLinkId());
                if (spouseSelection == null) {
                    continue;
                }

                relationshipService.updateSpouseLink(
                        spouseLink.getSpouseLinkId(),
                        spouseLink.getPersonAId(),
                        spouseLink.getPersonBId(),
                        spouseLink.getMarriageDateText(),
                        spouseLink.getMarriageNotes(),
                        spouseSelection.status(),
                        spouseSelection.isReserved(),
                        spouseLink.getSealingStatusDate(),
                        spouseLink.getSealingNotes()
                );
            }

            invalidateDerivedCaches();
            updateOrdinanceEditor(selected);
            refreshEligibilityTable(selected);
            refreshWorkQueue();
        } catch (Exception ex) {
            showError("Could not save ordinances.", ex);
        }
    }

    private void invalidateDerivedCaches() {
        ancestorSummaryCache.clear();
        ancestorSummariesByPersonIdCache.clear();
        ordinanceTabDataByPersonIdCache.clear();
        ancestorLinesWarmupDelay.stop();
        ordinancesWarmupDelay.stop();
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

    private void ensureMirroredParentLinkExists(long childPersonId, long spousePersonId, Integer childOrder, String notes) {
        for (ParentChildLink link : relationshipService.getParentsForPerson(childPersonId)) {
            if (link.getParentPersonId() != null && link.getParentPersonId().equals(spousePersonId)) {
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
            ensureMirroredParentLinkExists(childPersonId, spousePersonId, null, null);
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
            summaryLineStatusValue.setText("");
            summaryLineNextAvailableValue.setText("");
            summaryLineOpenValue.setText("");
            summaryLineSoonValue.setText("");
            summaryLineWaitingValue.setText("");
            summaryLineUnresolvedValue.setText("");
            summaryLineCompleteValue.setText("");
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
            summaryLineStatusValue.setText(summary == null || summary.getBadgeStatus() == null ? "" : summary.getBadgeStatus().name());
            summaryLineNextAvailableValue.setText(summary == null ? "" : formatSummaryDate(summary.getNextAvailableDate()));
            summaryLineOpenValue.setText(summary == null ? "" : String.valueOf(summary.getOpenCount()));
            summaryLineSoonValue.setText(summary == null ? "" : String.valueOf(summary.getOpeningSoonCount()));
            summaryLineWaitingValue.setText(summary == null ? "" : String.valueOf(summary.getWaiting110Count()));
            summaryLineUnresolvedValue.setText(summary == null ? "" : String.valueOf(summary.getUnresolvedCount()));
            summaryLineCompleteValue.setText(summary == null ? "" : String.valueOf(summary.getCompleteCount()));
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
            relationshipToRootValue.setText("");
            detailArea.setText("");
            return;
        }

        Person currentRoot = getCurrentRootPerson();
        if (currentRoot == null || currentRoot.getPersonId() == null) {
            relationshipToRootValue.setText("");
        } else if (currentRoot.getPersonId().equals(person.getPersonId())) {
            relationshipToRootValue.setText("Self (active root)");
        } else {
            relationshipToRootValue.setText("Selected person; active root is " + nullSafe(currentRoot.getDisplayName()));
        }

        StringBuilder builder = new StringBuilder();
        List<ParentChildLink> parents = relationshipService.getParentsForPerson(person.getPersonId());
        List<ParentChildLink> children = relationshipService.getChildrenForPerson(person.getPersonId());
        List<SpouseLink> spouses = relationshipService.getSpousesForPerson(person.getPersonId());
        PersonOrdinanceStatus ordinanceStatus = ordinanceService.getOrCreateForPerson(person.getPersonId());
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
        builder.append("Connected Parents: ").append(parents.isEmpty() ? "No" : "Yes (" + parents.size() + ")").append('\n');
        builder.append("Connected Children: ").append(children.isEmpty() ? "No" : "Yes (" + children.size() + ")").append('\n');
        builder.append("Connected Spouses: ").append(spouses.isEmpty() ? "No" : "Yes (" + spouses.size() + ")").append('\n');
        builder.append("Confirmed No Children: ").append(person.isConfirmedNoChildren() ? "Yes" : "No").append('\n');
        builder.append("Confirmed No Spouse: ").append(person.isConfirmedNoSpouse() ? "Yes" : "No").append('\n');
        builder.append("Reserved Ordinances: ").append(buildReservedOrdinanceSummary(person, ordinanceStatus, spouses)).append('\n');
        builder.append("Created At: ").append(nullSafe(person.getCreatedAt())).append('\n');
        builder.append("Updated At: ").append(nullSafe(person.getUpdatedAt())).append('\n');
        builder.append('\n');
        builder.append("Notes:\n").append(nullSafe(person.getNotes()));

        detailArea.setText(builder.toString());
    }

    private String buildReservedOrdinanceSummary(Person person, PersonOrdinanceStatus ordinanceStatus, List<SpouseLink> spouses) {
        List<String> reserved = FXCollections.observableArrayList();
        if (ordinanceStatus.isBaptismReserved()) {
            reserved.add("Baptism");
        }
        if (ordinanceStatus.isConfirmationReserved()) {
            reserved.add("Confirmation");
        }
        if (ordinanceStatus.isInitiatoryReserved()) {
            reserved.add("Initiatory");
        }
        if (ordinanceStatus.isEndowmentReserved()) {
            reserved.add("Endowment");
        }
        if (ordinanceStatus.isSealedToParentsReserved()) {
            reserved.add("Sealed to Parents");
        }
        for (SpouseLink spouse : spouses) {
            if (spouse.isSealedToSpouseReserved()) {
                reserved.add("Sealed to Spouse: " + nullSafe(spouse.getOtherPersonDisplayName(person.getPersonId())));
            }
        }
        return reserved.isEmpty() ? "None" : String.join(", ", reserved);
    }

    private void updateStatus() {
        int visibleCount = personTable.getItems().size();
        int totalCount = allPeople.size();
        String rootName = Optional.ofNullable(getCurrentRootPerson())
                .map(Person::getDisplayName)
                .orElse("(none)");

        activeRootLabel.setText("Current Root: " + rootName);
        statusLabel.setText("Visible people: " + visibleCount + " / " + totalCount + "    Active root: " + rootName);
    }

    private void updateSelectedPersonHeader(Person person) {
        if (person == null) {
            selectedPersonNameLabel.setText("None selected");
            selectedPersonFsPidLabel.setText("");
            selectedPersonFsPidLabel.setManaged(false);
            selectedPersonFsPidLabel.setVisible(false);
            return;
        }

        selectedPersonNameLabel.setText(nullSafe(person.getDisplayName()).isBlank()
                ? "(Unnamed Person)"
                : nullSafe(person.getDisplayName()));

        String fsPid = nullSafe(person.getFsPid()).trim();
        boolean hasFsPid = !fsPid.isBlank();
        selectedPersonFsPidLabel.setText(hasFsPid ? "FS PID " + fsPid : "");
        selectedPersonFsPidLabel.setManaged(hasFsPid);
        selectedPersonFsPidLabel.setVisible(hasFsPid);
    }

    private void clearSelectionDependentViews() {
        ancestorSummaryCache.clear();
        updateSummaryCard(null);
        updateDetailArea(null);
        refreshRelationshipTables(null);
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
                ignored -> ancestorLineSummaryService.buildSummary(person, allPeople)
        );
    }

    private String formatSummaryDate(LocalDate date) {
        return date == null ? "" : SUMMARY_DATE_FORMAT.format(date);
    }

    private enum TreeBranchType {
        ANCESTRY,
        DESCENDANCY
    }

    private enum DescendantAttentionStatus {
        AVAILABLE,
        POTENTIAL,
        NEEDS_INFO,
        COMPLETE,
        NONE
    }

    private record OrdinanceTabData(
            PersonOrdinanceStatus ordinanceStatus,
            List<SpouseLink> spouseLinks,
            List<OrdinanceEligibilityRow> eligibilityRows
    ) {
        private static OrdinanceTabData empty() {
            return new OrdinanceTabData(new PersonOrdinanceStatus(), List.of(), List.of());
        }
    }

    private record TreePersonNode(
            long personId,
            String label,
            TreeBranchType branchType,
            Set<Long> path,
            DescendantAttentionStatus attentionStatus
    ) {
        private static TreePersonNode placeholder(String label) {
            return new TreePersonNode(-1L, label, TreeBranchType.ANCESTRY, Set.of(), DescendantAttentionStatus.NONE);
        }

        private static TreePersonNode loading() {
            return new TreePersonNode(-1L, "Loading...", TreeBranchType.ANCESTRY, Set.of(), DescendantAttentionStatus.NONE);
        }

        private boolean loadingNode() {
            return personId < 0 && "Loading...".equals(label);
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
