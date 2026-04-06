package family.balling.descendencytracker.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;

import java.util.function.UnaryOperator;

final class FsPidFields {
    private final TextField prefixField = new TextField();
    private final TextField suffixField = new TextField();
    private final HBox container = new HBox(8);

    FsPidFields() {
        prefixField.setPrefColumnCount(4);
        suffixField.setPrefColumnCount(3);
        prefixField.setPromptText("XXXX");
        suffixField.setPromptText("XXX");
        prefixField.setTextFormatter(createFormatter(4));
        suffixField.setTextFormatter(createFormatter(3));
        container.getChildren().addAll(prefixField, suffixField);
    }

    HBox getNode() {
        return container;
    }

    void setPrefWidth(double widthPerField) {
        prefixField.setPrefWidth(widthPerField);
        suffixField.setPrefWidth(widthPerField);
    }

    void setDisable(boolean value) {
        prefixField.setDisable(value);
        suffixField.setDisable(value);
    }

    void clear() {
        prefixField.clear();
        suffixField.clear();
    }

    void requestFocus() {
        prefixField.requestFocus();
    }

    void selectAll() {
        prefixField.selectAll();
    }

    String getValue() {
        String prefix = DateTextSupport.clean(prefixField.getText());
        String suffix = DateTextSupport.clean(suffixField.getText());
        if (prefix == null && suffix == null) {
            return null;
        }
        if (prefix == null || prefix.length() != 4 || suffix == null || suffix.length() != 3) {
            return null;
        }
        return prefix + "-" + suffix;
    }

    boolean isCompleteOrBlank() {
        String prefix = DateTextSupport.clean(prefixField.getText());
        String suffix = DateTextSupport.clean(suffixField.getText());
        if (prefix == null && suffix == null) {
            return true;
        }
        return prefix != null && prefix.length() == 4 && suffix != null && suffix.length() == 3;
    }

    void setValue(String fsPid) {
        clear();
        String normalized = normalize(fsPid);
        if (normalized == null) {
            return;
        }

        String[] parts = normalized.split("-", -1);
        if (parts.length == 2) {
            prefixField.setText(parts[0]);
            suffixField.setText(parts[1]);
        }
    }

    static String normalize(String value) {
        String cleaned = DateTextSupport.clean(value);
        if (cleaned == null) {
            return null;
        }

        String compact = cleaned.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (compact.length() != 7) {
            return cleaned.toUpperCase();
        }

        return compact.substring(0, 4) + "-" + compact.substring(4);
    }

    private TextFormatter<String> createFormatter(int maxLength) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText().toUpperCase().replaceAll("[^A-Z0-9]", "");
            if (next.length() > maxLength) {
                return null;
            }

            change.setText(change.getText().toUpperCase().replaceAll("[^A-Z0-9]", ""));
            if (!change.getControlNewText().equals(next)) {
                int start = change.getRangeStart();
                int end = change.getRangeEnd();
                change.setRange(0, change.getControlText().length());
                change.setText(next);
                change.selectRange(Math.min(next.length(), start), Math.min(next.length(), end));
            }
            return change;
        };
        return new TextFormatter<>(filter);
    }
}
