package com.app.ui.dashboard.components.user;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class UserLineController {
    @FXML private Label userNameLabel;
    @FXML private Label programsUploadedLabel;
    @FXML private Label functionsUploadedLabel;
    @FXML private Label creditsLabel;
    @FXML private Label creditsUsedLabel;
    @FXML private Label runsLabel;

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
}


