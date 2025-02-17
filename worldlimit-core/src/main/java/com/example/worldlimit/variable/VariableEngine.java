package com.example.worldlimit.variable;

import com.example.worldlimit.WorldLimitPlugin;
import com.example.worldlimit.api.variable.VariableProvider;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VariableEngine {
    private final WorldLimitPlugin plugin;
    private final Map<String, VariableProvider> providers;
    private final Map<String, CachedValue> cache;
    private final boolean papiEnabled;
    private long cacheDuration;
    private boolean cacheEnabled;

    private static class CachedValue {
        final String value;
        final long expiry;

        CachedValue(String value, long expiry) {
            this.value = value;
            this.expiry = expiry;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }

    public VariableEngine(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.providers = new ConcurrentHashMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        
        // 从配置文件加载缓存设置
        loadSettings();
    }

    private void loadSettings() {
        this.cacheEnabled = plugin.getConfigManager().isVariableCacheEnabled();
        this.cacheDuration = plugin.getConfigManager().getVariableCacheDuration();
    }

    public void registerProvider(VariableProvider provider) {
        providers.put(provider.getName(), provider);
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("注册变量提供者: " + provider.getName());
        }
    }

    public String parseVariable(String variable, Player player) {
        if (variable == null || variable.isEmpty()) {
            return "";
        }

        // 移除百分号
        String varName = variable.startsWith("%") && variable.endsWith("%") 
                ? variable.substring(1, variable.length() - 1) 
                : variable;

        // 检查缓存
        String cacheKey = player.getName() + ":" + varName;
        if (cacheEnabled) {
            CachedValue cached = cache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.value;
            }
        }

        // 首先检查自定义变量提供者
        VariableProvider provider = providers.get(varName);
        if (provider != null && provider.isAvailable()) {
            String value = provider.getValue(player);
            cacheValue(cacheKey, value);
            return value;
        }

        // 然后尝试PAPI
        if (papiEnabled) {
            String value = PlaceholderAPI.setPlaceholders(player, "%" + varName + "%");
            if (!value.equals("%" + varName + "%")) {
                cacheValue(cacheKey, value);
                return value;
            }
        }

        return "";
    }

    private void cacheValue(String key, String value) {
        if (cacheEnabled) {
            long expiry = System.currentTimeMillis() + cacheDuration;
            cache.put(key, new CachedValue(value, expiry));
        }
    }

    public Double parseNumericVariable(String variable, Player player) {
        String value = parseVariable(variable, player);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void clearCache() {
        cache.clear();
    }

    public void clearCache(Player player) {
        String prefix = player.getName() + ":";
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public boolean isVariableAvailable(String variable) {
        if (variable == null || variable.isEmpty()) {
            return false;
        }

        String varName = variable.startsWith("%") && variable.endsWith("%") 
                ? variable.substring(1, variable.length() - 1) 
                : variable;

        // 检查自定义变量提供者
        VariableProvider provider = providers.get(varName);
        if (provider != null) {
            return provider.isAvailable();
        }

        // 检查PAPI是否可用
        return papiEnabled;
    }

    public void reloadSettings() {
        loadSettings();
        clearCache();
    }
} 