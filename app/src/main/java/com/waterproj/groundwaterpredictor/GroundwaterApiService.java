package com.waterproj.groundwaterpredictor;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GroundwaterApiService {
    @POST("predict")
    Call<GroundwaterResponse> predict(@Body GroundwaterRequest request);
}
