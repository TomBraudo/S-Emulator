package main.java.user;

import com.api.Api;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.ServerApp;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;

@WebServlet("/user/credits")
@MultipartConfig
public class AddCreditsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userId;
        try{
            userId = RequestHelpers.getUserId(req);
        }
        catch (Exception e){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing user header");
            return;
        }

        Api api = ServerApp.getApiForUser(userId);

        if(api == null){
            ResponseHelper.error(resp, 400, "User not found");
            return;
        }

        int credits = Integer.parseInt(req.getParameter("credits"));
        api.addCredits(credits);

        ResponseHelper.success(resp, "Credits added successfully", null);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userId;
        try{
            userId = RequestHelpers.getUserId(req);
        }
        catch (Exception e){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing user header");
            return;
        }

        Api api = ServerApp.getApiForUser(userId);

        if(api == null){
            ResponseHelper.error(resp, 400, "User not found");
            return;
        }

        int credits = api.getCredits();

        ResponseHelper.success(resp, "Credits retrieved successfully", credits);
    }
}
