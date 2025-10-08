package com.app.ui.dashboard;

import com.app.ui.dashboard.components.statisticsView.RunDetailsController;
import com.app.ui.dashboard.components.user.UserLineController;
import com.dto.api.Statistic;
import com.dto.api.UserInfo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.application.Platform;

import com.app.http.ApiClient;
import com.app.http.ApiException;
import com.app.ui.utils.Response;
import com.app.ui.utils.UiTicker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
    private int creditsCounter = 0;

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

    @FXML
    private void initialize() {
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
                    Platform.runLater(() -> loadedFilePathLabel.setText(file.getAbsolutePath()));
                } else {
                    String msg = resp == null ? "Unknown error" : resp.getMessage();
                    Platform.runLater(() -> loadedFilePathLabel.setText("Upload failed: " + msg));
                }
            } catch (ApiException | IOException ex) {
                Platform.runLater(() -> loadedFilePathLabel.setText("Upload failed: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onChargeCredits(ActionEvent event) {
        // TODO: implement credit charging workflow
    }

    @FXML
    private void onUnselectUser(ActionEvent event) throws IOException {
        populateStatisticsContainer(UserContext.getUserId());
    }

    @FXML
    private void onExecuteProgram(ActionEvent event) {
        // TODO: implement program execution logic
    }

    @FXML
    private void onExecuteFunction(ActionEvent event) {
        // TODO: implement function execution logic
    }
}

