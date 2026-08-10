package family.balling.descendencytracker.ui;

import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

final class DialogThemeSupport {
    private static boolean darkModeEnabled;

    private DialogThemeSupport() {
    }

    static void setDarkModeEnabled(boolean enabled) {
        darkModeEnabled = enabled;
    }

    static void apply(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }

        DialogPane pane = dialog.getDialogPane();
        if (pane == null) {
            return;
        }

        String stylesheet = DialogThemeSupport.class
                .getResource("/family/balling/descendencytracker/ui/app-theme.css")
                .toExternalForm();

        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }

        pane.getStyleClass().addAll("app-root", "dialog-surface", "themed-dialog");
        if (darkModeEnabled && !pane.getStyleClass().contains("dark-mode")) {
            pane.getStyleClass().add("dark-mode");
        }
        if (!darkModeEnabled) {
            pane.getStyleClass().remove("dark-mode");
        }

        Node content = pane.getContent();
        if (content != null) {
            content.getStyleClass().add("editor-grid");
        }
    }
}
