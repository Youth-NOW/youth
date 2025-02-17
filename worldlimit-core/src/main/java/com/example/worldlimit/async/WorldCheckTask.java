package com.example.worldlimit.async;

import com.example.worldlimit.WorldLimitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * 世界访问检查异步任务
 */
public class WorldCheckTask extends BaseAsyncTask {
    private final WorldLimitPlugin plugin;
    private final String playerName;
    private final String worldName;
    private final Consumer<Boolean> callback;

    public WorldCheckTask(WorldLimitPlugin plugin, String playerName, String worldName, 
                         Consumer<Boolean> callback, Consumer<Exception> errorCallback) {
        super(errorCallback);
        this.plugin = plugin;
        this.playerName = playerName;
        this.worldName = worldName;
        this.callback = callback;
    }

    @Override
    public void execute() throws Exception {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            throw new IllegalStateException("玩家不在线: " + playerName);
        }

        // 获取世界配置
        ConfigurationSection worldConfig = plugin.getConfigManager().getWorldConfig(worldName);
        if (worldConfig == null || !worldConfig.getBoolean("enabled", true)) {
            runCallback(true);
            return;
        }

        // 检查权限
        if (player.hasPermission("worldlimit.bypass")) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                sendDebugMessage(player, "你有权限绕过世界限制");
            }
            runCallback(true);
            return;
        }

        // 检查条件
        boolean result = true;
        for (Object conditionObj : worldConfig.getList("conditions", java.util.Collections.emptyList())) {
            if (!(conditionObj instanceof java.util.Map)) continue;
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> condition = (java.util.Map<String, Object>) conditionObj;
            String type = (String) condition.get("type");
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                sendDebugMessage(player, "检查条件: " + (type != null ? type : "无类型"));
            }

            // 根据类型执行不同的检查
            boolean passed = checkCondition(player, type, condition);
            if (!passed) {
                result = false;
                break;
            }
        }

        runCallback(result);
    }

    private void runCallback(boolean result) {
        if (callback != null) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        }
    }

    private void sendDebugMessage(Player player, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> 
            player.sendMessage(ChatColor.YELLOW + "[调试] " + message));
    }

    private boolean checkCondition(Player player, String type, java.util.Map<String, Object> condition) {
        if (type == null || type.isEmpty()) {
            // 自动检测类型
            if (condition.containsKey("permission")) {
                type = "permission";
            } else if (condition.containsKey("item")) {
                type = "item";
            } else if (condition.containsKey("variable")) {
                type = "variable";
            } else if (condition.containsKey("time")) {
                type = "cooldown";
            }
        }

        ConfigurationSection section = plugin.getConfig().createSection("temp", condition);
        switch (type != null ? type.toLowerCase() : "") {
            case "variable":
                return plugin.getVariableChecker().checkCondition(player, section);
            case "permission":
                return checkPermission(player, section);
            case "item":
                return plugin.getItemChecker().checkCondition(player, section);
            case "cooldown":
                return plugin.getCooldownChecker().checkCondition(player, section, worldName);
            default:
                plugin.getLogger().warning("未知的条件类型: " + type);
                return true;
        }
    }

    private boolean checkPermission(Player player, ConfigurationSection condition) {
        String permission = condition.getString("permission");
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            sendDebugMessage(player, "检查权限: " + permission);
        }

        return player.hasPermission(permission);
    }
} 