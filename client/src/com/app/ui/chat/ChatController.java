package com.app.ui.chat;

import com.app.http.ApiClient;
import com.app.ui.dashboard.UserContext;
import com.app.ui.dashboard.components.errorComponents.ErrorMessageController;
import com.app.ui.utils.Response;
import com.dto.api.ChatMessage;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class ChatController {
    
    @FXML private AnchorPane chatRoot;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Button dashboardButton;
    
    private String currentUsername;
    private Timeline messagePollingTimeline;
    private int lastMessageCount = 0;
    
    @FXML
    private void initialize() {
        currentUsername = UserContext.getUserId();
        
        // Set up Enter key handler for message input
        if (messageInput != null) {
            messageInput.setOnKeyPressed(this::handleKeyPress);
        }
        
        // Load chat history when opening the chat
        loadChatHistory();
        
        // Start polling for new messages every second
        startMessagePolling();
    }
    
    private void startMessagePolling() {
        messagePollingTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> loadChatHistory())
        );
        messagePollingTimeline.setCycleCount(Animation.INDEFINITE);
        messagePollingTimeline.play();
    }
    
    private void stopMessagePolling() {
        if (messagePollingTimeline != null) {
            messagePollingTimeline.stop();
        }
    }
    
    private void handleKeyPress(KeyEvent event) {
        // Send message on Enter (without Shift)
        if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
            event.consume(); // Prevent newline
            onSendMessage();
        }
    }
    
    @FXML
    private void onSendMessage() {
        String content = messageInput.getText();
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        
        // Send message to server
        try {
            ApiClient api = new ApiClient();
            HashMap<String, String> body = new HashMap<>();
            body.put("username", currentUsername);
            body.put("content", content.trim());
            
            Response<Void> resp = api.postResponse("/chat/send", body, null, Void.class);
            if (resp != null && resp.isSuccess()) {
                // Clear input field
                messageInput.clear();
                
                // Reload chat history to show the new message
                loadChatHistory();
            } else {
                String msg = resp == null ? "Unknown error" : resp.getMessage();
                ErrorMessageController.showError("Failed to send message: " + msg);
            }
        } catch (Exception e) {
            ErrorMessageController.showError("Failed to send message: " + e.getMessage());
        }
    }
    
    private void loadChatHistory() {
        try {
            ApiClient api = new ApiClient();
            Response<List<ChatMessage>> resp = api.getListResponse("/chat/history", null, ChatMessage.class);
            
            if (resp != null && resp.isSuccess()) {
                List<ChatMessage> messages = resp.getData();
                // Only update UI if the message count has changed
                if (messages.size() != lastMessageCount) {
                    lastMessageCount = messages.size();
                    Platform.runLater(() -> {
                        displayMessages(messages);
                    });
                }
            }
        } catch (Exception e) {
            // Silently ignore errors during polling to avoid spamming error messages
            // Only show error on initial load
            if (lastMessageCount == 0) {
                ErrorMessageController.showError("Failed to load chat history: " + e.getMessage());
            }
        }
    }
    
    private void displayMessages(List<ChatMessage> messages) {
        messagesContainer.getChildren().clear();
        
        for (ChatMessage message : messages) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/app/ui/chat/components/messageLine.fxml")
                );
                Parent messageNode = loader.load();
                com.app.ui.chat.components.MessageLineController controller = loader.getController();
                
                boolean isCurrentUser = message.getUsername().equals(currentUsername);
                controller.init(message.getUsername(), message.getContent(), message.getTimestamp(), isCurrentUser);
                
                messagesContainer.getChildren().add(messageNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Auto-scroll to bottom
        Platform.runLater(() -> {
            messagesScrollPane.setVvalue(1.0);
        });
    }
    
    @FXML
    private void onBackToDashboard() {
        try {
            // Stop polling messages when leaving the chat
            stopMessagePolling();
            
            // Load dashboard
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/app/ui/dashboard/dashboard.fxml")
            );
            Parent root = loader.load();
            
            // Switch scene
            Stage stage = (Stage) chatRoot.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/app/ui/app.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("S-Emulator - Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            ErrorMessageController.showError("Failed to return to dashboard: " + e.getMessage());
        }
    }
}

