package main.java.program;

import com.api.Api;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.List;

@WebServlet("/program/commands")
@MultipartConfig
public class CommandsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        int expansionLevel = Integer.parseInt(req.getParameter("expansionLevel"));
        try {
            List<String> commands = api.getProgramCommands(expansionLevel);
            ResponseHelper.success(resp, "Commands retrieved successfully", commands);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve commands: " + e.getMessage());
        }
    }
}