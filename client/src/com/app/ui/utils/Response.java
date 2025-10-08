package com.app.ui.utils;

public class Response<T> {
    private String message;
    private int code;
    private boolean success;
    private T data;

    public Response() {}

    public Response(String message, int code, boolean success, T data) {
        this.message = message;
        this.code = code;
        this.success = success;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }
    public int getCode() {
        return code;
    }
    public boolean isSuccess() {
        return success;
    }
    public T getData() {
        return data;
    }
}
