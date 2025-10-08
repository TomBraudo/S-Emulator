package com.app.http;

import com.google.gson.Gson;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient;
import okhttp3.MultipartBody;

import java.io.File;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ApiClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;

    public ApiClient() {
        this(HttpClientProvider.get(), Json.get(), HttpConfig.DEFAULT_BASE_URL);
    }

    public ApiClient(OkHttpClient httpClient, Gson gson, String baseUrl) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public <T> T get(String path, Map<String, ?> query, Class<T> responseType) throws IOException {
        String url = buildUrl(path, query);
        Request request = new Request.Builder().url(url).get().build();
        return execute(request, responseType);
    }

    public <B, T> T post(String path, B body, Map<String, ?> query, Class<T> responseType) throws IOException {
        String url = buildUrl(path, query);
        String json = body == null ? "" : gson.toJson(body);
        RequestBody requestBody = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        return execute(request, responseType);
    }

    public <T> T postWithHeaders(String path, Object body, Map<String, String> headers, Class<T> responseType) throws IOException {
        // Deprecated in favor of post(path, body, query, responseType); kept for compatibility
        return post(path, body, null, responseType);
    }

    public <T> T postMultipartFile(String path, String partName, File file, String contentType, Map<String, ?> query, Class<T> responseType) throws IOException {
        MediaType mt = contentType == null ? MediaType.parse("application/octet-stream") : MediaType.parse(contentType);
        RequestBody fileBody = RequestBody.create(file, mt);
        MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(partName, file.getName(), fileBody);
        String url = buildUrl(path, query);
        Request request = new Request.Builder().url(url).post(mb.build()).build();
        return execute(request, responseType);
    }

    public <B, T> T put(String path, B body, Map<String, ?> query, Class<T> responseType) throws IOException {
        String url = buildUrl(path, query);
        String json = body == null ? "" : gson.toJson(body);
        RequestBody requestBody = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).put(requestBody).build();
        return execute(request, responseType);
    }

    public <T> T delete(String path, Map<String, ?> query, Class<T> responseType) throws IOException {
        String url = buildUrl(path, query);
        Request request = new Request.Builder().url(url).delete().build();
        return execute(request, responseType);
    }

    private <T> T execute(Request request, Class<T> responseType) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? new String(response.body().bytes(), StandardCharsets.UTF_8) : "";
            if (!response.isSuccessful()) {
                throw new ApiException(response.code(), "HTTP " + response.code() + " for " + request.method() + " " + request.url(), body);
            }
            if (responseType == null || responseType == Void.class) {
                return null;
            }
            if (responseType == String.class) {
                @SuppressWarnings("unchecked")
                T cast = (T) body;
                return cast;
            }
            return gson.fromJson(body, responseType);
        }
    }

    public <T> com.app.ui.utils.Response<T> getResponse(String path, Map<String, ?> query, Class<T> dataType) throws IOException {
        java.lang.reflect.Type type = Json.typeOfResponse(dataType);
        String url = buildUrl(path, query);
        Request request = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body() != null ? new String(resp.body().bytes(), StandardCharsets.UTF_8) : "";
            if (!resp.isSuccessful()) {
                throw new ApiException(resp.code(), "HTTP " + resp.code() + " for GET " + request.url(), body);
            }
            return gson.fromJson(body, type);
        }
    }

    public <E> com.app.ui.utils.Response<java.util.List<E>> getListResponse(String path, Map<String, ?> query, Class<E> elementType) throws IOException {
        java.lang.reflect.Type type = Json.typeOfListResponse(elementType);
        String url = buildUrl(path, query);
        Request request = new Request.Builder().url(url).get().build();
        try (Response resp = httpClient.newCall(request).execute()) {
            String body = resp.body() != null ? new String(resp.body().bytes(), StandardCharsets.UTF_8) : "";
            if (!resp.isSuccessful()) {
                throw new ApiException(resp.code(), "HTTP " + resp.code() + " for GET " + request.url(), body);
            }
            return gson.fromJson(body, type);
        }
    }

    public <B, T> com.app.ui.utils.Response<T> postResponse(String path, B body, Map<String, ?> query, Class<T> dataType) throws IOException {
        java.lang.reflect.Type type = Json.typeOfResponse(dataType);
        String url = buildUrl(path, query);
        String json = body == null ? "" : gson.toJson(body);
        RequestBody requestBody = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        try (Response resp = httpClient.newCall(request).execute()) {
            String respBody = resp.body() != null ? new String(resp.body().bytes(), StandardCharsets.UTF_8) : "";
            if (!resp.isSuccessful()) {
                throw new ApiException(resp.code(), "HTTP " + resp.code() + " for POST " + request.url(), respBody);
            }
            return gson.fromJson(respBody, type);
        }
    }

    public <T> com.app.ui.utils.Response<T> postMultipartFileResponse(String path, String partName, File file, String contentType, Map<String, ?> query, Class<T> dataType) throws IOException {
        java.lang.reflect.Type type = Json.typeOfResponse(dataType);
        MediaType mt = contentType == null ? MediaType.parse("application/octet-stream") : MediaType.parse(contentType);
        RequestBody fileBody = RequestBody.create(file, mt);
        MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(partName, file.getName(), fileBody);
        String url = buildUrl(path, query);
        Request req = new Request.Builder().url(url).post(mb.build()).build();
        try (Response resp = httpClient.newCall(req).execute()) {
            String respBody = resp.body() != null ? new String(resp.body().bytes(), StandardCharsets.UTF_8) : "";
            if (!resp.isSuccessful()) {
                throw new ApiException(resp.code(), "HTTP " + resp.code() + " for POST " + req.url(), respBody);
            }
            return gson.fromJson(respBody, type);
        }
    }

    private String buildUrl(String path, Map<String, ?> query) {
        HttpUrl.Builder builder = HttpUrl.parse(baseUrl + path).newBuilder();
        if (query != null) {
            for (Map.Entry<String, ?> e : query.entrySet()) {
                Object value = e.getValue();
                if (value != null) builder.addQueryParameter(e.getKey(), String.valueOf(value));
            }
        }
        return builder.build().toString();
    }
}


