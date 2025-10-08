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
            com.dto.api.ProgramInfo info = Api.getProgramInformation(programName);
            ResponseHelper.success(resp, "Information retrieved successfully", info);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve information: " + e.getMessage());
        }
    }
}