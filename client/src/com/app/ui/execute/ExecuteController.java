package com.app.ui.execute;

import com.app.http.ApiClient;
import com.app.ui.dashboard.components.errorComponents.ErrorMessageController;
import com.app.ui.dashboard.components.errorComponents.WarningMessageController;
import com.app.ui.execute.components.commandRow.CommandRowController;
import com.app.ui.utils.Response;
import com.dto.api.ProgramCommands;
import com.dto.api.ProgramInfo;
import com.dto.api.ProgramResult;
import com.dto.api.ProgramSummary;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kotlin.Result;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.IntConsumer;

import com.app.ui.execute.components.inputComponent.InputFormController;

public class ExecuteController {
    
    // Top section - user info and controls
    @FXML
    private Label userNameLabel;
    
    @FXML
    private Label creditsLabel;
    
    @FXML
    private Label programNameLabel;
    
    @FXML
    private ComboBox<Integer> expansionLevelMenu;
    
    @FXML
    private ComboBox<String> highlightSelectorMenu;
    
    // Left panel - program display
    @FXML
    private VBox commandsContainer;
    
    @FXML
    private HBox summaryLineContainer;
    
    @FXML
    private ScrollPane historyChainScrollPane;
    
    @FXML
    private VBox historyChainContainer;
    
    // Right panel - execution controls
    @FXML
    private HBox modeSelection;
    
    @FXML
    private ToggleButton debugBtn;
    
    @FXML
    private ToggleButton executeBtn;
    
    @FXML
    private Pane actionButtonsContainer;
    
    @FXML
    private HBox architectureSelection;
    
    @FXML
    private ToggleButton arch1Btn;
    
    @FXML
    private ToggleButton arch2Btn;
    
    @FXML
    private ToggleButton arch3Btn;
    
    @FXML
    private ToggleButton arch4Btn;
    
    @FXML
    private VBox executionInputContainer;
    
    @FXML
    private VBox variablesContainer;
    
    @FXML
    private Label cyclesLabel;
    
    @FXML
    private Button backToDashboardBtn;
    
    // Action buttons - created dynamically
    private Button startDebugBtn;
    private Button stepOverBtn;
    private Button continueBtn;
    private Button stopBtn;
    private Button stepBackBtn;
    
    // Breakpoint management
    private final Set<Integer> breakpointIndices = new HashSet<>();
    
    // State tracking
    private int currentExpansionLevel = 0;
    private int selectedArchitecture = 1; // Default to architecture 1
    private List<String> currentCommands = new ArrayList<>();
    private List<String> currentArchitectures = new ArrayList<>();
    private boolean isDebugging = false;
    
    @FXML
    public void initialize(){
        // Initialize UI components
        setActiveProgram();
        // Set program/function name in header
        String displayName = ExecuteContext.getProgramName() == null ? ExecuteContext.getFunctionName() : ExecuteContext.getProgramName();
        setProgramName(displayName);
        setupToggleActions();
        setupArchitectureToggle();
        setupBackButton();
        setupExpansionLevelMenu();
        setupHighlightSelectorMenu();
        
        // Populate menus on init (highlight with expansion level 0)
        populateExpansionLevelMenu();
        populateHighlightSelectorMenu(0);
    }

    private void setActiveProgram() {
        String name = ExecuteContext.getProgramName() == null ? ExecuteContext.getFunctionName() : ExecuteContext.getProgramName();
        ApiClient api = new ApiClient();
        try{
            Response<Void> resp = api.postResponse("/program/set", null,new HashMap<>(){{put("programName", name);}}, Void.class);
        }
        catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
        }

    }

    private void populateCommands(int expansionLevel){
        ApiClient api = new ApiClient();
        try {
            Response<ProgramCommands> resp = api.getResponse("/program/commands", new HashMap<>(){{put("expansionLevel", String.valueOf(expansionLevel));}}, ProgramCommands.class);
            
            if (resp.getData() != null) {
                currentCommands = resp.getData().getCommands();
                currentArchitectures = resp.getData().getArchitectures();
                currentExpansionLevel = expansionLevel;
                
                // Clear existing command rows
                commandsContainer.getChildren().clear();
                
                // Populate command rows with architecture highlighting
                for (int i = 0; i < currentCommands.size(); i++) {
                    String command = currentCommands.get(i);
                    int commandArch = parseArchitecture(currentArchitectures.get(i));
                    
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("components/commandRow/commandRow.fxml"));
                        javafx.scene.Node row = loader.load();
                        CommandRowController controller = loader.getController();
                        
                        int index = i;
                        IntConsumer toggle = (idx) -> toggleBreakpoint(idx, controller);
                        controller.init(index, command, breakpointIndices.contains(index), toggle);
                        controller.setOnCommandClicked(this::showHistoryInChain);
                        
                        // Apply architecture highlighting
                        controller.setArchitecture(commandArch, selectedArchitecture);
                        
                        // Store controller on the row for later access
                        row.getProperties().put("controller", controller);
                        
                        commandsContainer.getChildren().add(row);
                    } catch (IOException e) {
                        // Fallback to simple label if loading fails
                        Label commandLabel = new Label(command);
                        commandLabel.getStyleClass().add("command-label");
                        commandLabel.setMaxWidth(Double.MAX_VALUE);
                        commandLabel.setWrapText(true);
                        commandsContainer.getChildren().add(commandLabel);
                    }
                }
            }
        }
        catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
        }
    }

    private void populateExpansionLevelMenu(){
        ApiClient api = new ApiClient();
        try {
            Response<Integer> resp = api.getResponse("/program/level", null, Integer.class);

            if (resp.getData() != null) {
                expansionLevelMenu.getItems().clear();
                for(int i = 0; i <= resp.getData(); i++){
                    expansionLevelMenu.getItems().add(i);
                }
                expansionLevelMenu.getSelectionModel().select(0);
            }
        }
        catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
        }
    }

    private void populateHighlightSelectorMenu(int expansionLevel){
        ApiClient api = new ApiClient();
        try {
            Response<List<String>> resp = api.getListResponse("/program/highlight", new HashMap<>(){{put("expansionLevel", String.valueOf(expansionLevel));}}, String.class);
            if (resp.getData() != null) {
                highlightSelectorMenu.getItems().clear();
                highlightSelectorMenu.getItems().addAll(resp.getData());
                highlightSelectorMenu.getSelectionModel().select(0);
            }
        }
        catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
        }
    }
    
    private int parseArchitecture(String arch) {
        // Parse architecture string (e.g., "I", "II", "III", "IV") to integer
        if (arch == null || arch.isEmpty()) return 1;
        switch (arch.trim()) {
            case "I": return 1;
            case "II": return 2;
            case "III": return 3;
            case "IV": return 4;
            default: return 1;
        }
    }
    
    private String architectureCode(int selected){
        switch (selected){
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return "I";
        }
    }
    
    private void toggleBreakpoint(int index, CommandRowController controller) {
        if (breakpointIndices.contains(index)) {
            removeBreakpoint(index);
            controller.setActive(false);
        } else {
            setBreakpoint(index);
            controller.setActive(true);
        }
    }
    
    private void setBreakpoint(int index) {
        breakpointIndices.add(index);
        
        // If debugging is active, notify the backend about the new breakpoint
        if (isDebugging) {
            ApiClient api = new ApiClient();
            try {
                api.postResponse("/program/debug/breakpoint", null, 
                    new HashMap<>(){{put("index", String.valueOf(index));}}, 
                    Void.class);
            } catch (Exception e) {
                ErrorMessageController.showError("Failed to set breakpoint: " + e.getMessage());
                // Rollback the local change
                breakpointIndices.remove(index);
            }
        }
    }
    
    private void removeBreakpoint(int index) {
        breakpointIndices.remove(index);
        
        // If debugging is active, notify the backend about breakpoint removal
        if (isDebugging) {
            ApiClient api = new ApiClient();
            try {
                api.deleteResponse("/program/debug/breakpoint", 
                    new HashMap<>(){{put("index", String.valueOf(index));}}, 
                    Void.class);
            } catch (Exception e) {
                ErrorMessageController.showError("Failed to remove breakpoint: " + e.getMessage());
                // Rollback the local change
                breakpointIndices.add(index);
            }
        }
    }
    
    private void setHighlightedIndex(int highlightedIndex) {
        for (javafx.scene.Node n : commandsContainer.getChildren()) {
            if (n instanceof javafx.scene.layout.HBox row) {
                Object controllerObj = row.getProperties().get("controller");
                if (controllerObj instanceof CommandRowController crc) {
                    crc.setHighlighted(crc.getCommandIndex() == highlightedIndex);
                }
            }
        }
    }
    
    private void refreshArchitectureHighlighting() {
        // Update all command rows with new architecture highlighting
        for (int i = 0; i < commandsContainer.getChildren().size(); i++) {
            javafx.scene.Node n = commandsContainer.getChildren().get(i);
            if (n instanceof javafx.scene.layout.HBox row) {
                Object controllerObj = row.getProperties().get("controller");
                if (controllerObj instanceof CommandRowController crc) {
                    // ensure non-debug visuals reflect current debug state (only when actively debugging)
                    crc.setDebugMode(isDebugging);
                    int commandArch = parseArchitecture(currentArchitectures.get(crc.getCommandIndex()));
                    crc.setArchitecture(commandArch, selectedArchitecture);
                }
            }
        }
    }
    
    public void initializeWithContext() {
        // Load context from ExecuteContext
        String username = ExecuteContext.getUsername();
        int credits = ExecuteContext.getCredits();
        String programName = ExecuteContext.getProgramName();
        String functionName = ExecuteContext.getFunctionName();
        
        // Update UI with context
        if (username != null) {
            setUserName(username);
        }
        setCredits(credits);
        
        // Update title or display program/function name
        // For now, we'll just set initial state
        setCycles(0);
    }
    
    private void setupToggleActions() {
        // Default to execute mode (matches FXML selected="true" on executeBtn)
        showExecuteActions();

        executeBtn.setOnAction(event -> {
            // Prevent deselection - always keep one selected
            if(!executeBtn.isSelected()){
                executeBtn.setSelected(true);
            } else {
                showExecuteActions();
                debugBtn.setSelected(false);
            }
        });

        debugBtn.setOnAction(event -> {
            // Prevent deselection - always keep one selected
            if(!debugBtn.isSelected()){
                debugBtn.setSelected(true);
            } else {
                showDebugActions();
                executeBtn.setSelected(false);
            }
        });
    }
    
    private void setupArchitectureToggle() {
        // Set up individual toggle button handlers to prevent deselection
        if (arch1Btn != null) {
            arch1Btn.setOnAction(e -> {
                if (!arch1Btn.isSelected()) {
                    arch1Btn.setSelected(true);
                } else {
                    arch2Btn.setSelected(false);
                    arch3Btn.setSelected(false);
                    arch4Btn.setSelected(false);
                    onArchitectureChanged(1);
                }
            });
        }
        
        if (arch2Btn != null) {
            arch2Btn.setOnAction(e -> {
                if (!arch2Btn.isSelected()) {
                    arch2Btn.setSelected(true);
                } else {
                    arch1Btn.setSelected(false);
                    arch3Btn.setSelected(false);
                    arch4Btn.setSelected(false);
                    onArchitectureChanged(2);
                }
            });
        }
        
        if (arch3Btn != null) {
            arch3Btn.setOnAction(e -> {
                if (!arch3Btn.isSelected()) {
                    arch3Btn.setSelected(true);
                } else {
                    arch1Btn.setSelected(false);
                    arch2Btn.setSelected(false);
                    arch4Btn.setSelected(false);
                    onArchitectureChanged(3);
                }
            });
        }
        
        if (arch4Btn != null) {
            arch4Btn.setOnAction(e -> {
                if (!arch4Btn.isSelected()) {
                    arch4Btn.setSelected(true);
                } else {
                    arch1Btn.setSelected(false);
                    arch2Btn.setSelected(false);
                    arch3Btn.setSelected(false);
                    onArchitectureChanged(4);
                }
            });
        }
    }
    
    private void onArchitectureChanged(int architecture) {
        selectedArchitecture = architecture;
        // Refresh the highlighting for all command rows
        refreshArchitectureHighlighting();
    }
    
    private void setupBackButton() {
        if (backToDashboardBtn != null) {
            backToDashboardBtn.setOnAction(e -> handleBackToDashboard());
        }
    }
    
    private void setupExpansionLevelMenu() {
        if (expansionLevelMenu != null) {
            expansionLevelMenu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    currentExpansionLevel = newVal;
                    populateCommands(currentExpansionLevel);
                    // Refresh highlight options for the selected expansion level
                    populateHighlightSelectorMenu(currentExpansionLevel);
                    writeProgramSummary(currentExpansionLevel);
                }
            });
        }
    }
    
    private void setupHighlightSelectorMenu() {
        if (highlightSelectorMenu != null) {
            highlightSelectorMenu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                applySearchHighlight(newVal);
            });
        }
    }

    private void writeProgramSummary(int expansionLevel) {
        ApiClient api = new ApiClient();
        try {
            Response<ProgramSummary> resp = api.getResponse("/program/summary", new HashMap<>(){{put("expansionLevel", String.valueOf(expansionLevel));}}, ProgramSummary.class);
            summaryLineContainer.getChildren().clear();
            Label title = new Label("Program summary:");
            title.getStyleClass().add("label-strong");
            summaryLineContainer.getChildren().add(title);
            summaryLineContainer.getChildren().add(new Label("I: " + resp.getData().getArchitectureCommandsCount(0)));
            summaryLineContainer.getChildren().add(new Label("II: " + resp.getData().getArchitectureCommandsCount(1)));
            summaryLineContainer.getChildren().add(new Label("III: " + resp.getData().getArchitectureCommandsCount(2)));
            summaryLineContainer.getChildren().add(new Label("IV: " + resp.getData().getArchitectureCommandsCount(3)));
        }
        catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
        }
    }

    private void applySearchHighlight(String selection){
        // If currently in debug mode, suppress search highlighting entirely
        if (debugBtn != null && debugBtn.isSelected()){
            return;
        }
        String token = selection;
        if (token == null || token.equals("None")){
            // clear all search highlights
            for (javafx.scene.Node n : commandsContainer.getChildren()){
                if (n instanceof javafx.scene.layout.HBox row){
                    Object controllerObj = row.getProperties().get("controller");
                    if (controllerObj instanceof CommandRowController crc){
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

        for (javafx.scene.Node n : commandsContainer.getChildren()){
            if (n instanceof javafx.scene.layout.HBox row){
                Object controllerObj = row.getProperties().get("controller");
                if (controllerObj instanceof CommandRowController crc){
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
    
    private void showExecuteActions() {
        actionButtonsContainer.getChildren().clear();

        VBox executeBox = new VBox();
        executeBox.setAlignment(Pos.CENTER);
        executeBox.setPrefWidth(308);  // Match container width
        executeBox.setPrefHeight(100); // Match container height
        
        Button startExecution = new Button("Start execution");
        startExecution.setOnAction(event -> startExecution());
        styleStartButton(startExecution);

        executeBox.getChildren().add(startExecution);
        actionButtonsContainer.getChildren().add(executeBox);

        // Leaving execute mode: disable debug-only visuals
        setDebugVisualMode(false);
        setDebugging(false);
    }

    private void showDebugActions() {
        actionButtonsContainer.getChildren().clear();

        VBox debugBox = new VBox(10); // spacing 10
        debugBox.setAlignment(Pos.CENTER);

        startDebugBtn = new Button("Start debugging");
        stepOverBtn = new Button("Step over");
        stopBtn = new Button("Stop");
        continueBtn = new Button("Continue");
        stepBackBtn = new Button("Step back");

        styleStartButton(startDebugBtn);
        styleActionButton(stepOverBtn);
        styleActionButton(stopBtn);
        styleActionButton(continueBtn);
        styleActionButton(stepBackBtn);

        // skeleton handlers
        startDebugBtn.setOnAction(e -> startDebugging());
        stepOverBtn.setOnAction(e -> stepOverInstruction());
        stopBtn.setOnAction(e -> stopDebugging());
        continueBtn.setOnAction(e -> continueDebug());
        stepBackBtn.setOnAction(e -> stepBack());

        // Initialize buttons disabled; they will be enabled after start/step/continue
        stepOverBtn.setDisable(true);
        stopBtn.setDisable(true);
        continueBtn.setDisable(true);
        stepBackBtn.setDisable(true);

        // Two rows: [Start debugging, Step over] and [Stop, Continue, Step back]
        HBox row1 = new HBox(10);
        row1.setAlignment(Pos.CENTER);
        row1.getChildren().addAll(startDebugBtn, stepOverBtn);

        HBox row2 = new HBox(10);
        row2.setAlignment(Pos.CENTER);
        row2.getChildren().addAll(stopBtn, continueBtn, stepBackBtn);

        debugBox.getChildren().addAll(row1, row2);

        actionButtonsContainer.getChildren().add(debugBox);

        // Don't activate debug visual mode until debugging actually starts
    }

    private void styleActionButton(Button button) {
        button.setPrefWidth(95);
        button.getStyleClass().addAll("btn", "btn-primary");
    }
    
    private void styleStartButton(Button button) {
        button.setPrefWidth(130);
        button.getStyleClass().addAll("btn", "btn-primary");
    }
    
    // Action button handlers (TODO: Implement logic)
    
    private void startExecution() {
        ApiClient api = new ApiClient();
        try {
            // Check if program is runnable with current expansion level and architecture
            Response<Void> resp = api.getResponse("/program/runnability", 
                new HashMap<>(){{
                    put("expansionLevel", String.valueOf(currentExpansionLevel));
                    put("architecture", architectureCode(selectedArchitecture));
                }}, 
                Void.class);
            
            // If we get here with a successful response, program is runnable
        }catch (Exception e){
            // Error response (non-2xx status code) means program is not runnable
            ErrorMessageController.showError(e.getMessage());
            return;
        }

        openInputFormForExecution();
    }

    private int architectureToCost(int arch) {
        return switch (arch) {
            case 1 -> 5;
            case 2 -> 100;
            case 3 -> 500;
            case 4-> 1000;
            default -> 0;
        };
    }

    private void startDebugging() {
        // 1. Collect all active breakpoints
        List<Integer> breakpoints = new ArrayList<>(breakpointIndices);
        
        // 2. Save current expansion level from combo box
        int expansionLevel = currentExpansionLevel;
        
        // 3. Save current architecture from toggle
        int architecture = selectedArchitecture;
        
        // 4. Verify it's legal to start debugging
        ApiClient api = new ApiClient();
        try {
            // Check if program is runnable with current expansion level and architecture
            Response<Void> resp = api.getResponse("/program/runnability", 
                new HashMap<>(){{
                    put("expansionLevel", String.valueOf(expansionLevel));
                    put("architecture", architectureCode(architecture));
                }}, 
                Void.class);
            
            // If we get here with a successful response, program is runnable
        }catch (Exception e){
            // Error response (non-2xx status code) means program is not runnable
            ErrorMessageController.showError(e.getMessage());
            return;
        }
        
        // 5. Open input form with callback
        openInputFormForDebugging(breakpoints, expansionLevel, architecture);
    }
    
    private void stepOverInstruction() {
        ApiClient api = new ApiClient();
        try {
            Response<ProgramResult> resp = api.postResponse("/program/debug/step", null, null, ProgramResult.class);
            handleDebugStepResult(resp.getData());
        } catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
            setDebugVisualMode(false);
            setDebugging(false);
        }
    }
    
    private void continueDebug() {
        ApiClient api = new ApiClient();
        try {
            Response<ProgramResult> resp = api.postResponse("/program/debug/continue", null, null, ProgramResult.class);
            handleDebugStepResult(resp.getData());
        } catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
            setDebugVisualMode(false);
            setDebugging(false);
        }
    }
    
    private void stepBack() {
        ApiClient api = new ApiClient();
        try {
            Response<ProgramResult> resp = api.deleteResponse("/program/debug/step", null, ProgramResult.class);
            handleDebugStepResult(resp.getData());
        } catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
            setDebugVisualMode(false);
            setDebugging(false);
        }
    }
    
    private void handleDebugStepResult(ProgramResult result) {
        // 1. Update credits from sessionCycles
        // Note: sessionCycles is negative when stepping back (refund), positive when stepping forward (charge)
        int newCredits = ExecuteContext.getCredits() - result.getSessionCycles();
        ExecuteContext.setCredits(newCredits);
        setCredits(newCredits);
        
        // 2. Update variable table and cycles
        populateVariablesContainer(result.getVariableToValue());
        cyclesLabel.setText("Cycles: " + result.getCycles());
        
        // 3. If finished, stop debugging session
        if(!result.isDebug()){
            if(result.getHaltReason() == ProgramResult.HaltReason.INSUFFICIENT_CREDITS){
                WarningMessageController.showWarning("Program execution halted due to insufficient credits");
            }
            setDebugVisualMode(false);
            setDebugging(false);
            return;
        }
        
        // 4. If not finished, highlight the current line and update button states
        setHighlightedIndex(result.getDebugIndex());
        
        // Disable step back button if at the beginning (cycles == 0)
        if (stepBackBtn != null) {
            stepBackBtn.setDisable(result.getCycles() == 0);
        }
    }
    
    private void stopDebugging() {
        ApiClient api = new ApiClient();
        try {
            // Notify the server to stop the debugging session
            api.postResponse("/program/debug/stop", null, null, Void.class);
        } catch (Exception e) {
            ErrorMessageController.showError("Failed to stop debugging: " + e.getMessage());
        } finally {
            // Always turn off debug mode locally, even if API call fails
            setDebugVisualMode(false);
            setDebugging(false);
        }
    }

    private void openInputFormForExecution(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("components/inputComponent/inputForm.fxml"));
            ScrollPane root = loader.load();

            InputFormController inputFormController = loader.getController();

            // Placeholder: fetch the input variable names for the current program/function and level
            // Replace the following line with a real fetch and pass the result to initData
            ApiClient api = new ApiClient();
            Response<List<String>> resp = api.getListResponse("/program/input", null, String.class);
            inputFormController.initData(resp.getData());

            // Provide a callback for when Finish is clicked; leave body empty for now
            inputFormController.setDataCallback(inputMap -> {
                populateInputVariablesContainer(inputMap);
                executeCallback(inputMap);
            });

            javafx.stage.Stage formStage = new javafx.stage.Stage();
            formStage.setTitle("Input Form");
            formStage.setScene(new javafx.scene.Scene(root));
            formStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            formStage.showAndWait();

        } catch (IOException e) {
            ErrorMessageController.showError("Failed to load input form:\n" + e.getMessage());
        }
    }

    private void openInputFormForDebugging(List<Integer> breakpoints, int expansionLevel, int architecture){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("components/inputComponent/inputForm.fxml"));
            ScrollPane root = loader.load();

            InputFormController inputFormController = loader.getController();

            // Fetch the input variable names for the current program/function
            ApiClient api = new ApiClient();
            Response<List<String>> resp = api.getListResponse("/program/input", null, String.class);
            inputFormController.initData(resp.getData());

            // Provide callback for when Finish is clicked
            inputFormController.setDataCallback(inputMap -> {
                populateInputVariablesContainer(inputMap);
                debugStartCallback(inputMap, breakpoints, expansionLevel, architecture);
            });

            javafx.stage.Stage formStage = new javafx.stage.Stage();
            formStage.setTitle("Input Form - Debugging");
            formStage.setScene(new javafx.scene.Scene(root));
            formStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            formStage.showAndWait();

        } catch (IOException e) {
            ErrorMessageController.showError("Failed to load input form:\n" + e.getMessage());
        }
    }

    private void executeCallback(Map<String, Integer> inputMap){
        // Build a positional input list [x1, x2, ..., xN] filling missing with 0
        Map<Integer, Integer> indexToValue = new HashMap<>();
        int maxIndex = 0;
        for (Map.Entry<String, Integer> entry : inputMap.entrySet()){
            String key = entry.getKey();
            if (key != null && key.startsWith("x")){
                try{
                    int idx = Integer.parseInt(key.substring(1));
                    maxIndex = Math.max(maxIndex, idx);
                    indexToValue.put(idx, entry.getValue());
                } catch (NumberFormatException ignored){ }
            }
        }
        List<Integer> input = new ArrayList<>();
        for (int i = 1; i <= maxIndex; i++){
            input.add(indexToValue.getOrDefault(i, 0));
        }

        ApiClient api = new ApiClient();
        try {
            Response<ProgramResult> resp = api.postResponse("/program/execute",
                    new HashMap<>() {{
                        put("expansionLevel", currentExpansionLevel);
                        put("architecture", architectureCode(selectedArchitecture));
                        put("input", input);
                    }},
                    null,
                    ProgramResult.class);
            ProgramResult result = resp.getData();
            if(result.getHaltReason() == ProgramResult.HaltReason.INSUFFICIENT_CREDITS){
                WarningMessageController.showWarning("Program execution halted due to insufficient credits, and did not execute completely");
            }
            populateVariablesContainer(result.getVariableToValue());
            int newCredits = ExecuteContext.getCredits() - (result.getCycles() + architectureToCost(selectedArchitecture));
            ExecuteContext.setCredits(newCredits);
            setCredits(newCredits);
            cyclesLabel.setText("Cycles: " + String.valueOf(result.getCycles()));

        } catch (Exception e){
            ErrorMessageController.showError(e.getMessage());
        }
    }

    private void debugStartCallback(Map<String, Integer> inputMap, List<Integer> breakpoints, int expansionLevel, int architecture){
        // Build a positional input list [x1, x2, ..., xN] filling missing with 0
        Map<Integer, Integer> indexToValue = new HashMap<>();
        int maxIndex = 0;
        for (Map.Entry<String, Integer> entry : inputMap.entrySet()){
            String key = entry.getKey();
            if (key != null && key.startsWith("x")){
                try{
                    int idx = Integer.parseInt(key.substring(1));
                    maxIndex = Math.max(maxIndex, idx);
                    indexToValue.put(idx, entry.getValue());
                } catch (NumberFormatException ignored){ }
            }
        }
        List<Integer> input = new ArrayList<>();
        for (int i = 1; i <= maxIndex; i++){
            input.add(indexToValue.getOrDefault(i, 0));
        }

        ApiClient api = new ApiClient();
        try {
            // 1. Call /program/debug/start with the needed body
            Response<ProgramResult> resp = api.postResponse("/program/debug/start",
                    new HashMap<>() {{
                        put("expansionLevel", expansionLevel);
                        put("architecture", architectureCode(architecture));
                        put("input", input);
                        put("breakpoints", breakpoints);
                    }},
                    null,
                    ProgramResult.class);
            
            ProgramResult result = resp.getData();
            
            // 3. Populate the variables container from the variables map
            populateVariablesContainer(result.getVariableToValue());
            
            // 4. Charge credits from the user (overhead + sessionCycles)
            int newCredits = ExecuteContext.getCredits() - (result.getSessionCycles() + architectureToCost(architecture));
            ExecuteContext.setCredits(newCredits);
            setCredits(newCredits);
            cyclesLabel.setText("Cycles: " + result.getCycles());
            
            // 5. Check if the program has ended (regardless of reason)
            if(!result.isDebug()){
                // Program ended during start - stop debugging session
                if(result.getHaltReason() == ProgramResult.HaltReason.INSUFFICIENT_CREDITS){
                    WarningMessageController.showWarning("Program execution halted due to insufficient credits");
                }
                // Don't activate debug mode if program already finished
                setDebugVisualMode(false);
                setDebugging(false);
                return;
            }
            
            // Debugging started successfully and still active - activate debug mode and enable buttons
            setDebugVisualMode(true);
            setDebugging(true);
            
            // Highlight the current instruction index
            setHighlightedIndex(result.getDebugIndex());
            
            // Disable step back button if at the beginning (cycles == 0)
            if (stepBackBtn != null) {
                stepBackBtn.setDisable(result.getCycles() == 0);
            }

        } catch (Exception e){
            // 2. If error response, show error, turn off debug mode, return
            ErrorMessageController.showError(e.getMessage());
            setDebugVisualMode(false);
            setDebugging(false);
            return;
        }
    }
    public void populateInputVariablesContainer(Map<String, Integer> inputMap){
        executionInputContainer.getChildren().clear();
        for (Map.Entry<String, Integer> entry :
                inputMap.entrySet()) {
            Label varLabel = new Label(entry.getKey() + ": " + entry.getValue());
            varLabel.getStyleClass().add("label-strong");
            varLabel.setWrapText(true);
            executionInputContainer.getChildren().add(varLabel);
        }
    }
    public void populateVariablesContainer(List<ProgramResult.VariableToValue> variableMap){
        variablesContainer.getChildren().clear();
        for (ProgramResult.VariableToValue variableToValue: variableMap) {
            Label varLabel = new Label(variableToValue.variable() + ": " + variableToValue.value());
            varLabel.getStyleClass().add("label-strong");
            varLabel.setWrapText(true);
            variablesContainer.getChildren().add(varLabel);
        }
    }

    private void setDebugVisualMode(boolean debugMode){
        for (int i = 0; i < commandsContainer.getChildren().size(); i++) {
            javafx.scene.Node n = commandsContainer.getChildren().get(i);
            if (n instanceof javafx.scene.layout.HBox row) {
                Object controllerObj = row.getProperties().get("controller");
                if (controllerObj instanceof CommandRowController crc) {
                    crc.setDebugMode(debugMode);
                    if (debugMode){
                        // clear non-debug highlights while in debug mode
                        crc.setSearchHighlighted(false);
                    }
                }
            }
        }
    }

    private void setDebugging(boolean debugging){
        this.isDebugging = debugging;
        
        // Enable/disable debug step buttons
        if (stepOverBtn != null) stepOverBtn.setDisable(!debugging);
        if (stopBtn != null) stopBtn.setDisable(!debugging);
        if (continueBtn != null) continueBtn.setDisable(!debugging);
        if (stepBackBtn != null) stepBackBtn.setDisable(!debugging);
        
        // Disable start debugging button when actively debugging
        if (startDebugBtn != null) startDebugBtn.setDisable(debugging);
        
        // Disable execute/debug toggle when actively debugging
        if (debugBtn != null) debugBtn.setDisable(debugging);
        if (executeBtn != null) executeBtn.setDisable(debugging);
    }
    
    private void showHistoryInChain(int commandIndex) {
        try {
            // Get the command history from the API
            ApiClient api = new ApiClient();
            Response<List<String>> resp = api.getListResponse(
                "/command/history",
                new HashMap<>() {{
                    put("index", String.valueOf(commandIndex));
                    put("expansionLevel", String.valueOf(currentExpansionLevel));
                }},
                String.class
            );
            
            if (resp.getData() != null) {
                renderHistoryChain(resp.getData());
            }
        } catch (Exception e) {
            ErrorMessageController.showError("Failed to load command history:\n" + e.getMessage());
        }
    }
    
    private void renderHistoryChain(List<String> history) {
        if (historyChainContainer == null) return;
        historyChainContainer.getChildren().clear();
        
        if (history == null || history.isEmpty()) {
            Label empty = new Label("No history available");
            empty.getStyleClass().add("label-meta");
            historyChainContainer.getChildren().add(empty);
            return;
        }
        
        // Present newest first: commands[N-1] at the top
        for (int i = history.size() - 1; i >= 0; i--) {
            String command = history.get(i);
            Label commandLabel = new Label(command);
            commandLabel.setMaxWidth(Double.MAX_VALUE);
            commandLabel.setWrapText(true);
            commandLabel.getStyleClass().add("command-label");
            historyChainContainer.getChildren().add(commandLabel);
        }
    }
    
    private void handleBackToDashboard() {
        try {
            // Persist current credits to dashboard context before loading UI
            int creditsNow = getCurrentCreditsSafe();
            com.app.ui.dashboard.UserContext.setCredits(creditsNow);

            // Get username from context
            String username = ExecuteContext.getUsername();

            // Clear execution context
            ExecuteContext.clear();

            // Load dashboard
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/app/ui/dashboard/dashboard.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            // Switch scene
            javafx.stage.Stage stage = (javafx.stage.Stage) backToDashboardBtn.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/app/ui/app.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("S-Emulator - Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: Show error message
        }
    }

    private int getCurrentCreditsSafe(){
        try{
            if (creditsLabel != null){
                String text = creditsLabel.getText();
                if (text != null){
                    // Expected format: "Available Credits: X"
                    int idx = text.lastIndexOf(':');
                    if (idx >= 0 && idx + 1 < text.length()){
                        String num = text.substring(idx + 1).trim();
                        return Integer.parseInt(num);
                    }
                    return Integer.parseInt(text.trim());
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }
    
    // Public methods for updating UI
    
    public void setUserName(String userName) {
        if (userNameLabel != null) {
            userNameLabel.setText(userName);
        }
    }
    
    public void setCredits(int credits) {
        if (creditsLabel != null) {
            creditsLabel.setText("Available Credits: " + credits);
        }
    }
    
    public void setCycles(int cycles) {
        if (cyclesLabel != null) {
            cyclesLabel.setText("Cycles: " + cycles);
        }
    }
    
    public void setProgramName(String programName) {
        if (programNameLabel != null && programName != null) {
            programNameLabel.setText(programName);
        }
    }
}

