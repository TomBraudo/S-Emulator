package main.java;

import com.api.Api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApp {

    private static final Map<String, Api> userApis = new ConcurrentHashMap<>();

    public static Api getApiForUser(String username) {
        return userApis.get(username);
    }

    public static void registerUser(String username) {
        if (userApis.containsKey(username)) throw new IllegalArgumentException("User already registered");
        userApis.put(username, new Api(username));
    }

    public static List<String> getRegisteredUsers() {
        return List.copyOf(userApis.keySet());
    }
}
