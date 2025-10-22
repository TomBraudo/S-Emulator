package main.java.chat;

import com.dto.api.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton class to manage chat messages in memory
 */
public class ChatManager {
    private static ChatManager instance;
    private final List<ChatMessage> chatHistory;
    
    private ChatManager() {
        chatHistory = new ArrayList<>();
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }
    
    /**
     * Add a new message to the chat history
     */
    public synchronized void addMessage(String username, String content) {
        long timestamp = System.currentTimeMillis();
        ChatMessage message = new ChatMessage(username, content, timestamp);
        chatHistory.add(message);
    }
    
    /**
     * Get all chat messages
     */
    public synchronized List<ChatMessage> getAllMessages() {
        return Collections.unmodifiableList(new ArrayList<>(chatHistory));
    }
    
    /**
     * Clear all chat history (optional, for testing purposes)
     */
    public synchronized void clearHistory() {
        chatHistory.clear();
    }
}

