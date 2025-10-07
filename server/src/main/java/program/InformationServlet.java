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
import java.util.HashMap;
import java.util.List;
import java.util.OptionalDouble;

@WebServlet("/program/information")
@MultipartConfig
public class InformationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        String programName = req.getParameter("programName");
        try {
            String owner = Api.getProgramOwner(programName);
            int commandsCount = Api.getCommandCount(programName);
            int maxLevel = Api.getProgramMaxExpansionLevel(programName);
            int ranCount = Api.getProgramRanCount(programName);
            OptionalDouble averageCost = Api.getProgramAverageCost(programName);
            String source = Api.getFunctionSourceProgram(programName);
            HashMap<String, Object> info = new HashMap<>();
            info.put("owner", owner);
            info.put("commandsCount", commandsCount);
            info.put("maxLevel", maxLevel);
            info.put("ranCount", ranCount);
            info.put("averageCost", averageCost.orElse(0.0));
            info.put("source", source);

            ResponseHelper.success(resp, "Information retrieved successfully", info);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve information: " + e.getMessage());
        }
    }
}