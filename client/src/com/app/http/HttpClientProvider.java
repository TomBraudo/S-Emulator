package com.app.http;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import com.app.ui.dashboard.UserContext;

import java.time.Duration;

public final class HttpClientProvider {
    private static volatile OkHttpClient sharedClient;

    private HttpClientProvider() {}

    public static OkHttpClient get() {
        OkHttpClient local = sharedClient;
        if (local == null) {
            synchronized (HttpClientProvider.class) {
                local = sharedClient;
                if (local == null) {
                    sharedClient = local = buildDefaultClient();
                }
            }
        }
        return local;
    }

    private static OkHttpClient buildDefaultClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String userId = UserContext.getUserId();
                    Request original = chain.request();
                    if (userId == null || userId.isBlank()) {
                        return chain.proceed(original);
                    }
                    Request withHeader = original.newBuilder()
                            .header("X-User-Id", userId)
                            .build();
                    return chain.proceed(withHeader);
                })
                .connectTimeout(Duration.ofMillis(HttpConfig.DEFAULT_CONNECT_TIMEOUT_MS))
                .readTimeout(Duration.ofMillis(HttpConfig.DEFAULT_READ_TIMEOUT_MS))
                .writeTimeout(Duration.ofMillis(HttpConfig.DEFAULT_WRITE_TIMEOUT_MS))
                .build();
    }
}


