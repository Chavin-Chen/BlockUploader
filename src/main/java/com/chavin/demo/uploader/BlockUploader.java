package com.chavin.demo.uploader;


import io.reactivex.rxjava3.core.Single;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

import java.util.List;

public interface BlockUploader {

    @POST("test/upload.php")
    @Multipart
    Single<Response<ResponseBody>> upload(@Header("filename") String filename,
                                          @Header("total") long total,
                                          @Header("offset") long offset,
                                          @Part List<MultipartBody.Part> body);
}
