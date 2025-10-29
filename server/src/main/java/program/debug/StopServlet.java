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

@WebServlet("/program/debug/stop")
@MultipartConfig
public class StopServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try{
            int cost = api.stopDebug();
            ResponseHelper.success(resp, "Program debugging stopped successfully. Total cost: " + cost, cost);
        }
        catch (Exception e){
            ResponseHelper.error(resp, 400, "Failed to stop debugging program: " + e.getMessage());
        }
    }
}