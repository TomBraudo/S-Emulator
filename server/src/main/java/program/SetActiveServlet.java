package main.java.program;

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
import java.util.HashMap;

@WebServlet("/program/set")
@MultipartConfig
public class SetActiveServlet extends HttpServlet {
    private static class RequestDto{
        public String programName;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        RequestDto dto = RequestHelpers.getBody(req, RequestDto.class);
        try {
            api.setCurProgram(dto.programName);
            ResponseHelper.success(resp, "Active program set successfully", null);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to set active program: " + e.getMessage());
        }
    }
}
