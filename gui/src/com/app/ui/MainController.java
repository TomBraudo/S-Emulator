package com.app.ui;

import com.api.Api;
import com.api.ProgramResult;
import com.app.ui.errorComponents.ErrorMessageController;
import com.app.ui.inputComponent.InputFormController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class MainController {
    @FXML
    private Label cyclesLabel;
    @FXML
    private VBox variablesContainer;
    @FXML
    private Button loadProgramBtn;
    @FXML
    private Label filePathLabel;
    @FXML
    private Button newInputBtn;
    @FXML
    private ScrollPane inputScrollPane;
    @FXML
    private AnchorPane inputDisplayPane;
    @FXML
    private VBox inputVbox;
    @FXML
    private ScrollPane programDisplayScrollPane;
    @FXML
    private VBox programDisplayVBox;
    @FXML
    private VBox actionsPanel;
    @FXML
    private ToggleButton debugBtn, executeBtn;

    private List<Integer> curInput;
    private int curExpansionLevel = 0;

    @FXML
    public void initialize() {
        newInputBtn.setOnAction(event -> openInputForm());
        loadProgramBtn.setOnAction(event -> loadSProgram());

        setupToggleActions();
    }

    private void setupToggleActions() {
        showExecuteActions();

        executeBtn.setOnAction(event -> {
            if(executeBtn.isSelected()){
                showExecuteActions();
                debugBtn.setSelected(false);
            }
        });

        debugBtn.setOnAction(event -> {
            if(debugBtn.isSelected()){
                showDebugActions();
                executeBtn.setSelected(false);
            }
        });
    }

    private void showExecuteActions() {
        actionsPanel.getChildren().clear();

        Button startExecution = new Button("Start execution");
        startExecution.setOnAction(event -> executeProgram());
        styleActionButton(startExecution);

        actionsPanel.getChildren().add(startExecution);
    }

    private void showDebugActions() {
        actionsPanel.getChildren().clear();

        VBox debugBox = new VBox(10); // spacing 10
        debugBox.setAlignment(Pos.CENTER);

        Button startDebug = new Button("Start debugging");
        Button stepOver = new Button("Step over");
        Button stepBack = new Button("Step backwards");

        styleActionButton(startDebug);
        styleActionButton(stepOver);
        styleActionButton(stepBack);

        debugBox.getChildren().addAll(startDebug, stepOver, stepBack);

        actionsPanel.getChildren().add(debugBox);
    }

    private void styleActionButton(Button button) {
        button.setPrefWidth(150);
        button.setStyle(
                "-fx-background-color: #38cb82;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: black;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;"
        );
    }


    private void executeProgram() {
        ProgramResult res = Api.executeProgram(curInput, curExpansionLevel);
        for(ProgramResult.VariableToValue var : res.getVariableToValue()){
            Label varLabel = new Label(var.variable() + ": " + var.value());
            varLabel.setStyle("-fx-font-weight: bold;");
            variablesContainer.getChildren().add(varLabel);
        }
        cyclesLabel.setText("Cycles: " + res.getCycles());
    }

    private void loadSProgram() {
        try {
            openFileChooser();
        } catch (Exception e) {
            ErrorMessageController.showError("Failed to load S program:\n" + e.getMessage());
            return;
        }

        List<String> commands = Api.getProgramCommands();

        // Clear old items before adding new ones
        programDisplayVBox.getChildren().clear();

        for (String command : commands) {
            Label commandLabel = new Label(command);
            commandLabel.setStyle(
                    "-fx-border-color: black; -fx-border-width: 1; -fx-padding: 5; "
                            + "-fx-background-color: white;"
            );
            commandLabel.setMaxWidth(Double.MAX_VALUE);
            commandLabel.setWrapText(true);

            programDisplayVBox.getChildren().add(commandLabel);
        }
    }

    private void openFileChooser() throws Exception {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();

        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Program Files", "*.xml"),
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) loadProgramBtn.getScene().getWindow();
        java.io.File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            String filePath = file.getAbsolutePath();
            Api.loadSProgram(file.getAbsolutePath());
            filePathLabel.setText(filePath);
        } else {
            throw new Exception("No file selected");
        }
    }

    private void openInputForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("inputComponent/inputForm.fxml"));
            ScrollPane root = loader.load();

            InputFormController inputFormController = loader.getController();

            List<String> inputVariables = Api.getInputVariableNames();

            inputFormController.initData(inputVariables);

            inputFormController.setDataCallback(inputMap -> {
                displayInputData(inputMap);
            });

            Stage formStage = new Stage();
            formStage.setTitle("Input Form");
            formStage.setScene(new Scene(root));
            formStage.initModality(Modality.APPLICATION_MODAL);
            formStage.showAndWait();

        } catch (IOException e) {
            ErrorMessageController.showError("Failed to load input form:\n" + e.getMessage());
        }
    }

    private void displayInputData(Map<String, Integer> data) {
        inputVbox.getChildren().clear();

        Comparator<String> numericComparator = (key1, key2) -> {
            int num1 = Integer.parseInt(key1.substring(1));
            int num2 = Integer.parseInt(key2.substring(1));
            return Integer.compare(num1, num2);
        };

        TreeMap<String, Integer> sortedData = new TreeMap<>(numericComparator);
        sortedData.putAll(data);

        Label header = new Label("Input Data Received:");
        header.setStyle("-fx-font-weight: bold;");
        inputVbox.getChildren().add(header);

        for (Map.Entry<String, Integer> entry : sortedData.entrySet()) {
            Label dataLabel = new Label(entry.getKey() + ": " + entry.getValue());
            inputVbox.getChildren().add(dataLabel);
        }

        int maxIndex = 0;
        if (!sortedData.isEmpty()) {
            String lastKey = sortedData.lastKey();
            maxIndex = Integer.parseInt(lastKey.substring(1));
        }

        curInput = new ArrayList<>();
        for (int i = 1; i <= maxIndex; i++) {
            String variableKey = "x" + i;
            int value = sortedData.getOrDefault(variableKey, 0);
            curInput.add(value);
        }
    }
}
