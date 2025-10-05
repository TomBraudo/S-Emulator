package main.java.program;

import com.api.Api;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import main.java.ServerApp;

import java.io.*;

@WebServlet("/program/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        String userId = req.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing user header");
            return;
        }

        Api api = ServerApp.getApiForUser(userId);

        Part filePart = req.getPart("file");
        if (filePart == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing file part");
            return;
        }

        try (InputStream fileContent = filePart.getInputStream()) {
            api.loadSProgram(fileContent);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to load program: " + e.getMessage());
            return;
        }

        resp.setContentType("text/plain");
        resp.getWriter().println("Program uploaded successfully for user " + userId);
    }
}