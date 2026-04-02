package family.balling.descendencytracker.app;

import javafx.application.Application;

public final class AppLauncher {
    private AppLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(DescendencyTrackerApp.class, args);
    }
}