package main.java.program;

import com.api.Api;
import com.dto.api.ProgramInfo;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalDouble;

@WebServlet("/function/information")
@MultipartConfig
public class FunctionInformationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}

        try {
            List<ProgramInfo> programInfos = new ArrayList<>();
            List<String> programs = Api.getFunctionNames();
            for(String program : programs){
                programInfos.add(Api.getProgramInformation(program));
            }
            ResponseHelper.success(resp, "Information retrieved successfully", programInfos);
        } catch (Exception e) {
            ResponseHelper.error(resp, "Failed to retrieve information: " + e.getMessage());
        }
    }
}