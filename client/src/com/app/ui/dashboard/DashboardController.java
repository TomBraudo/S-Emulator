package com.app.ui.dashboard;

import javafx.fxml.FXML;
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
import com.app.http.Json;
import com.app.ui.utils.Response;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    @FXML
    private void initialize() {
        if (usernameLabel != null) {
            String id = UserContext.getUserId();
            if (id != null && !id.isBlank()) {
                usernameLabel.setText(id);
            }
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
    private void onUnselectUser(ActionEvent event) {
        // TODO: implement user unselection logic
    }

    @FXML
    private void onExecuteProgram(ActionEvent event) {
        // TODO: implement program execution logic
    }
}
