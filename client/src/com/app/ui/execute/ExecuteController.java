package com.app.ui.execute;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import com.gluonhq.charm.glisten.control.ToggleButtonGroup;

public class ExecuteController {
    
    // Top section - user info and controls
    @FXML
    private Label userNameLabel;
    
    @FXML
    private Label creditsLabel;
    
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
    private ToggleButtonGroup modeSelection;
    
    @FXML
    private ToggleButton debugBtn;
    
    @FXML
    private ToggleButton executeBtn;
    
    @FXML
    private Pane actionButtonsContainer;
    
    @FXML
    private ToggleButtonGroup architectureSelection;
    
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
    
    @FXML
    public void initialize() {
        // Initialize UI components
        setupToggleActions();
        setupArchitectureToggle();
        setupBackButton();
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
    
    private void setupArchitectureToggle() {
        // Set up architecture selection toggle behavior
        if (architectureSelection != null) {
            architectureSelection.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    // Handle architecture change
                    // TODO: Implement architecture selection logic
                }
            });
        }
    }
    
    private void setupBackButton() {
        if (backToDashboardBtn != null) {
            backToDashboardBtn.setOnAction(e -> handleBackToDashboard());
        }
    }
    
    private void showExecuteActions() {
        actionButtonsContainer.getChildren().clear();

        Button startExecution = new Button("Start execution");
        startExecution.setOnAction(event -> startExecution());
        styleActionButton(startExecution);

        actionButtonsContainer.getChildren().add(startExecution);
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

        styleActionButton(startDebug);
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
    }

    private void styleActionButton(Button button) {
        button.setPrefWidth(150);
        button.getStyleClass().addAll("btn", "btn-primary", "btn-wide");
    }
    
    // Action button handlers (TODO: Implement logic)
    
    private void startExecution() {
        // TODO: Open input form, then execute program
    }
    
    private void startDebugging() {
        // TODO: Open input form, then start debugging
    }
    
    private void stepOverInstruction() {
        // TODO: Step over one instruction in debug mode
    }
    
    private void stopDebugging() {
        // TODO: Stop debugging session
    }
    
    private void continueDebug() {
        // TODO: Continue execution until next breakpoint
    }
    
    private void stepBack() {
        // TODO: Step back one instruction in debug mode
    }
    
    private void handleBackToDashboard() {
        // TODO: Navigate back to dashboard
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
}

