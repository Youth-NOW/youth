package com.example.worldlimit.async;

import com.example.worldlimit.WorldLimitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class AsyncManager {
    private final WorldLimitPlugin plugin;
    private ThreadPoolExecutor executor;
    private final PerformanceStats stats;
    private static final int INITIAL_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = INITIAL_POOL_SIZE * 4;
    private static final int QUEUE_CAPACITY = 1000;
    private static final long KEEP_ALIVE_TIME = 60L;

    public AsyncManager(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.stats = new PerformanceStats();
        initializeExecutor();
    }

    private void initializeExecutor() {
        this.executor = new ThreadPoolExecutor(
            INITIAL_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadFactory() {
                private int count = 1;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("WorldLimit-Worker-" + count++);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void submitTask(AsyncTask task) {
        if (executor.isShutdown()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        executor.submit(() -> {
            try {
                task.execute();
                stats.recordTaskCompletion(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "异步任务执行失败", e);
                if (task.getErrorCallback() != null) {
                    task.getErrorCallback().accept(e);
                }
            }
        });
    }

    public void expandThreadPool() {
        int currentMax = executor.getMaximumPoolSize();
        int newMax = Math.min(currentMax * 2, MAX_POOL_SIZE);
        
        if (currentMax < newMax) {
            executor.setMaximumPoolSize(newMax);
            plugin.getLogger().info(String.format(
                "线程池已扩容: %d -> %d (最大容量: %d)",
                currentMax, newMax, MAX_POOL_SIZE
            ));
        } else {
            plugin.getLogger().warning("线程池已达到最大容量，无法继续扩容");
        }
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public PerformanceStats getPerformanceStats() {
        return stats;
    }

    public boolean isOverloaded() {
        return executor.getQueue().size() > QUEUE_CAPACITY * 0.8;
    }

    public class PerformanceStats {
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        private final AtomicLong totalProcessingTime = new AtomicLong(0);

        public void recordTaskCompletion(long processingTime) {
            completedTasks.incrementAndGet();
            totalProcessingTime.addAndGet(processingTime);
        }

        public void sendTo(Player player) {
            int completed = completedTasks.get();
            long total = totalProcessingTime.get();
            double avgTime = completed > 0 ? (double) total / completed : 0;

            player.sendMessage("§a=== 性能统计 ===");
            player.sendMessage("§f活动线程数: " + executor.getActiveCount());
            player.sendMessage("§f队列大小: " + executor.getQueue().size());
            player.sendMessage("§f已完成任务: " + completed);
            player.sendMessage("§f平均处理时间: " + String.format("%.2f", avgTime) + "ms");
        }
    }
} 