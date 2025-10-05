package main.java.responses;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResponseHelper {

    private static final Gson gson = new Gson();

    /**
     * Sends a JSON response to the client.
     */
    public static <T> void send(HttpServletResponse resp, int code, boolean success, String message, T data)
            throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(code);

        JsonResponse<T> jsonResponse = new JsonResponse<>(message, code, success, data);
        String json = gson.toJson(jsonResponse);

        resp.getWriter().write(json);
    }

    /**
     * Shortcut for successful response (HTTP 200)
     */
    public static <T> void success(HttpServletResponse resp, String message, T data) throws IOException {
        send(resp, HttpServletResponse.SC_OK, true, message, data);
    }

    /**
     * Shortcut for error response (default HTTP 400)
     */
    public static void error(HttpServletResponse resp, String message) throws IOException {
        send(resp, HttpServletResponse.SC_BAD_REQUEST, false, message, null);
    }

    /**
     * Shortcut for error response with custom HTTP code
     */
    public static void error(HttpServletResponse resp, int code, String message) throws IOException {
        send(resp, code, false, message, null);
    }
}