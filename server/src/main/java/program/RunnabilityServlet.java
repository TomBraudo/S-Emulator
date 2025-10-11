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
import java.util.Map;

@WebServlet("/program/runnability")
@MultipartConfig
public class RunnabilityServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        int expansionLevel = Integer.parseInt(req.getParameter("expansionLevel"));
        String architecture = req.getParameter("architecture");
        try {
            Map.Entry<Boolean, String> canRun = api.canRun(expansionLevel, architecture);
            if(canRun.getKey()){
                ResponseHelper.success(resp, "Program can run", canRun.getValue());
            }
            else{
                ResponseHelper.error(resp, "Program cannot run: " + canRun.getValue());
            }
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve runnability: " + e.getMessage());
        }
    }
}