package com.app.ui.dashboard;

import com.app.ui.dashboard.components.program.ProgramLineController;
import com.app.ui.dashboard.components.function.FunctionLineController;
import com.app.ui.dashboard.components.statisticsView.RunDetailsController;
import com.app.ui.dashboard.components.user.UserLineController;
import com.dto.api.ProgramInfo;
import com.dto.api.Statistic;
import com.dto.api.UserInfo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.application.Platform;

import com.app.http.ApiClient;
import com.app.http.ApiException;
import com.app.ui.dashboard.components.errorComponents.ErrorMessageController;
import com.app.ui.dashboard.components.errorComponents.SuccessMessageController;
import com.app.ui.utils.Response;
import com.app.ui.utils.UiTicker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DashboardController {

    // Root
    @FXML private AnchorPane dashboardRoot;

    // Top bar
    @FXML private Button loadFileButton;
    @FXML private Label loadedFilePathLabel;
    @FXML private Button chargeCreditsButton;
    @FXML private Label creditsLabel;
    @FXML private Label usernameLabel;
    @FXML private Label dashboardTitleLabel;
    @FXML private Label creditsTitleLabel;

    // Users section
    @FXML private BorderPane usersPane;
    @FXML private ScrollPane usersScroll;
    @FXML private VBox usersContainer;
    @FXML private Button unselectUserButton;

    // Statistics section
    @FXML private ScrollPane statisticsScroll;
    @FXML private VBox statisticsContainer;

    // Programs section
    @FXML private BorderPane programsPane;
    @FXML private ScrollPane programsScroll;
    @FXML private VBox programsContainer;
    @FXML private Button executeProgramButton;

    // Functions section
    @FXML private BorderPane functionsPane;
    @FXML private ScrollPane functionsScroll;
    @FXML private VBox functionsContainer;
    @FXML private Button executeFunctionButton;

    private String contextProgram = null;
    private String contextFunction = null;
    
    // Track which items should be highlighted (for persistence across refreshes)
    private Set<String> highlightedPrograms = new HashSet<>();
    private Set<String> highlightedFunctions = new HashSet<>();

    @FXML
    private void initialize() {
        Integer stored = UserContext.getCredits();
        creditsLabel.setText(String.valueOf(stored == null ? 0 : stored));
        if (usernameLabel != null) {
            String id = UserContext.getUserId();
            if (id != null && !id.isBlank()) {
                usernameLabel.setText(id);
            }
        }

        if(usersContainer != null){
            UiTicker.getInstance().registerTask("update-users", () -> {
                try {
                    updateUserStats();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        if(programsContainer != null){
            UiTicker.getInstance().registerTask("update-programs", () -> {
                try {
                    populateProgramContainer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        if(functionsContainer != null){
            UiTicker.getInstance().registerTask("update-functions", () -> {
                try {
                    populateFunctionContainer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void updateUserStats() throws IOException {
        usersContainer.getChildren().clear();
        ApiClient api = new ApiClient();
        Response<List<UserInfo>> resp = api.getListResponse("/user/all", null, UserInfo.class);
        for(UserInfo user : resp.getData()){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/ui/dashboard/components/user/userLine.fxml"));
            Parent userLineNode = loader.load();
            UserLineController controller = loader.getController();
            controller.init(
                    user.getName(),
                    String.valueOf(user.getProgramUploadedCount()),
                    String.valueOf(user.getFunctionUploadedCount()),
                    String.valueOf(user.getCredits()),
                    String.valueOf(user.getCreditsUsed()),
                    String.valueOf(user.getRunCount())
            );
            controller.setOnPressAction(() -> {
                try {
                    populateStatisticsContainer(user.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            usersContainer.getChildren().add(userLineNode);
        }
    }

    private void populateStatisticsContainer(String userId) throws IOException {
        statisticsContainer.getChildren().clear();
        ApiClient api = new ApiClient();
        Response<List<Statistic>> resp = api.getListResponse("/user/statistics", new HashMap<>(){{ put("user", userId); }}, Statistic.class);
        for(Statistic stat : resp.getData()){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/ui/dashboard/components/statisticsView/runDetails.fxml"));
            Parent runDetailsNode = loader.load();
            RunDetailsController controller = loader.getController();
            controller.setStatistic(stat);
            statisticsContainer.getChildren().add(runDetailsNode);
        }
    }

    private void populateProgramContainer() throws IOException {
        programsContainer.getChildren().clear();
        ApiClient api = new ApiClient();
        Response<List<ProgramInfo>> resp = api.getListResponse("/program/information", null, ProgramInfo.class);

        for(ProgramInfo info : resp.getData()){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/ui/dashboard/components/program/programLine.fxml"));
            Parent programLineNode = loader.load();
            ProgramLineController controller = loader.getController();
            controller.init(
                    info.getName(),
                    info.getOwner(),
                    String.valueOf(info.getCommandsCount()),
                    String.valueOf(info.getMaxLevel()),
                    String.valueOf(info.getRanCount()),
                    String.valueOf(info.getAverageCost())
            );
            controller.setOnPressAction(() -> {
                // Unselect and unhighlight all program lines
                for (Node node : programsContainer.getChildren()) {
                    if (node.getUserData() instanceof ProgramLineController) {
                        ProgramLineController c = (ProgramLineController) node.getUserData();
                        c.setSelected(false);
                        c.setHighlighted(false);
                    }
                }
                
                // Unselect and unhighlight all function lines
                for (Node node : functionsContainer.getChildren()) {
                    if (node.getUserData() instanceof FunctionLineController) {
                        FunctionLineController c = (FunctionLineController) node.getUserData();
                        c.setSelected(false);
                        c.setHighlighted(false);
                    }
                }

                // Select this program line and clear function context
                controller.setSelected(true);
                contextProgram = info.getName();
                contextFunction = null;
                
                // Highlight dependency functions
                highlightProgramDependencies(info.getName());
            });

            // Restore highlighting if this program was previously selected
            if (contextProgram != null && contextProgram.equals(info.getName())) {
                controller.setSelected(true);
            }
            
            // Restore highlighting if this program is in the highlighted set
            if (highlightedPrograms.contains(info.getName())) {
                controller.setHighlighted(true);
            }

            // Store controller reference for easy access
            programLineNode.setUserData(controller);
            programsContainer.getChildren().add(programLineNode);
        }
    }

    private void populateFunctionContainer() throws IOException {
        functionsContainer.getChildren().clear();
        ApiClient api = new ApiClient();
        Response<List<ProgramInfo>> resp = api.getListResponse("/function/information", null, ProgramInfo.class);

        for(ProgramInfo info : resp.getData()){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/ui/dashboard/components/function/functionLine.fxml"));
            Parent functionLineNode = loader.load();
            FunctionLineController controller = loader.getController();
            controller.init(
                    info.getName(),
                    info.getSourceProgram() != null ? info.getSourceProgram() : "N/A",
                    info.getOwner(),
                    String.valueOf(info.getCommandsCount()),
                    String.valueOf(info.getMaxLevel())
            );
            controller.setOnPressAction(() -> {
                // Unselect and unhighlight all function lines
                for (Node node : functionsContainer.getChildren()) {
                    if (node.getUserData() instanceof FunctionLineController) {
                        FunctionLineController c = (FunctionLineController) node.getUserData();
                        c.setSelected(false);
                        c.setHighlighted(false);
                    }
                }
                
                // Unselect and unhighlight all program lines
                for (Node node : programsContainer.getChildren()) {
                    if (node.getUserData() instanceof ProgramLineController) {
                        ProgramLineController c = (ProgramLineController) node.getUserData();
                        c.setSelected(false);
                        c.setHighlighted(false);
                    }
                }

                // Select this function line and clear program context
                controller.setSelected(true);
                contextFunction = info.getName();
                contextProgram = null;
                
                // Highlight dependency functions and programs that use this function
                highlightFunctionDependencies(info.getName());
            });

            // Restore highlighting if this function was previously selected
            if (contextFunction != null && contextFunction.equals(info.getName())) {
                controller.setSelected(true);
            }
            
            // Restore highlighting if this function is in the highlighted set
            if (highlightedFunctions.contains(info.getName())) {
                controller.setHighlighted(true);
            }

            // Store controller reference for easy access
            functionLineNode.setUserData(controller);
            functionsContainer.getChildren().add(functionLineNode);
        }
    }

    private void highlightProgramDependencies(String programName) {
        try {
            ApiClient api = new ApiClient();
            Response<List<String>> resp = api.getListResponse("/dependencies", 
                new HashMap<>() {{ put("name", programName); }}, String.class);
            
            List<String> dependencies = resp.getData();
            
            // Save highlighted state for persistence across refreshes
            highlightedFunctions.clear();
            highlightedFunctions.addAll(dependencies);
            highlightedPrograms.clear();
            
            // Highlight all dependency functions in red
            for (Node node : functionsContainer.getChildren()) {
                if (node.getUserData() instanceof FunctionLineController) {
                    FunctionLineController controller = (FunctionLineController) node.getUserData();
                    if (dependencies.contains(controller.getName())) {
                        controller.setHighlighted(true);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - highlighting is not critical
            e.printStackTrace();
        }
    }

    private void highlightFunctionDependencies(String functionName) {
        try {
            ApiClient api = new ApiClient();
            
            // Get function dependencies
            Response<List<String>> depsResp = api.getListResponse("/dependencies", 
                new HashMap<>() {{ put("name", functionName); }}, String.class);
            List<String> dependencies = depsResp.getData();
            
            // Get programs that use this function
            Response<List<String>> usersResp = api.getListResponse("/function/used-by", 
                new HashMap<>() {{ put("name", functionName); }}, String.class);
            List<String> programsUsing = usersResp.getData();
            
            // Save highlighted state for persistence across refreshes
            highlightedFunctions.clear();
            highlightedFunctions.addAll(dependencies);
            highlightedPrograms.clear();
            highlightedPrograms.addAll(programsUsing);
            
            // Highlight all dependency functions in red
            for (Node node : functionsContainer.getChildren()) {
                if (node.getUserData() instanceof FunctionLineController) {
                    FunctionLineController controller = (FunctionLineController) node.getUserData();
                    if (dependencies.contains(controller.getName())) {
                        controller.setHighlighted(true);
                    }
                }
            }
            
            // Highlight all programs that use this function in red
            for (Node node : programsContainer.getChildren()) {
                if (node.getUserData() instanceof ProgramLineController) {
                    ProgramLineController controller = (ProgramLineController) node.getUserData();
                    if (programsUsing.contains(controller.getName())) {
                        controller.setHighlighted(true);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - highlighting is not critical
            e.printStackTrace();
        }
    }

    @FXML
    private void onLoadFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Program XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = chooser.showOpenDialog(dashboardRoot.getScene().getWindow());
        if (file == null) {
            return;
        }

        loadedFilePathLabel.setText(file.getAbsolutePath());

        CompletableFuture.runAsync(() -> {
            ApiClient api = new ApiClient();
            try {
                Response<Void> resp = api.postMultipartFileResponse("/program/upload", "file", file, "application/xml", null, Void.class);
                if (resp != null && resp.isSuccess()) {
                    Platform.runLater(() -> {
                        loadedFilePathLabel.setText(file.getAbsolutePath());
                        SuccessMessageController.showSuccess("Program uploaded successfully!");
                        // Refresh program and function lists
                        try {
                            populateProgramContainer();
                            populateFunctionContainer();
                        } catch (IOException e) {
                            ErrorMessageController.showError("Failed to refresh program list: " + e.getMessage());
                        }
                    });
                } else {
                    String msg = resp == null ? "Unknown error" : resp.getMessage();
                    Platform.runLater(() -> {
                        loadedFilePathLabel.setText("Upload failed: " + msg);
                        ErrorMessageController.showError("Upload failed: " + msg);
                    });
                }
            } catch (ApiException | IOException ex) {
                Platform.runLater(() -> {
                    loadedFilePathLabel.setText("Upload failed: " + ex.getMessage());
                    ErrorMessageController.showError("Upload failed: " + ex.getMessage());
                });
            }
        });
    }

    @FXML
    private void onChargeCredits(ActionEvent event) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("components/chargeCredits/chargeCredits.fxml"));
            Parent root = loader.load();
            com.app.ui.dashboard.components.chargeCredits.ChargeCreditsController controller = loader.getController();
            controller.setStage(stage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/app/ui/app.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Charge Credits");
            stage.setResizable(false);
            
            // Make the dashboard unuseable while form is open
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(dashboardRoot.getScene().getWindow());
            
            // Show and wait for result
            stage.showAndWait();
            
            // Get the result after the form is closed
            int credits = controller.getResult();
            if (credits > 0) {
                updateCreditsDisplay(credits);
                ApiClient api = new ApiClient();
                Response<Void> resp = api.postResponse("/user/credits", null, new HashMap<>(){{ put("credits", credits); }}, Void.class);
            }
            // If credits <= 0 (cancelled), do nothing
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onUnselectUser(ActionEvent event) throws IOException {
        populateStatisticsContainer(UserContext.getUserId());
    }

    @FXML
    private void onExecuteProgram(ActionEvent event) {
        if (contextProgram == null || contextProgram.isEmpty()) {
            com.app.ui.dashboard.components.errorComponents.ErrorMessageController.showError("Please select a program to execute");
            return;
        }
        
        openExecutePage(contextProgram, null);
    }

    @FXML
    private void onExecuteFunction(ActionEvent event) {
        if (contextFunction == null || contextFunction.isEmpty()) {
            com.app.ui.dashboard.components.errorComponents.ErrorMessageController.showError("Please select a function to execute");
            return;
        }
        
        openExecutePage(null, contextFunction);
    }
    
    private void openExecutePage(String programName, String functionName) {
        try {
            // Get current credits from label
            int currentCredits = 0;
            try {
                currentCredits = Integer.parseInt(creditsLabel.getText());
            } catch (NumberFormatException e) {
                currentCredits = 0;
            }
            
            // Set execution context
            com.app.ui.execute.ExecuteContext.setProgramName(programName);
            com.app.ui.execute.ExecuteContext.setFunctionName(functionName);
            com.app.ui.execute.ExecuteContext.setUsername(UserContext.getUserId());
            com.app.ui.execute.ExecuteContext.setCredits(currentCredits);
            
            // Load execute page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/app/ui/execute/execute.fxml"));
            Parent root = loader.load();
            
            // Get controller and initialize with context
            com.app.ui.execute.ExecuteController controller = loader.getController();
            controller.initializeWithContext();
            
            // Switch scene
            Stage stage = (Stage) dashboardRoot.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/app/ui/app.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("S-Emulator - Execution");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            com.app.ui.dashboard.components.errorComponents.ErrorMessageController.showError("Failed to open execution page:\n" + e.getMessage());
        }
    }
    
    public void updateCreditsDisplay(int creditsToAdd) {
        try {
            String currentCreditsText = creditsLabel.getText();
            int currentCredits = Integer.parseInt(currentCreditsText);
            int newCredits = currentCredits + creditsToAdd;
            creditsLabel.setText(String.valueOf(newCredits));
        } catch (NumberFormatException e) {
            // If parsing fails, just set the new amount
            creditsLabel.setText(String.valueOf(creditsToAdd));
        }
    }
}

