package com.app.ui.errorComponents;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ErrorMessageController {

    @FXML
    private Label errorLabel;

    /**
     * Called automatically by FXMLLoader, sets the message text in the UI.
     */
    public void setErrorMessage(String message) {
        errorLabel.setText(message);
    }

    @FXML
    private void handleConfirm() {
        // Close the window
        Stage stage = (Stage) errorLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Static helper to display an error message in a modal dialog.
     * Call this anywhere: ErrorMessageController.showError("Something went wrong!");
     */
    public static void showError(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(ErrorMessageController.class.getResource("errorMessage.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Error");
            stage.initModality(Modality.APPLICATION_MODAL); // block interaction with other windows
            stage.setScene(new Scene(loader.load()));

            // Get controller and set message
            ErrorMessageController controller = loader.getController();
            controller.setErrorMessage(message);

            stage.showAndWait();
        } catch (IOException e) {
            // Fallback: show a minimal window with the error text to avoid silent failures
            try {
                Stage stage = new Stage();
                stage.setTitle("Error");
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(new javafx.scene.control.Label("Error: " + message + "\n(" + e.getMessage() + ")"), 400, 120));
                stage.showAndWait();
            } catch (Exception ignored) {}
        }
    }
}
