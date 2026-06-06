package com.waterproj.groundwaterpredictor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RainfallClient {
    private static final String BASE_URL = "https://rainfalpredictor.onrender.com";
    private static Retrofit retrofit;

    public static RainfallApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(RainfallApiService.class);
    }
}