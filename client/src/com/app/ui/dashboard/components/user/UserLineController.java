package com.app.ui.dashboard.components.user;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class UserLineController {
    @FXML private Label userNameLabel;
    @FXML private Label programsUploadedLabel;
    @FXML private Label functionsUploadedLabel;
    @FXML private Label creditsLabel;
    @FXML private Label creditsUsedLabel;
    @FXML private Label runsLabel;
    @FXML private VBox root;
    
    private Runnable onPressAction;

    public void init(String userName,
                     String programsUploaded,
                     String functionsUploaded,
                     String credits,
                     String creditsUsed,
                     String runs) {
        userNameLabel.setText(userName);
        programsUploadedLabel.setText(programsUploaded);
        functionsUploadedLabel.setText(functionsUploaded);
        creditsLabel.setText(credits);
        creditsUsedLabel.setText(creditsUsed);
        runsLabel.setText(runs);
    }

    @FXML
    private void initialize() {
        // Set click handler on the root VBox
        if (root != null) {
            root.setOnMouseClicked(this::handleClick);
        }
    }
    
    private void handleClick(MouseEvent event) {
        if (onPressAction != null) {
            onPressAction.run();
        }
    }

    public void setOnPressAction(Runnable action) {
        this.onPressAction = action;
    }
}


