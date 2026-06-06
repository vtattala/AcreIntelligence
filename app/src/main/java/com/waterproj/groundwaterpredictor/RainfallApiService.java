package com.waterproj.groundwaterpredictor;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RainfallApiService {
    @POST("predict_rainfall")
    Call<RainfallResponse> predictRainfall(@Body RainfallRequest request);
}