package main.java.program.debug;

import com.api.Api;
import com.dto.api.ProgramResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.List;

@WebServlet("/program/debug/start")
@MultipartConfig
public class StartServlet extends HttpServlet {

    private static class RequestDto{
        public int expansionLevel;
        public List<Integer> input;
        public List<Integer> breakpoints;
        public String architecture;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        RequestDto dto = RequestHelpers.getBody(req, RequestDto.class);

        try{
            ProgramResult result = api.startDebugging(dto.input, dto.expansionLevel, dto.breakpoints, dto.architecture);
            ResponseHelper.success(resp, "Debugging started successfully", result);
        }
        catch (Exception e){
            ResponseHelper.error(resp, 400, "Failed to start debugging program: " + e.getMessage());
        }
    }
}