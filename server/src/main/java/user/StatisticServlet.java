package main.java.user;

import com.api.Api;
import com.dto.api.Statistic;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.utils.RequestHelpers;
import main.java.utils.ResponseHelper;

import java.io.IOException;
import java.util.List;

@WebServlet("/user/statistics")
@MultipartConfig
public class StatisticServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String user = req.getParameter("user");
        List<Statistic> statistics = Api.getUserStatistics(user);
        ResponseHelper.success(resp, "Statistics retrieved successfully", statistics);
    }
}
