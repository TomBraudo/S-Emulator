package com.app.ui.execute;

import com.app.http.ApiClient;
import com.app.ui.dashboard.components.errorComponents.ErrorMessageController;
import com.app.ui.execute.components.commandRow.CommandRowController;
import com.app.ui.utils.Response;
import com.dto.api.ProgramCommands;
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
        // TODO: If debugging is active, notify the backend about the new breakpoint
    }
    
    private void removeBreakpoint(int index) {
        breakpointIndices.remove(index);
        // TODO: If debugging is active, notify the backend about breakpoint removal
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
                    // ensure non-debug visuals reflect current debug state
                    crc.setDebugMode(debugBtn != null && debugBtn.isSelected());
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

        Button startDebug = new Button("Start debugging");
        stepOverBtn = new Button("Step over");
        stopBtn = new Button("Stop");
        continueBtn = new Button("Continue");
        stepBackBtn = new Button("Step back");

        styleStartButton(startDebug);
        styleActionButton(stepOverBtn);
        styleActionButton(stopBtn);
        styleActionButton(continueBtn);
        styleActionButton(stepBackBtn);

        // skeleton handlers
        startDebug.setOnAction(e -> startDebugging());
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
        row1.getChildren().addAll(startDebug, stepOverBtn);

        HBox row2 = new HBox(10);
        row2.setAlignment(Pos.CENTER);
        row2.getChildren().addAll(stopBtn, continueBtn, stepBackBtn);

        debugBox.getChildren().addAll(row1, row2);

        actionButtonsContainer.getChildren().add(debugBox);

        // Entering debug mode: ensure only debug highlight is visible
        setDebugVisualMode(true);
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
        // TODO: Open input form, then execute program
    }
    
    private void startDebugging() {
        // TODO: Open input form, then start debugging
        setDebugging(true);
    }
    
    private void stepOverInstruction() {
        // TODO: Step over one instruction in debug mode
    }
    
    private void stopDebugging() {
        // TODO: Stop debugging session
        setDebugVisualMode(false);
        setDebugging(false);
    }
    
    private void continueDebug() {
        // TODO: Continue execution until next breakpoint
    }
    
    private void stepBack() {
        // TODO: Step back one instruction in debug mode
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
        if (stepOverBtn != null) stepOverBtn.setDisable(!debugging);
        if (stopBtn != null) stopBtn.setDisable(!debugging);
        if (continueBtn != null) continueBtn.setDisable(!debugging);
        if (stepBackBtn != null) stepBackBtn.setDisable(!debugging);
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

