package com.example.worldlimit.monitor;

import com.example.worldlimit.WorldLimitPlugin;
import com.example.worldlimit.async.AsyncManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ExceptionMonitor {
    private final WorldLimitPlugin plugin;
    private final Map<String, AtomicInteger> exceptionCounters;
    private final Map<String, Long> isolatedVariables;
    private static final int EXCEPTION_THRESHOLD = 10;
    private static final long ISOLATION_DURATION = 300000; // 5分钟

    public ExceptionMonitor(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.exceptionCounters = new ConcurrentHashMap<>();
        this.isolatedVariables = new ConcurrentHashMap<>();
    }

    public void handleException(String type, Throwable e) {
        String message = e.getMessage();
        plugin.getLogger().log(Level.WARNING, type + ": " + message);

        // 增加异常计数
        AtomicInteger counter = exceptionCounters.computeIfAbsent(type, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        // 根据异常类型采取不同措施
        switch (type) {
            case "VERSION":
                handleVersionException(count, message);
                break;
            case "VARIABLE":
                handleVariableException(count, message);
                break;
            case "THREAD":
                handleThreadException(count, message);
                break;
        }

        // 定期重置计数器
        if (count == EXCEPTION_THRESHOLD) {
            plugin.getServer().getScheduler().runTaskLater(plugin, 
                () -> counter.set(0), 6000L); // 5分钟后重置
        }
    }

    private void handleVersionException(int count, String message) {
        if (count >= EXCEPTION_THRESHOLD) {
            plugin.getLogger().warning("[监控] 检测到频繁的版本适配异常，正在尝试降级...");
            // 通知版本管理器降级到基础模式
            plugin.getVersionManager().downgradeToBasicMode();
        }
    }

    private void handleVariableException(int count, String message) {
        // 从错误消息中提取变量名
        String varName = extractVariableName(message);
        if (varName != null && count >= 3) { // 连续3次错误后隔离变量
            isolateVariable(varName);
            suggestAlternativeVariable(varName);
        }
    }

    private void handleThreadException(int count, String message) {
        AsyncManager asyncManager = plugin.getAsyncManager();
        if (count >= EXCEPTION_THRESHOLD) {
            plugin.getLogger().warning("[监控] 检测到线程池阻塞，正在扩容...");
            // 通知异步管理器扩容
            asyncManager.expandThreadPool();
        }
    }

    private String extractVariableName(String message) {
        // 从错误消息中提取变量名的逻辑
        if (message.contains("变量不可用: ")) {
            return message.substring(message.indexOf("变量不可用: ") + 7);
        }
        return null;
    }

    private void isolateVariable(String varName) {
        if (!isolatedVariables.containsKey(varName)) {
            isolatedVariables.put(varName, System.currentTimeMillis() + ISOLATION_DURATION);
            plugin.getLogger().warning("[监控] 变量 " + varName + " 已被隔离，将在5分钟后重试");
        }
    }

    private void suggestAlternativeVariable(String varName) {
        // 根据变量名推荐替代变量
        String suggestion = null;
        if (varName.contains("health")) {
            suggestion = "%player_health%";
        } else if (varName.contains("level")) {
            suggestion = "%player_level%";
        } else if (varName.contains("exp")) {
            suggestion = "%player_exp%";
        }

        if (suggestion != null) {
            plugin.getLogger().warning("[建议] 考虑使用 " + suggestion + " 作为替代变量");
        }
    }

    public boolean isVariableIsolated(String varName) {
        Long isolationEnd = isolatedVariables.get(varName);
        if (isolationEnd != null) {
            if (System.currentTimeMillis() > isolationEnd) {
                // 隔离期结束，移除隔离
                isolatedVariables.remove(varName);
                return false;
            }
            return true;
        }
        return false;
    }

    public void clearIsolatedVariables() {
        isolatedVariables.clear();
    }

    public Map<String, Integer> getExceptionStats() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        exceptionCounters.forEach((type, counter) -> 
            stats.put(type, counter.get()));
        return stats;
    }
} 