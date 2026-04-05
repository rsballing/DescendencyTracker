package family.balling.descendencytracker.ui;

import family.balling.descendencytracker.domain.Person;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class PeopleNavigatorPane {
    private final ObservableList<Person> allPeople = FXCollections.observableArrayList();
    private final FilteredList<Person> filteredPeople = new FilteredList<>(allPeople, person -> true);
    private final SortedList<Person> sortedPeople = new SortedList<>(filteredPeople);

    private final TableView<Person> personTable = new TableView<>();
    private final TreeView<BirthYearTreeNode> peopleBirthYearTree = new TreeView<>();
    private final TextField searchField = new TextField();
    private final CheckBox rootOnlyCheckBox = new CheckBox("Only root");
    private final CheckBox bornMoreThan110CheckBox = new CheckBox("Born 110+ years ago");
    private final CheckBox born110WithIncompleteCheckBox = new CheckBox("110+ with incomplete ordinances");

    private final Predicate<Person> hasIncompleteTrackedOrdinances;
    private final Consumer<Person> onSelectionChanged;
    private final ToolBar filterToolBar;
    private final TabPane content;
    private boolean syncingPeopleViewSelection;

    PeopleNavigatorPane(
            Runnable onEditSelected,
            Consumer<Person> onSelectionChanged,
            Predicate<Person> hasIncompleteTrackedOrdinances
    ) {
        this.hasIncompleteTrackedOrdinances = hasIncompleteTrackedOrdinances;
        this.onSelectionChanged = onSelectionChanged;

        configurePersonSearchBar();
        configurePersonTable(onEditSelected);
        configurePeopleBirthYearTree();

        sortedPeople.comparatorProperty().bind(personTable.comparatorProperty());
        personTable.setItems(sortedPeople);
        personTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (!syncingPeopleViewSelection) {
                selectPersonInBirthYearTree(newSelection);
            }
            this.onSelectionChanged.accept(newSelection);
        });

        filterToolBar = buildFilterToolBar();

        content = new TabPane();
        content.getTabs().add(new Tab("Table", personTable));
        content.getTabs().add(new Tab("Birth Years", peopleBirthYearTree));
        content.getTabs().forEach(tab -> tab.setClosable(false));
    }

    ToolBar getFilterToolBar() {
        return filterToolBar;
    }

    TabPane getContent() {
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
        rootOnlyCheckBox.setSelected(false);
        bornMoreThan110CheckBox.setSelected(false);
        born110WithIncompleteCheckBox.setSelected(false);
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
        searchField.setPromptText("Search by name, PID, dates, notes, sex, or reviewed status...");
        searchField.setPrefWidth(320);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
        rootOnlyCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
        bornMoreThan110CheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
        born110WithIncompleteCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> applyPersonFilter());
    }

    private ToolBar buildFilterToolBar() {
        Button clearFiltersButton = new Button("Clear Filters");
        clearFiltersButton.setOnAction(event -> clearFilters());

        return new ToolBar(
                new Label("Find"),
                searchField,
                clearFiltersButton,
                rootOnlyCheckBox,
                bornMoreThan110CheckBox,
                born110WithIncompleteCheckBox
        );
    }

    private void applyPersonFilter() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        boolean rootOnly = rootOnlyCheckBox.isSelected();
        boolean bornMoreThan110 = bornMoreThan110CheckBox.isSelected() || born110WithIncompleteCheckBox.isSelected();
        boolean born110WithIncomplete = born110WithIncompleteCheckBox.isSelected();
        String[] tokens = searchText.isBlank() ? new String[0] : searchText.split("\\s+");

        filteredPeople.setPredicate(person -> {
            if (person == null) {
                return false;
            }

            if (rootOnly && !person.isRoot()) {
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
        refreshPeopleBirthYearTree();
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

    private void configurePersonTable(Runnable onEditSelected) {
        TableColumn<Person, String> rootColumn = new TableColumn<>("Root");
        rootColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().isRoot() ? "*" : "")
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
                    onEditSelected.run();
                }
            });
            return row;
        });
    }

    private void configurePeopleBirthYearTree() {
        peopleBirthYearTree.setShowRoot(false);
        peopleBirthYearTree.setRoot(new TreeItem<>(new BirthYearTreeNode(-1L, "People")));
        peopleBirthYearTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (syncingPeopleViewSelection) {
                return;
            }

            if (newSelection == null || newSelection.getValue() == null) {
                return;
            }

            long personId = newSelection.getValue().personId();
            if (personId > 0) {
                selectVisiblePersonInTable(personId);
            }
        });
    }

    private void refreshPeopleBirthYearTree() {
        TreeItem<BirthYearTreeNode> root = new TreeItem<>(new BirthYearTreeNode(-1L, "People"));
        Map<String, List<Person>> peopleByBirthYear = new HashMap<>();

        for (Person person : personTable.getItems()) {
            String birthYearGroup = resolveBirthYearGroup(person);
            peopleByBirthYear.computeIfAbsent(birthYearGroup, ignored -> FXCollections.observableArrayList()).add(person);
        }

        List<String> years = peopleByBirthYear.keySet().stream()
                .sorted((left, right) -> {
                    if ("Unknown Birth Year".equals(left)) {
                        return 1;
                    }
                    if ("Unknown Birth Year".equals(right)) {
                        return -1;
                    }
                    return left.compareTo(right);
                })
                .toList();

        for (String year : years) {
            List<Person> people = peopleByBirthYear.get(year);
            TreeItem<BirthYearTreeNode> yearItem = new TreeItem<>(
                    new BirthYearTreeNode(-1L, year + " (" + people.size() + ")")
            );
            yearItem.setExpanded(false);

            for (Person person : people) {
                yearItem.getChildren().add(new TreeItem<>(
                        new BirthYearTreeNode(person.getPersonId(), buildPersonBirthYearTreeLabel(person))
                ));
            }

            root.getChildren().add(yearItem);
        }

        peopleBirthYearTree.setRoot(root);
        if (!root.getChildren().isEmpty()) {
            root.getChildren().forEach(item -> item.setExpanded(false));
        }
    }

    private void selectPersonInBirthYearTree(Person person) {
        syncingPeopleViewSelection = true;
        try {
            if (person == null || peopleBirthYearTree.getRoot() == null) {
                peopleBirthYearTree.getSelectionModel().clearSelection();
                return;
            }

            for (TreeItem<BirthYearTreeNode> yearItem : peopleBirthYearTree.getRoot().getChildren()) {
                for (TreeItem<BirthYearTreeNode> personItem : yearItem.getChildren()) {
                    if (personItem.getValue() != null && person.getPersonId().equals(personItem.getValue().personId())) {
                        yearItem.setExpanded(true);
                        peopleBirthYearTree.getSelectionModel().select(personItem);
                        return;
                    }
                }
            }

            peopleBirthYearTree.getSelectionModel().clearSelection();
        } finally {
            syncingPeopleViewSelection = false;
        }
    }

    private boolean selectVisiblePersonInTable(long personId) {
        syncingPeopleViewSelection = true;
        try {
            for (Person person : personTable.getItems()) {
                if (person.getPersonId().equals(personId)) {
                    personTable.getSelectionModel().select(person);
                    personTable.scrollTo(person);
                    return true;
                }
            }
        } finally {
            syncingPeopleViewSelection = false;
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

    private String resolveBirthYearGroup(Person person) {
        Integer birthYear = extractYear(person == null ? null : person.getBirthDateText());
        return birthYear == null ? "Unknown Birth Year" : String.valueOf(birthYear);
    }

    private String buildPersonBirthYearTreeLabel(Person person) {
        String label = person.getDisplayName();
        if (person.getBirthDateText() != null && !person.getBirthDateText().isBlank()) {
            label += " - " + person.getBirthDateText();
        }
        if (person.getFsPid() != null && !person.getFsPid().isBlank()) {
            label += " [" + person.getFsPid() + "]";
        }
        return label;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private record BirthYearTreeNode(long personId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
