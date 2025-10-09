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
    
    @FXML
    public void initialize() {
        // Initialize UI components
        setupToggleActions();
        setupArchitectureToggle();
        setupBackButton();
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
        // TODO: Implement architecture change logic
        System.out.println("Architecture changed to: " + architecture);
    }
    
    private void setupBackButton() {
        if (backToDashboardBtn != null) {
            backToDashboardBtn.setOnAction(e -> handleBackToDashboard());
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
}

