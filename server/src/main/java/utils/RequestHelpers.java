package main.java.utils;

import com.api.Api;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.xml.xsom.impl.Ref;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.ServerApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;

public class RequestHelpers {
    public static final String USER_ID_HEADER = "X-User-Id";
    public static String getUserId(HttpServletRequest req){
        String userId = req.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Missing user header");
        }

        return userId;
    }

    public static <T> T getBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        Gson gson = new Gson();
        return gson.fromJson(body.toString(), clazz);
    }

    public static Api getApi(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userId;
        try{
            userId = RequestHelpers.getUserId(req);
        }
        catch (Exception e){
            ResponseHelper.error(resp, 400, "User ID header missing");
            return null;
        }

        Api api = ServerApp.getApiForUser(userId);
        if(api == null){
            ResponseHelper.error(resp, 400, "User not found");
            return null;
        }

        return api;
    }
}
