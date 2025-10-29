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
import java.util.List;

@WebServlet("/function/used-by")
@MultipartConfig
public class ProgramsUsingServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        String name = req.getParameter("name");
        if (name == null || name.isEmpty()) {
            ResponseHelper.error(resp, "Missing required parameter: name");
            return;
        }

        try {
            List<String> programsUsing = Api.getProgramsUsing(name);
            ResponseHelper.success(resp, "Programs using function retrieved successfully", programsUsing);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve programs: " + e.getMessage());
        }
    }
}

