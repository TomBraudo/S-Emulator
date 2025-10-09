package main.java.program;

import com.api.Api;
import com.dto.api.ProgramInfo;
import com.dto.api.ProgramSummary;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalDouble;

@WebServlet("/program/summary")
@MultipartConfig
public class SummaryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try {
            int expansionLevel = Integer.parseInt(req.getParameter("expansionLevel"));
            ProgramSummary summary = api.getProgramSummary(expansionLevel);
            ResponseHelper.success(resp, "Summary retrieved successfully", summary);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve summary: " + e.getMessage());
        }
    }
}