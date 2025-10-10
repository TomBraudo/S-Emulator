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

@WebServlet("/program/highlight")
@MultipartConfig
public class HighlightServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try {
            int expansionLevel = Integer.parseInt(req.getParameter("expansionLevel"));
            List<String> variables = api.getVariables(expansionLevel);
            List<String> labels = api.getLabels(expansionLevel);
            List<String> items = new java.util.ArrayList<>();
            items.add("None");
            items.addAll(variables);
            items.addAll(labels);
            ResponseHelper.success(resp, "Highlights retrieved successfully", items);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve highlights: " + e.getMessage());
        }
    }
}