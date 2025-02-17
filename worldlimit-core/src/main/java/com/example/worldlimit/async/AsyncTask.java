package com.example.worldlimit.async;

import java.util.function.Consumer;

/**
 * 异步任务接口
 */
public interface AsyncTask {
    /**
     * 执行任务
     * @throws Exception 如果任务执行失败
     */
    void execute() throws Exception;

    /**
     * 获取错误回调
     * @return 错误处理回调函数
     */
    Consumer<Exception> getErrorCallback();
} 