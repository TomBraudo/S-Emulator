package com.app.ui.dashboard.components.chargeCredits;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ChargeCreditsController {
    @FXML private TextField creditsField;
    @FXML private Label errorLabel;
    @FXML private Button chargeButton;
    @FXML private Button cancelButton;
    
    private int result = -1; // -1 indicates cancelled
    private Stage stage;

    @FXML
    private void onCharge(ActionEvent e) {
        String creditsText = creditsField.getText() == null ? "" : creditsField.getText().trim();
        
        // Validate input
        if (creditsText.isEmpty()) {
            showError("Amount is required");
            return;
        }
        
        int credits;
        try {
            credits = Integer.parseInt(creditsText);
            if (credits <= 0) {
                showError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Please enter a valid number");
            return;
        }

        // Set result and close
        this.result = credits;
        closeWindow();
    }

    @FXML
    private void onCancel(ActionEvent e) {
        this.result = -1; // Indicate cancelled
        closeWindow();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void closeWindow() {
        if (stage != null) {
            stage.close();
        }
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public int getResult() {
        return result;
    }
}
