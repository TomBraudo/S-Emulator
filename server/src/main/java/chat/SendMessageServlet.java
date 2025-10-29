package main.java.chat;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;

@WebServlet("/chat/send")
@MultipartConfig
public class SendMessageServlet extends HttpServlet {
    
    private static class RequestDto {
        public String username;
        public String content;
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Parse request body
            RequestDto dto = RequestHelpers.getBody(req, RequestDto.class);
            
            // Validate input
            if (dto.username == null || dto.username.trim().isEmpty()) {
                ResponseHelper.error(resp, "Username is required");
                return;
            }
            
            if (dto.content == null || dto.content.trim().isEmpty()) {
                ResponseHelper.error(resp, "Message content is required");
                return;
            }
            
            // Add message to chat history
            ChatManager chatManager = ChatManager.getInstance();
            chatManager.addMessage(dto.username.trim(), dto.content.trim());
            
            ResponseHelper.success(resp, "Message sent successfully", null);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to send message: " + e.getMessage());
        }
    }
}

