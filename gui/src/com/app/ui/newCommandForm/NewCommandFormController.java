package com.app.ui.newCommandForm;

import com.api.Api;
import com.dto.CommandSchemaDto;
import com.dto.ArgFieldDto;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewCommandFormController {

    @FXML private ComboBox<String> commandNameCombo;
    @FXML private VBox fieldsContainer;
    @FXML private Button finishButton;

    private final Map<String, TextField> keyToField = new HashMap<>();
    private final Map<String, String> keyToType  = new HashMap<>(); // VARIABLE | LABEL | INTEGER | STRING

    @FXML
    public void initialize() {
        List<String> names = Api.listCommandNames();
        commandNameCombo.getItems().setAll(names);
        if (!names.isEmpty()) {
            String defaultName = names.contains("INCREASE") ? "INCREASE" : names.get(0);
            commandNameCombo.getSelectionModel().select(defaultName);
            rebuildFields();
        }
        commandNameCombo.setOnAction(e -> rebuildFields());
        updateFinishDisabled();
    }

    private void rebuildFields() {
        fieldsContainer.getChildren().clear();
        keyToField.clear();
        keyToType.clear();
        String name = commandNameCombo.getValue();
        CommandSchemaDto schema = Api.getCommandSchema(name);

        // Variable: mandatory for all except GOTO_LABEL (UI rule)
        if (!"GOTO_LABEL".equals(name)) {
            fieldsContainer.getChildren().add(labeledField("Variable", "variable", "VARIABLE"));
        }

        // Optional Label
        if (schema.isSupportsLabel()) {
            fieldsContainer.getChildren().add(labeledField("Label (optional)", "label", "LABEL"));
        }

        // Required args
        for (ArgFieldDto arg : schema.getRequiredArgs()) {
            String t = mapArgType(arg);
            fieldsContainer.getChildren().add(labeledField(arg.getDisplayName(), arg.getName(), t));
        }

        updateFinishDisabled();
    }

    private HBox labeledField(String labelText, String key, String type) {
        HBox row = new HBox(8);
        Label label = new Label(labelText);
        TextField tf = new TextField();
        tf.setId(key);
        row.getChildren().addAll(label, tf);
        keyToField.put(key, tf);
        keyToType.put(key, type);
        tf.textProperty().addListener((obs, o, n) -> {
            validateField(key);
            updateFinishDisabled();
        });
        // initial validation (empty is considered invalid for required fields except label)
        validateField(key);
        return row;
    }

    @FXML
    private void onFinish() {
        String name = commandNameCombo.getValue();
        CommandSchemaDto schema = Api.getCommandSchema(name);

        String variable;
        if ("GOTO_LABEL".equals(name)) {
            variable = "z1"; // UI rule
        } else {
            variable = getFieldValue("variable");
        }

        String label = getFieldValue("label");
        if (label != null && label.isBlank()) label = null;

        Map<String, String> args = new HashMap<>();
        for (ArgFieldDto arg : schema.getRequiredArgs()) {
            String v = getFieldValue(arg.getName());
            args.put(arg.getName(), v);
        }

        Api.createAndAddCommand(name, variable, label, args);
        // Close the dialog after success
        javafx.stage.Window w = finishButton.getScene().getWindow();
        if (w instanceof javafx.stage.Stage s) {
            s.close();
        } else {
            w.hide();
        }
    }

    private String getFieldValue(String key) {
        if (key == null) return null;
        TextField tf = keyToField.get(key);
        return tf == null ? null : tf.getText();
    }

    private String mapArgType(ArgFieldDto arg) {
        String name = arg.getName();
        String type = arg.getType();
        if ("assignedVariable".equals(name) || "variableName".equals(name)) return "VARIABLE";
        if ("INTEGER".equals(type)) return "INTEGER";
        if ("LABEL".equals(type)) return "LABEL";
        return "STRING";
    }

    private void validateField(String key) {
        TextField tf = keyToField.get(key);
        if (tf == null) return;
        String type = keyToType.get(key);
        String value = tf.getText();

        boolean required = !"label".equals(key) && !"GOTO_LABEL".equals(commandNameCombo.getValue());
        boolean ok;
        if ((value == null || value.isBlank())) {
            ok = !required; // empty allowed only if not required
        } else {
            ok = switch (type) {
                case "INTEGER" -> value.matches("-?\\d+");
                case "VARIABLE" -> value.matches("([xz]\\d+|y)");
                case "LABEL" -> value.equals("EXIT") || value.matches("L[1-9]\\d?");
                default -> true; // STRING
            };
        }
        setInvalidStyle(tf, !ok);
    }

    private void updateFinishDisabled() {
        boolean anyInvalid = false;
        for (Map.Entry<String, TextField> e : keyToField.entrySet()) {
            TextField tf = e.getValue();
            if (tf.getProperties().getOrDefault("invalid", Boolean.FALSE).equals(Boolean.TRUE)) {
                anyInvalid = true;
                break;
            }
        }
        // also ensure required fields are non-empty
        if (!"GOTO_LABEL".equals(commandNameCombo.getValue())) {
            TextField var = keyToField.get("variable");
            if (var == null || var.getText() == null || var.getText().isBlank()) anyInvalid = true;
        }
        finishButton.setDisable(anyInvalid);
    }

    private void setInvalidStyle(TextField tf, boolean invalid) {
        tf.getProperties().put("invalid", invalid);
        if (invalid) {
            tf.setStyle("-fx-border-color: #d9534f; -fx-border-width: 2;");
        } else {
            tf.setStyle("");
        }
    }
}


