package main.java.utils;

public class JsonResponse<T> {
    private String message;
    private int code;
    private boolean success;
    private T data;

    public JsonResponse(String message, int code, boolean success, T data) {
        this.message = message;
        this.code = code;
        this.success = success;
        this.data = data;
    }

    // Getters (optional, Gson can use fields directly)
    public String getMessage() { return message; }
    public int getCode() { return code; }
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
}