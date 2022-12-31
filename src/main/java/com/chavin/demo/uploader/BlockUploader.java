package com.chavin.demo.uploader;


import io.reactivex.rxjava3.core.Single;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface BlockUploader {

    @POST("test/upload/")
    @Multipart
    Single<Response<ResponseBody>> upload(@Part("block") RequestBody body);
}
