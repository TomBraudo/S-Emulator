package com.app.http;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public final class Json {
    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .serializeNulls()
            .create();

    private Json() {}

    public static Gson get() {
        return gson;
    }

    public static <T> Type typeOfResponse(Class<T> dataType) {
        return TypeToken.getParameterized(com.app.ui.utils.Response.class, dataType).getType();
    }

    public static <E> Type typeOfListResponse(Class<E> elem) {
        return TypeToken.getParameterized(
            com.app.ui.utils.Response.class,
            TypeToken.getParameterized(java.util.List.class, elem).getType()
        ).getType();
    }
}


