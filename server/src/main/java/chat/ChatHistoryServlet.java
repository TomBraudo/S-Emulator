package main.java.chat;

import com.dto.api.ChatMessage;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.List;

@WebServlet("/chat/history")
public class ChatHistoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ChatManager chatManager = ChatManager.getInstance();
            List<ChatMessage> messages = chatManager.getAllMessages();
            ResponseHelper.success(resp, "Chat history retrieved successfully", messages);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve chat history: " + e.getMessage());
        }
    }
}

