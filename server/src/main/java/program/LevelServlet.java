package main.java.program;

import com.api.Api;
import com.dto.api.ProgramCommands;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.List;

@WebServlet("/program/level")
@MultipartConfig
public class LevelServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try {
            int maxLevel = api.getMaxLevel();
            ResponseHelper.success(resp, "Max level retrieved successfully", maxLevel);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve max level: " + e.getMessage());
        }
    }
}