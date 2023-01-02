package com.chavin.demo.uploader;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localhost:8080/")
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createSynchronous())
            .build();

    public static void main(String[] args) throws Exception {
        File file = new File("php-8.2.0.tar.gz");
//        File file = new File("LICENSE.zip");
        int blockSize = 1024 * 1024 * 2;
        int cnt = (int) (file.length() / blockSize + ((file.length() % blockSize) != 0 ? 1 : 0));

        System.out.println(file.length() + "/" + blockSize + " : " + cnt);

        CountDownLatch latch = new CountDownLatch(cnt);
        String fileName = file.getName();
        long fileLength = file.length();
        new BlockReader(file, (offset, bytes) -> {
            if (null == bytes) {
                System.out.println(offset + " : failed");
                return;
            }
            syncUpload(fileName, fileLength, offset, bytes);
            latch.countDown();
        }, 4, blockSize).start(null);

        latch.await();

        System.out.println("all things done!");
    }

    private static void syncUpload(String fileName, long fileLength, long offset, byte[] bytes) {
        RequestBody data = RequestBody.create(MediaType.parse("application/octet-stream"), bytes);
        MultipartBody body = new MultipartBody.Builder()
                .addFormDataPart("file", fileName, data)
                .setType(MultipartBody.FORM)
                .build();
        retrofit.create(BlockUploader.class).upload(fileName, fileLength, offset, body.parts()).subscribe(resp -> {
            if (resp.isSuccessful()) {
                System.out.println("✓ offset: " + offset + " upload succeed " + resp.code());
            } else {
                System.out.println("✗ offset: " + offset + " upload failed " + resp.code());
            }
        }, throwable -> {
            System.out.println("! offset: " + offset + " upload failed");
        });
    }
}
