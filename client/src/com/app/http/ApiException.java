package com.app.http;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public ApiException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}


