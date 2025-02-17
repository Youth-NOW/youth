package com.example.worldlimit.async;

import java.util.function.Consumer;

/**
 * 异步任务基础实现类
 */
public abstract class BaseAsyncTask implements AsyncTask {
    private final Consumer<Exception> errorCallback;

    protected BaseAsyncTask() {
        this(null);
    }

    protected BaseAsyncTask(Consumer<Exception> errorCallback) {
        this.errorCallback = errorCallback;
    }

    @Override
    public Consumer<Exception> getErrorCallback() {
        return errorCallback;
    }
} 