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

@WebServlet("/program/debug/breakpoint")
@MultipartConfig
public class BreakpointServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        int index = Integer.parseInt(req.getParameter("index"));

        try{
            api.setBreakpoint(index);
            ResponseHelper.success(resp, "Breakpoint set successfully", null);
        }
        catch (Exception e){
            ResponseHelper.error(resp, 400, "Failed to set breakpoint: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        int index = Integer.parseInt(req.getParameter("index"));
        try{
            api.removeBreakpoint(index);
            ResponseHelper.success(resp, "Breakpoint removed successfully", null);
        }
        catch (Exception e){
            ResponseHelper.error(resp, 400, "Failed to remove breakpoint: " + e.getMessage());
        }
    }
}