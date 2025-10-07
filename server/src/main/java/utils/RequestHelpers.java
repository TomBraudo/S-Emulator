package main.java.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.xml.xsom.impl.Ref;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    public static HashMap<String, Object> getBody(HttpServletRequest req) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
        return gson.fromJson(body.toString(), type);
    }
}
