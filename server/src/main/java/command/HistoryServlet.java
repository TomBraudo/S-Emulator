package main.java.command;

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

@WebServlet("/command/history")
@MultipartConfig
public class HistoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try {
            int index = Integer.parseInt(req.getParameter("index"));
            int expansionLevel = Integer.parseInt(req.getParameter("expansionLevel"));
            List<String> history = api.getCommandHistory(expansionLevel, index);
            ResponseHelper.success(resp, "History retrieved successfully", history);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve history: " + e.getMessage());
        }
    }
}