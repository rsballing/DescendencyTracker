package family.balling.descendencytracker.app;

import family.balling.descendencytracker.application.BackupService;
import family.balling.descendencytracker.application.AncestorLineSummaryService;
import family.balling.descendencytracker.application.LineStewardshipService;
import family.balling.descendencytracker.application.OrdinanceEligibilityService;
import family.balling.descendencytracker.application.OrdinanceService;
import family.balling.descendencytracker.application.PersonService;
import family.balling.descendencytracker.application.RelationshipService;
import family.balling.descendencytracker.application.WorkQueueService;
import family.balling.descendencytracker.persistence.DatabaseManager;
import family.balling.descendencytracker.persistence.SqliteLineStewardshipRepository;
import family.balling.descendencytracker.persistence.SqliteOrdinanceRepository;
import family.balling.descendencytracker.persistence.SqlitePersonRepository;
import family.balling.descendencytracker.persistence.SqliteRelationshipRepository;
import family.balling.descendencytracker.repository.LineStewardshipRepository;
import family.balling.descendencytracker.repository.OrdinanceRepository;
import family.balling.descendencytracker.repository.PersonRepository;
import family.balling.descendencytracker.repository.RelationshipRepository;
import family.balling.descendencytracker.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class DescendencyTrackerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.initialize();

            PersonRepository personRepository = new SqlitePersonRepository(databaseManager);
            RelationshipRepository relationshipRepository = new SqliteRelationshipRepository(databaseManager);
            OrdinanceRepository ordinanceRepository = new SqliteOrdinanceRepository(databaseManager);
            LineStewardshipRepository lineStewardshipRepository = new SqliteLineStewardshipRepository(databaseManager);

            PersonService personService = new PersonService(personRepository);
            RelationshipService relationshipService = new RelationshipService(personRepository, relationshipRepository);
            OrdinanceService ordinanceService = new OrdinanceService(ordinanceRepository);
            LineStewardshipService lineStewardshipService = new LineStewardshipService(lineStewardshipRepository);
            OrdinanceEligibilityService ordinanceEligibilityService = new OrdinanceEligibilityService();
            AncestorLineSummaryService ancestorLineSummaryService = new AncestorLineSummaryService(
                    relationshipService,
                    ordinanceService,
                    ordinanceEligibilityService
            );
            BackupService backupService = new BackupService(databaseManager);
            WorkQueueService workQueueService = new WorkQueueService(
                    relationshipService,
                    ordinanceService,
                    ordinanceEligibilityService
            );

            MainView mainView = new MainView(
                    personService,
                    relationshipService,
                    ordinanceService,
                    ancestorLineSummaryService,
                    ordinanceEligibilityService,
                    backupService,
                    workQueueService
            );

            Scene scene = new Scene(mainView, 1500, 900);
            scene.getStylesheets().add(
                    DescendencyTrackerApp.class
                            .getResource("/family/balling/descendencytracker/ui/app-theme.css")
                            .toExternalForm()
            );
            primaryStage.setTitle("DescendencyTracker");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("The application could not start.");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
            ex.printStackTrace();
        }
    }
}
