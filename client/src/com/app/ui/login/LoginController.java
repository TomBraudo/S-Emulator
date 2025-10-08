package com.app.ui.login;

import com.app.http.ApiClient;
import com.app.http.ApiException;
import com.app.http.Json;
import com.app.ui.utils.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    private void onLogin(ActionEvent e) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isEmpty()) {
            errorLabel.setText("Username is required");
            errorLabel.setVisible(true);
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        CompletableFuture.runAsync(() -> {
            ApiClient api = new ApiClient();
            Map<String, String> headers = new HashMap<>();
            headers.put("X-User-Id", username);
            try {
                Response<Void> resp = api.postResponse("/user/register", null, null, Void.class);
                if (resp != null && resp.isSuccess()) {
                    Platform.runLater(() -> openDashboard(username));
                } else {
                    String msg = resp == null ? "Unknown error" : resp.getMessage();
                    Platform.runLater(() -> {
                        errorLabel.setText("Registration failed: " + msg);
                        errorLabel.setVisible(true);
                        loginButton.setDisable(false);
                    });
                }
            } catch (ApiException | IOException ex) {
                Platform.runLater(() -> {
                    errorLabel.setText("Registration failed: " + ex.getMessage());
                    errorLabel.setVisible(true);
                    loginButton.setDisable(false);
                });
            }
        });
    }

    private void openDashboard(String username) {
        try {
            // Pass username to dashboard via a static holder or user context
            com.app.ui.dashboard.UserContext.setUserId(username);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/app/ui/dashboard/dashboard.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/app/ui/app.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("S-Emulator - Dashboard");
            stage.show();
        } catch (IOException ex) {
            errorLabel.setText("Failed to open dashboard: " + ex.getMessage());
            errorLabel.setVisible(true);
            loginButton.setDisable(false);
        }
    }
}


