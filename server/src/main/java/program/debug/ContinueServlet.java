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

@WebServlet("/program/debug/continue")
@MultipartConfig
public class ContinueServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try{
            ProgramResult result = api.continueDebug();
            ResponseHelper.success(resp, "Debugging continued successfully", result);
        }
        catch (Exception e){
            ResponseHelper.error(resp, 400, "Failed to continue debugging program: " + e.getMessage());
        }
    }
}