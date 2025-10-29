package main.java.user;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.ServerApp;
import main.java.utils.ResponseHelper;

import java.io.IOException;

@WebServlet("/user/register")
@MultipartConfig
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userId = req.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            ResponseHelper.error(resp, 400, "Missing X-User-Id header");
            return;
        }

        try {
            ServerApp.registerUser(userId);
        }
        catch (Exception e){
            ResponseHelper.error(resp, 400, "Failed to register user: " + e.getMessage());
            return;
        }


        ResponseHelper.success(resp, "User registered successfully", null);
    }
}
