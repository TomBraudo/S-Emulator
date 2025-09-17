package com.app.ui;

import com.api.Api;
import com.api.ProgramResult;
import com.app.ui.errorComponents.ErrorMessageController;
import com.app.ui.inputComponent.InputFormController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.function.IntConsumer;

public class MainController {
    @FXML
    private ComboBox<String> highlightSelectorMenu;
    @FXML
    private ComboBox<Integer> expansionLevelMenu;
    @FXML
    private ComboBox<String> programSelectorMenu;
    @FXML
    private Label cyclesLabel;
    @FXML
    private VBox variablesContainer;
    @FXML
    private Button loadProgramBtn;
    @FXML
    private Label filePathLabel;
    @FXML
    private ProgressBar loadingProgressBar;
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
    private VBox statisticsTable;
    @FXML
    private ToggleButton debugBtn, executeBtn;

    private Button stepOverBtn;
    private Button continueBtn;
    private Button stopBtn;
    private Button stepBackBtn;

    private List<Integer> curInput = new ArrayList<>();
    private int curExpansionLevel = 0;
    private final Map<String, Integer> lastVariableValues = new HashMap<>();

    @FXML
    public void initialize() {
        newInputBtn.setOnAction(event -> openInputForm());
        loadProgramBtn.setOnAction(event -> loadSProgram());

        setupToggleActions();
        initExpansionLevelMenu();
        initProgramSelectorMenu();
        initHighlightSelectorMenu();
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
        stepOverBtn = new Button("Step over");
        stopBtn = new Button("Stop");
        continueBtn = new Button("Continue");
        stepBackBtn = new Button("Step back");

        styleActionButton(startDebug);
        styleActionButton(stepOverBtn);
        styleActionButton(stopBtn);
        styleActionButton(continueBtn);
        styleActionButton(stepBackBtn);

        // skeleton handlers
        startDebug.setOnAction(e -> startDebuggingProgram());
        stepOverBtn.setOnAction(e -> stepOverInstruction());
        stopBtn.setOnAction(e -> stopDebugging());
        continueBtn.setOnAction(e -> continueDebug());
        stepBackBtn.setOnAction(e -> stepBack());

        // Initialize buttons disabled; they will be enabled after start/step/continue via writeVariablesState
        stepOverBtn.setDisable(true);
        stopBtn.setDisable(true);
        continueBtn.setDisable(true);
        stepBackBtn.setDisable(true);

        // Two rows: [Start debugging, Step over] and [Stop, Continue, Step back]
        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER);
        row1.getChildren().addAll(startDebug, stepOverBtn);

        HBox row2 = new HBox(10);
        row2.setAlignment(Pos.CENTER);
        row2.getChildren().addAll(stopBtn, continueBtn, stepBackBtn);

        debugBox.getChildren().addAll(row1, row2);

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

        variablesContainer.getChildren().removeIf(node -> !Objects.equals(node.getId(), "variablesContainerHeader"));

        for(ProgramResult.VariableToValue var : res.getVariableToValue()){
            Label varLabel = new Label(var.variable() + ": " + var.value());
            varLabel.setStyle("-fx-font-weight: bold;");
            variablesContainer.getChildren().add(varLabel);
        }
        cyclesLabel.setText("Cycles: " + res.getCycles());
        refreshStatisticsTable();
    }

    private void startDebuggingProgram(){
        if(!Api.isLoaded()){
            ErrorMessageController.showError("Program not loaded, can't debug");
            return;
        }
        ProgramResult res = Api.startDebugging(curInput, curExpansionLevel, new ArrayList<>(breakpointIndices));

        // Update variables view each debug step
        writeVariablesState(res);
    }

    private void setHighlightedIndex(int highlightedIndex){
        for (int i = 0; i < programDisplayVBox.getChildren().size(); i++){
            javafx.scene.Node n = programDisplayVBox.getChildren().get(i);
            if (n instanceof javafx.scene.layout.HBox row){
                Object controllerObj = row.getProperties().get("controller");
                if (controllerObj instanceof com.app.ui.commandRow.CommandRowController crc){
                    crc.setHighlighted(i == highlightedIndex);
                }
            }
        }
    }

    private void stepOverInstruction(){
        if(!Api.isLoaded()){
            ErrorMessageController.showError("Program not loaded, can't step");
            return;
        }
        ProgramResult res = Api.stepOver();

        writeVariablesState(res);
    }
    
    private void continueDebug(){
        if(!Api.isLoaded()){
            ErrorMessageController.showError("Program not loaded, can't continue");
            return;
        }
        ProgramResult res = Api.continueDebug();

        writeVariablesState(res);
    }

    private void stopDebugging(){
        if(!Api.isLoaded()){
            ErrorMessageController.showError("Program not loaded, can't stop");
            return;
        }
        Api.stopDebug();
        // After stopping, clear highlights and disable buttons
        setHighlightedIndex(-1);
        if (stepOverBtn != null) stepOverBtn.setDisable(true);
        if (stopBtn != null) stopBtn.setDisable(true);
        if (continueBtn != null) continueBtn.setDisable(true);
        if (stepBackBtn != null) stepBackBtn.setDisable(true);
    }

    private void stepBack() {
        if(!Api.isLoaded()){
            ErrorMessageController.showError("Program not loaded, can't step back");
            return;
        }
        try{
            ProgramResult res = Api.stepBack();
            writeVariablesState(res);
        }
        catch (Exception e){
            ErrorMessageController.showError("Failed to step back:\n" + e.getMessage());
        }
    }

    private void writeVariablesState(ProgramResult res) {

        variablesContainer.getChildren().removeIf(node -> !Objects.equals(node.getId(), "variablesContainerHeader"));

        // Build a map of current variable values to detect changes
        Map<String, Integer> currentValues = new HashMap<>();
        for (ProgramResult.VariableToValue var : res.getVariableToValue()) {
            currentValues.put(var.variable(), var.value());
        }

        for(ProgramResult.VariableToValue var : res.getVariableToValue()){
            String name = var.variable();
            int value = var.value();
            Label varLabel = new Label(name + ": " + value);
            // Highlight if value changed compared to previous step
            Integer prev = lastVariableValues.get(name);
            if (prev != null && prev != value) {
                varLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #b7f7b0;");
            } else {
                varLabel.setStyle("-fx-font-weight: bold;");
            }
            variablesContainer.getChildren().add(varLabel);
        }

        // Update last seen values for next comparison
        lastVariableValues.clear();
        lastVariableValues.putAll(currentValues);
        cyclesLabel.setText("Cycles: " + res.getCycles());

        if(res.isDebug()){
            int idx = res.getDebugIndex();
            setHighlightedIndex(idx);
            if (stepOverBtn != null) stepOverBtn.setDisable(false);
            if (stopBtn != null) stopBtn.setDisable(false);
            if (continueBtn != null) continueBtn.setDisable(false);
            if (stepBackBtn != null) stepBackBtn.setDisable(false);
        } else {
            setHighlightedIndex(-1);
            if (stepOverBtn != null) stepOverBtn.setDisable(true);
            if (stopBtn != null) stopBtn.setDisable(true);
            if (continueBtn != null) continueBtn.setDisable(true);
            if (stepBackBtn != null) stepBackBtn.setDisable(true);
            refreshStatisticsTable();
        }
    }

    private void refreshStatisticsTable(){
        if (statisticsTable == null) return;
        statisticsTable.getChildren().clear();
        for (com.api.Statistic stat : com.api.Statistic.getStatistics()){
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("statisticsView/runDetails.fxml"));
                javafx.scene.Node row = loader.load();
                com.app.ui.statisticsView.RunDetailsController c = loader.getController();
                c.setStatistic(stat);
                statisticsTable.getChildren().add(row);
            } catch (IOException e){
                // ignore UI refresh errors
            }
        }
    }

    private void loadSProgram() {
        try {
            // The loading happens asynchronously now, so we need to handle that
            openFileChooser();

            // We'll postpone the UI updates until the loading task completes
            // The actual loading happens in a background thread in openFileChooser
        } catch (Exception e) {
            ErrorMessageController.showError("Failed to load S program:\n" + e.getMessage());
        }
    }

    private void populateExpansionLevelMenu() {
        expansionLevelMenu.getItems().setAll(
                java.util.stream.IntStream.rangeClosed(0, Api.getMaxLevel())
                        .boxed()
                        .toList()
        );
        curExpansionLevel = 0;
        expansionLevelMenu.getSelectionModel().selectFirst();
    }

    private void populateProgramChooser() {
        programSelectorMenu.getItems().setAll(Api.getCurProgramName());
        programSelectorMenu.getItems().addAll(Api.getAvailableFunctions());
        programSelectorMenu.getSelectionModel().selectFirst();
    }

    private void initExpansionLevelMenu() {
        expansionLevelMenu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                curExpansionLevel = newVal;
                List<String> commands = Api.getProgramCommands(curExpansionLevel);
                printLoadedProgram(commands);
                populateHighlightSelector();
            }
        });
    }

    private void initProgramSelectorMenu() {
        programSelectorMenu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<String> commands = Api.getFunctionCommands(newVal, curExpansionLevel);
                printLoadedProgram(commands);
                Api.setCurProgram(newVal);
                populateExpansionLevelMenu(); // safe now, no duplicate listener
                populateHighlightSelector();
            }
        });
    }

    private void initHighlightSelectorMenu(){
        populateHighlightSelector();
        highlightSelectorMenu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applySearchHighlight(newVal);
        });
    }

    private void populateHighlightSelector(){
        if (!Api.isLoaded()){
            highlightSelectorMenu.getItems().setAll("None");
            highlightSelectorMenu.getSelectionModel().selectFirst();
            return;
        }
        List<String> labels = Api.getLabels(curExpansionLevel);
        List<String> variables = Api.getVariables(curExpansionLevel);
        List<String> items = new ArrayList<>();
        items.add("None");
        items.addAll(labels);
        items.addAll(variables);
        highlightSelectorMenu.getItems().setAll(items);
        highlightSelectorMenu.getSelectionModel().selectFirst();
    }

    private void applySearchHighlight(String selection){
        String token = selection;
        if (token == null || token.equals("None")){
            // clear all search highlights
            for (javafx.scene.Node n : programDisplayVBox.getChildren()){
                if (n instanceof javafx.scene.layout.HBox row){
                    Object controllerObj = row.getProperties().get("controller");
                    if (controllerObj instanceof com.app.ui.commandRow.CommandRowController crc){
                        crc.setSearchHighlighted(false);
                    }
                }
            }
            return;
        }

        List<String> tokensToMatch = new ArrayList<>();
        if (token.startsWith("L")){
            tokensToMatch.add(token);
        } else if (token.startsWith("x") || token.startsWith("z")){
            String suffix = token.substring(1);
            tokensToMatch.add("x" + suffix);
            tokensToMatch.add("z" + suffix);
        } else {
            // fallback: match raw token
            tokensToMatch.add(token);
        }

        for (javafx.scene.Node n : programDisplayVBox.getChildren()){
            if (n instanceof javafx.scene.layout.HBox row){
                Object controllerObj = row.getProperties().get("controller");
                if (controllerObj instanceof com.app.ui.commandRow.CommandRowController crc){
                    String text = crc.getCommandText();
                    boolean match = false;
                    if (text != null){
                        for (String t : tokensToMatch){
                            if (text.contains(t)) { match = true; break; }
                        }
                    }
                    crc.setSearchHighlighted(match);
                }
            }
        }
    }

    private final Set<Integer> breakpointIndices = new HashSet<>();

    private void printLoadedProgram(List<String> commands) {
        programDisplayVBox.getChildren().clear();
        // Clear breakpoints on reload for now (basic behavior)
        breakpointIndices.clear();

        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("commandRow/commandRow.fxml"));
                javafx.scene.Node row = loader.load();
                com.app.ui.commandRow.CommandRowController controller = loader.getController();

                int index = i;
                IntConsumer toggle = (idx) -> toggleBreakpoint(idx, controller);
                controller.init(index, command, breakpointIndices.contains(index), toggle);
                controller.setOnCommandClicked(idx -> openHistoryWindow(idx));

                // store controller on the row for later highlighting
                row.getProperties().put("controller", controller);

                programDisplayVBox.getChildren().add(row);
            } catch (IOException e) {
                // Fallback to simple label if loading fails
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
    }

    private void openHistoryWindow(int commandIndex){
        try {
            List<String> history = Api.getCommandHistory(curExpansionLevel, commandIndex);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("historyView/historyView.fxml"));
            ScrollPane root = loader.load();
            com.app.ui.historyView.HistoryViewController controller = loader.getController();
            controller.setHistory(history);

            Stage stage = new Stage();
            stage.setTitle("Command history");
            stage.setScene(new Scene(root, 480, 360));
            stage.initModality(Modality.NONE);
            stage.show();
        } catch (IOException e){
            ErrorMessageController.showError("Failed to open history view\n" + e.getMessage());
        }
    }

    private void toggleBreakpoint(int index, com.app.ui.commandRow.CommandRowController controller){
        if(breakpointIndices.contains(index)){
            removeBreakpoint(index);
            controller.setActive(false);
        } else {
            setBreakpoint(index);
            controller.setActive(true);
        }
    }

    private void setBreakpoint(int index){
        if(breakpointIndices.add(index) && Api.isDebugging()){
            Api.setBreakpoint(index);
        }
    }

    private void removeBreakpoint(int index){
        if(breakpointIndices.remove(index) && Api.isDebugging()){
            Api.removeBreakpoint(index);
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

            // First, hide file path label and show progress bar
            filePathLabel.setVisible(false);
            loadingProgressBar.setVisible(true);
            loadingProgressBar.setProgress(0);

            // Create task for loading program with animation
            javafx.concurrent.Task<Void> loadTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Actually load the program first (this is fast)
                    Api.loadSProgram(filePath);

                    // Simulate longer loading time (1.5 seconds total)
                    for (int i = 1; i <= 15; i++) {
                        updateProgress(i, 15);
                        Thread.sleep(100); // Sleep for 100ms, 15 times = 1.5 seconds
                    }
                    return null;
                }
            };

            // Bind progress bar to task progress
            loadingProgressBar.progressProperty().bind(loadTask.progressProperty());

            // Handle when loading is complete
            loadTask.setOnSucceeded(event -> {
                // Update UI on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    // Show file path and hide progress bar
                    filePathLabel.setText(filePath);
                    loadingProgressBar.setVisible(false);
                    filePathLabel.setVisible(true);

                    // Unbind progress bar
                    loadingProgressBar.progressProperty().unbind();

                    // Update UI with loaded program
                    try {
                        List<String> commands = Api.getProgramCommands(curExpansionLevel);
                        printLoadedProgram(commands);
                        populateExpansionLevelMenu();
                        populateProgramChooser();
                        populateHighlightSelector();
                        refreshStatisticsTable();
                    } catch (Exception e) {
                        ErrorMessageController.showError("Error loading program details: " + e.getMessage());
                    }
                });
            });

            // Handle loading failure
            loadTask.setOnFailed(event -> {
                javafx.application.Platform.runLater(() -> {
                    Throwable exception = loadTask.getException();
                    loadingProgressBar.setVisible(false);
                    filePathLabel.setVisible(true);
                    loadingProgressBar.progressProperty().unbind();
                    ErrorMessageController.showError("Failed to load program: " + exception.getMessage());
                });
            });

            // Start the loading task in a new thread
            new Thread(loadTask).start();
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
