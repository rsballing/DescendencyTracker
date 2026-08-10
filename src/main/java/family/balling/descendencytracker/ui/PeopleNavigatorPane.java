package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.Person;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class PeopleNavigatorPane {
    private final ObservableList<Person> allPeople = FXCollections.observableArrayList();
    private final FilteredList<Person> filteredPeople = new FilteredList<>(allPeople, person -> true);
    private final SortedList<Person> sortedPeople = new SortedList<>(filteredPeople);

    private final TableView<Person> personTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> peopleFilterCombo = new ComboBox<>();
    private final ComboBox<Person> pinnedPeopleCombo = new ComboBox<>();

    private final Predicate<Person> hasIncompleteTrackedOrdinances;
    private final Consumer<Person> onSelectionChanged;
    private final VBox content;

    PeopleNavigatorPane(
            Runnable onAddPerson,
            Consumer<Person> onOpenPinnedPerson,
            Runnable onEditSelected,
            Runnable onShowSelectedPersonDetails,
            Runnable onSetSelectedAsRoot,
            Consumer<Person> onSelectionChanged,
            Predicate<Person> hasIncompleteTrackedOrdinances
    ) {
        this.hasIncompleteTrackedOrdinances = hasIncompleteTrackedOrdinances;
        this.onSelectionChanged = onSelectionChanged;

        configurePersonSearchBar();
        configurePersonTable(onEditSelected, onShowSelectedPersonDetails, onSetSelectedAsRoot);

        sortedPeople.comparatorProperty().bind(personTable.comparatorProperty());
        personTable.setItems(sortedPeople);
        personTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                this.onSelectionChanged.accept(newSelection)
        );

        Label heading = new Label("People");
        heading.getStyleClass().add("panel-title");
        Button addPersonButton = new Button("New Person");
        addPersonButton.setFocusTraversable(false);
        addPersonButton.setMnemonicParsing(false);
        addPersonButton.getStyleClass().addAll("utility-button", "section-add-button");
        addPersonButton.setTooltip(new Tooltip("Add Person"));
        addPersonButton.setOnAction(event -> onAddPerson.run());
        configurePinnedPeopleCombo(onOpenPinnedPerson);
        HBox headingRow = new HBox(6, heading, addPersonButton, pinnedPeopleCombo);
        headingRow.getStyleClass().add("panel-header-row");

        content = new VBox(6, headingRow, buildFilterToolBar(), personTable);
        content.setPadding(new Insets(8));
        content.getStyleClass().add("panel-surface");
        VBox.setVgrow(personTable, Priority.ALWAYS);
    }

    VBox getContent() {
        return content;
    }

    Person getSelectedPerson() {
        return personTable.getSelectionModel().getSelectedItem();
    }

    TableView<Person> getPersonTable() {
        return personTable;
    }

    void setPeople(List<Person> people) {
        allPeople.setAll(people);
        applyPersonFilter();
    }

    void clearFilters() {
        searchField.clear();
        peopleFilterCombo.setValue("All People");
    }

    void setPinnedPeople(List<Person> pinnedPeople) {
        Person selected = pinnedPeopleCombo.getValue();
        pinnedPeopleCombo.getItems().setAll(pinnedPeople);
        if (selected == null) {
            pinnedPeopleCombo.setValue(null);
            return;
        }

        for (Person person : pinnedPeople) {
            if (selected.getPersonId() != null && selected.getPersonId().equals(person.getPersonId())) {
                pinnedPeopleCombo.setValue(person);
                return;
            }
        }
        pinnedPeopleCombo.setValue(null);
    }

    boolean selectPersonById(long personId, boolean clearFiltersFirst) {
        if (clearFiltersFirst) {
            clearFilters();
        }
        return selectVisiblePersonInTable(personId);
    }

    void reselectPerson(Long personId) {
        if (personId == null) {
            return;
        }

        for (Person person : personTable.getItems()) {
            if (personId.equals(person.getPersonId())) {
                personTable.getSelectionModel().select(person);
                personTable.scrollTo(person);
                return;
            }
        }
    }

    void selectFirstVisiblePerson() {
        if (!personTable.getItems().isEmpty()) {
            personTable.getSelectionModel().selectFirst();
        }
    }

    int getVisibleCount() {
        return personTable.getItems().size();
    }

    private void configurePersonSearchBar() {
        searchField.setPromptText("Filter people by name, PID, year, notes, sex, or reviewed status...");
        searchField.setPrefWidth(160);
        peopleFilterCombo.getItems().setAll(
                "All People",
                "Born 110+ years ago",
                "110+ with incomplete ordinances"
        );
        peopleFilterCombo.setValue("All People");
        peopleFilterCombo.setPrefWidth(180);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
        peopleFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
    }

    private ToolBar buildFilterToolBar() {
        Button clearFiltersButton = new Button("Clear");
        clearFiltersButton.setOnAction(event -> clearFilters());

        ToolBar toolBar = new ToolBar(
                new Label("Find"),
                searchField,
                clearFiltersButton,
                peopleFilterCombo
        );
        toolBar.getStyleClass().add("section-toolbar");
        return toolBar;
    }

    private void configurePinnedPeopleCombo(Consumer<Person> onOpenPinnedPerson) {
        pinnedPeopleCombo.setPromptText("Pinned");
        pinnedPeopleCombo.setPrefWidth(220);
        pinnedPeopleCombo.setVisibleRowCount(12);
        pinnedPeopleCombo.setFocusTraversable(false);
        pinnedPeopleCombo.getStyleClass().add("utility-combo");
        pinnedPeopleCombo.setButtonCell(createPinnedPersonCell());
        pinnedPeopleCombo.setCellFactory(list -> createPinnedPersonCell());
        pinnedPeopleCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            onOpenPinnedPerson.accept(newValue);
            pinnedPeopleCombo.setValue(null);
        });
    }

    private ListCell<Person> createPinnedPersonCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(getListView() == null ? "Pinned" : null);
                    return;
                }
                setText(buildPinnedPersonLabel(item));
            }
        };
    }

    private String buildPinnedPersonLabel(Person person) {
        String displayName = nullSafe(person == null ? null : person.getDisplayName());
        if (displayName.isBlank()) {
            displayName = "(Unnamed Person)";
        }
        String fsPid = nullSafe(person == null ? null : person.getFsPid()).trim();
        return fsPid.isBlank() ? displayName : displayName + " [" + fsPid + "]";
    }

    private void applyPersonFilter() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String activeFilter = peopleFilterCombo.getValue();
        boolean bornMoreThan110 = "Born 110+ years ago".equals(activeFilter)
                || "110+ with incomplete ordinances".equals(activeFilter);
        boolean born110WithIncomplete = "110+ with incomplete ordinances".equals(activeFilter);
        String[] tokens = searchText.isBlank() ? new String[0] : searchText.split("\\s+");

        filteredPeople.setPredicate(person -> {
            if (person == null) {
                return false;
            }

            if (bornMoreThan110 && !isBornMoreThan110YearsAgo(person)) {
                return false;
            }

            if (born110WithIncomplete && !hasIncompleteTrackedOrdinances.test(person)) {
                return false;
            }

            if (searchText.isBlank()) {
                return true;
            }

            String haystack = PersonSelectionSupport.buildSearchText(person);
            for (String token : tokens) {
                if (!haystack.contains(token)) {
                    return false;
                }
            }
            return true;
        });

        reconcileSelectionAfterFilter();
    }

    private void reconcileSelectionAfterFilter() {
        Person selected = personTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            if (!personTable.getItems().isEmpty()) {
                personTable.getSelectionModel().selectFirst();
            } else {
                onSelectionChanged.accept(null);
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
                onSelectionChanged.accept(null);
            }
        }
    }

    private void configurePersonTable(
            Runnable onEditSelected,
            Runnable onShowSelectedPersonDetails,
            Runnable onSetSelectedAsRoot
    ) {
        TableColumn<Person, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getDisplayName()))
        );
        nameColumn.setPrefWidth(240);

        TableColumn<Person, String> birthColumn = new TableColumn<>("Birth");
        birthColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getBirthDateText()))
        );
        birthColumn.setPrefWidth(110);

        TableColumn<Person, String> deathColumn = new TableColumn<>("Death");
        deathColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getDeathDateText()))
        );
        deathColumn.setPrefWidth(110);

        TableColumn<Person, String> fsPidColumn = new TableColumn<>("FS PID");
        fsPidColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getFsPid()))
        );
        fsPidColumn.setPrefWidth(120);

        personTable.getColumns().addAll(nameColumn, birthColumn, deathColumn, fsPidColumn);
        personTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        personTable.setPlaceholder(new Label("No matching people."));
        personTable.setFixedCellSize(28);
        personTable.getStyleClass().add("compact-table");

        MenuItem editPersonItem = new MenuItem("Edit Person");
        editPersonItem.setOnAction(event -> onEditSelected.run());
        MenuItem personDetailsItem = new MenuItem("Person Details");
        personDetailsItem.setOnAction(event -> onShowSelectedPersonDetails.run());
        MenuItem setRootItem = new MenuItem("Set As Root");
        setRootItem.setOnAction(event -> onSetSelectedAsRoot.run());
        ContextMenu tableContextMenu = new ContextMenu(editPersonItem, personDetailsItem, setRootItem);
        personTable.setContextMenu(tableContextMenu);

        personTable.setRowFactory(table -> {
            TableRow<Person> row = new TableRow<>();
            MenuItem rowEditItem = new MenuItem("Edit Person");
            rowEditItem.setOnAction(event -> {
                if (!row.isEmpty()) {
                    personTable.getSelectionModel().select(row.getItem());
                    onEditSelected.run();
                }
            });
            MenuItem rowDetailsItem = new MenuItem("Person Details");
            rowDetailsItem.setOnAction(event -> {
                if (!row.isEmpty()) {
                    personTable.getSelectionModel().select(row.getItem());
                    onShowSelectedPersonDetails.run();
                }
            });
            MenuItem rowSetRootItem = new MenuItem("Set As Root");
            rowSetRootItem.setOnAction(event -> {
                if (!row.isEmpty()) {
                    personTable.getSelectionModel().select(row.getItem());
                    onSetSelectedAsRoot.run();
                }
            });
            ContextMenu rowContextMenu = new ContextMenu(rowEditItem, rowDetailsItem, rowSetRootItem);
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowContextMenu)
            );
            row.itemProperty().addListener((obs, oldPerson, newPerson) -> updateRowStyle(row, newPerson));
            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> updateRowStyle(row, row.getItem()));
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEditSelected.run();
                }
            });
            return row;
        });
    }

    private void updateRowStyle(TableRow<Person> row, Person person) {
        if (row == null || row.isEmpty() || person == null) {
            row.setStyle("");
            return;
        }

        if (person.isRoot()) {
            row.setStyle("-fx-font-weight: bold; -fx-font-style: italic;");
            return;
        }

        row.setStyle("");
    }

    private boolean selectVisiblePersonInTable(long personId) {
        for (Person person : personTable.getItems()) {
            if (person.getPersonId().equals(personId)) {
                personTable.getSelectionModel().select(person);
                personTable.scrollTo(person);
                return true;
            }
        }
        return false;
    }

    private boolean isBornMoreThan110YearsAgo(Person person) {
        Integer birthYear = extractYear(person == null ? null : person.getBirthDateText());
        if (birthYear == null) {
            return false;
        }

        return birthYear <= LocalDate.now().minusYears(110).getYear();
    }

    private Integer extractYear(String dateText) {
        if (dateText == null) {
            return null;
        }

        String trimmed = dateText.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String[] tokens = trimmed.split("[^0-9]+");
        for (int index = tokens.length - 1; index >= 0; index--) {
            String token = tokens[index];
            if (token.length() == 4) {
                try {
                    return Integer.parseInt(token);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
