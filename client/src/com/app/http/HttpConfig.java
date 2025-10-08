package com.app.http;

public final class HttpConfig {
    private HttpConfig() {}

    public static final String DEFAULT_BASE_URL = "http://localhost:8080";
    public static final long DEFAULT_CONNECT_TIMEOUT_MS = 10_000L;
    public static final long DEFAULT_READ_TIMEOUT_MS = 30_000L;
    public static final long DEFAULT_WRITE_TIMEOUT_MS = 30_000L;
}


