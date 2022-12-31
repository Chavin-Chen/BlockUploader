package com.chavin.demo.uploader;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final Retrofit retrofit = new Retrofit.Builder().baseUrl("http://localtest")
            .addCallAdapterFactory(RxJava3CallAdapterFactory.createSynchronous())
            .build();

    public static void main(String[] args) throws Exception {
        File file = new File("php-8.2.0.tar.gz");
        int blockSize = 1024 * 1024 * 2;
        int cnt = (int) (file.length() / blockSize + ((file.length() % blockSize) != 0 ? 1 : 0));

        System.out.println(file.length() + "/" + blockSize + " : " + cnt);

        CountDownLatch latch = new CountDownLatch(cnt);
        new BlockReader(file, (offset, bytes) -> {
            if (null == bytes) {
                System.out.println(offset + " : failed");
                return;
            }
            syncUpload(file.getName(), offset, bytes);
            latch.countDown();
        }, 4, blockSize).start(null);

        latch.await();

        System.out.println("all things done!");
    }

    private static void syncUpload(String name, long offset, byte[] bytes) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("offset", String.valueOf(offset))
                .addFormDataPart("data", name,
                        RequestBody.create(MediaType.parse("application/octet-stream"), bytes))
                .build();
        retrofit.create(BlockUploader.class).upload(body).subscribe(resp -> {
            if (resp.isSuccessful()) {
                System.out.println("✓ offset: " + offset + " upload succeed");
            } else {
                System.out.println("✗ offset: " + offset + " upload failed");
            }
        }, throwable -> {
            System.out.println("! offset: " + offset + " upload failed");
        });
    }
}
