package main.java.program;

import com.api.Api;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import main.java.ServerApp;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.*;

@WebServlet("/program/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        Api api = RequestHelpers.getApi(req, resp);
        if(api == null){return;}


        Part filePart = req.getPart("file");
        if (filePart == null) {
            ResponseHelper.error(resp, 400, "Missing file part");
            return;
        }

        try (InputStream fileContent = filePart.getInputStream()) {
            api.loadSProgram(fileContent);
            ResponseHelper.success(resp, "Program uploaded successfully", null);
        } catch (Exception e) {
            ResponseHelper.error(resp, 400, "Failed to upload program: " + e.getMessage());
            return;
        }
    }
}