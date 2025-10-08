package main.java.user;

import com.api.Api;
import com.dto.api.UserInfo;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.ServerApp;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/user/all")
@MultipartConfig
public class AllUsersServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> allUsers = ServerApp.getRegisteredUsers();
        List<UserInfo> userInfos = new ArrayList<>();
        for(String user : allUsers){
            Api api = ServerApp.getApiForUser(user);
            userInfos.add(api.getInfo());
        }
        ResponseHelper.success(resp, "Users retrieved successfully", userInfos);
    }
}
