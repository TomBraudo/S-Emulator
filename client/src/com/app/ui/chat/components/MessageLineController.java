package com.app.ui.chat.components;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageLineController {
    @FXML private HBox messageRoot;
    @FXML private VBox messageBox;
    @FXML private Label senderLabel;
    @FXML private Label contentLabel;
    @FXML private Label timestampLabel;

    public void init(String sender, String content, long timestamp, boolean isCurrentUser) {
        senderLabel.setText(sender);
        contentLabel.setText(content);
        
        // Format timestamp to HH:mm
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String timeString = timeFormat.format(new Date(timestamp));
        timestampLabel.setText(timeString);
        
        // Align messages: current user on right, others on left (WhatsApp style)
        if (isCurrentUser) {
            messageRoot.setAlignment(Pos.CENTER_RIGHT);
            messageBox.getStyleClass().add("message-box-sent");
        } else {
            messageRoot.setAlignment(Pos.CENTER_LEFT);
            messageBox.getStyleClass().add("message-box-received");
        }
    }
}

