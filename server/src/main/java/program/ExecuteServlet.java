package main.java.program;

import com.api.Api;
import com.api.ProgramResult;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import main.java.ServerApp;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

@WebServlet("/program/execute")
@MultipartConfig
public class ExecuteServlet extends HttpServlet {

    private static class RequestDto{
        public int expansionLevel;
        public List<Integer> input;
        public String architecture;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        RequestDto dto = RequestHelpers.getBody(req, RequestDto.class);

        try{
            ProgramResult result = api.executeProgram(dto.input, dto.expansionLevel, dto.architecture);
            if(result.getHaltReason().equals(ProgramResult.HaltReason.INSUFFICIENT_CREDITS)){
                resp.sendError(HttpServletResponse.SC_PAYMENT_REQUIRED, "Insufficient credits to execute program");
                return;
            }
            ResponseHelper.success(resp, "Program executed successfully", result);
        }
        catch (Exception e){
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to execute program: " + e.getMessage());
            return;
        }
    }
}