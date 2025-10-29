package com.app.ui.dashboard.components.errorComponents;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class SuccessMessageController {
    
    public static void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Center the dialog on the screen
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.centerOnScreen();
        
        alert.showAndWait();
    }
}

