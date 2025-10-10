package com.app.ui.dashboard.components.errorComponents;

import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class WarningMessageController {
    
    public static void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Center the dialog on the screen
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.centerOnScreen();
        
        alert.showAndWait();
    }
}


