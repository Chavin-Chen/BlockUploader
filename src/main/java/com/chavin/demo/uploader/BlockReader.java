package com.chavin.demo.uploader;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockReader {
    public interface BlockCallback {
        void onFinished(long offset, byte @Nullable [] bytes);
    }

    public interface BlockFilter {
        boolean ignore(long offset);
    }

    private static final int DEF_POOL_SIZE = 1;
    private static final int DEF_BLOCK_SIZE = 8192;
    private final File mFile;
    private final BlockCallback mCallback;
    private final int mPoolSize;
    private final int mBlockSize;
    private final AtomicInteger mRunning = new AtomicInteger(0);
    private final Queue<ReadTask> mQueue = new LinkedBlockingQueue<>();

    public BlockReader(@NotNull File file, @NotNull BlockCallback callback) {
        this(file, callback, DEF_POOL_SIZE, DEF_BLOCK_SIZE);
    }

    public BlockReader(@NotNull File file, @NotNull BlockCallback callback, int poolSize, int blockSize) {
        mFile = file;
        mCallback = callback;
        mPoolSize = poolSize;
        mBlockSize = blockSize;
    }

    /**
     * 开始分块读取
     *
     * @param filter 可以自定义忽略已经之前已经上传过的块
     */
    public void start(@Nullable BlockFilter filter) {
        Observable.empty().observeOn(Schedulers.computation()).doOnComplete(() -> {
            long length = mFile.length();
            for (long offset = 0; offset < length; offset += mBlockSize) {
                if (null != filter && filter.ignore(offset)) {
                    continue;
                }
                mQueue.offer(new ReadTask(offset));
            }
            for (int i = 0; i < Math.min(mPoolSize, mQueue.size()); i++) {
                Observable.empty().observeOn(Schedulers.io()).doOnComplete(this::schedule).subscribe();
            }
        }).subscribe();
    }

    private void schedule() {
        if (mRunning.get() >= mPoolSize) {
            return;
        }
        ReadTask task;
        synchronized (mQueue) {
            if (mRunning.get() >= mPoolSize) {
                return;
            }
            task = mQueue.poll();
            if (null != task) {
                mRunning.incrementAndGet();
            }
        }
        if (null != task) {
            task.run();
        }
    }

    private class ReadTask implements Action {
        public static final String RAF_MODE = "r";
        private final long mOffset;

        private ReadTask(long offset) {
            mOffset = offset;
        }

        @Override
        public void run() {
            try (RandomAccessFile raf = new RandomAccessFile(mFile, RAF_MODE);
                 ByteArrayOutputStream out = new ByteArrayOutputStream(mBlockSize)) {
                raf.seek(mOffset);
                byte[] buf = new byte[DEF_BLOCK_SIZE];
                long cnt = 0;
                for (int bytes = raf.read(buf); bytes != -1 && cnt < mBlockSize; bytes = raf.read(buf)) {
                    out.write(buf, 0, bytes);
                    cnt += bytes;
                }
                out.flush();
                mCallback.onFinished(mOffset, out.toByteArray());
            } catch (IOException e) {
                mCallback.onFinished(mOffset, null);
            } finally {
                mRunning.decrementAndGet();
                schedule();
            }
        }
    }
}
