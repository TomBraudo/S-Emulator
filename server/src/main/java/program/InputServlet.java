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

@WebServlet("/program/input")
@MultipartConfig
public class InputServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try {
            List<String> inputVariables = api.getInputVariableNames();
            ResponseHelper.success(resp, "Input variables retrieved successfully", inputVariables);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve input variables: " + e.getMessage());
        }
    }
}