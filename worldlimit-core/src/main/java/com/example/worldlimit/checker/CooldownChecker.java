package com.example.worldlimit.checker;

import com.example.worldlimit.WorldLimitPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownChecker {
    private final WorldLimitPlugin plugin;
    private final boolean debug;
    private final Map<String, Map<UUID, Long>> cooldowns;

    public CooldownChecker(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfigManager().isDebugEnabled();
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public boolean checkCondition(Player player, ConfigurationSection condition, String worldName) {
        // 检查绕过权限
        String bypassPermission = condition.getString("permission_bypass");
        if (bypassPermission != null && player.hasPermission(bypassPermission)) {
            if (debug) {
                player.sendMessage(ChatColor.YELLOW + "[调试] 你有权限绕过冷却时间");
            }
            return true;
        }

        // 获取冷却时间
        int cooldownTime = condition.getInt("time", 60);
        if (cooldownTime <= 0) {
            return true;
        }

        // 获取玩家的冷却记录
        Map<UUID, Long> worldCooldowns = cooldowns.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>());
        Long lastAccess = worldCooldowns.get(player.getUniqueId());

        // 如果没有记录或冷却已过期
        if (lastAccess == null) {
            updateCooldown(player, worldName);
            return true;
        }

        // 计算剩余时间
        long currentTime = System.currentTimeMillis();
        long elapsedTime = (currentTime - lastAccess) / 1000; // 转换为秒
        long remainingTime = cooldownTime - elapsedTime;

        if (remainingTime <= 0) {
            updateCooldown(player, worldName);
            return true;
        }

        if (debug) {
            player.sendMessage(ChatColor.YELLOW + "[调试] 冷却剩余时间: " + remainingTime + "秒");
        }

        // 发送冷却消息
        String message = plugin.getConfigManager().getDefaultCooldownMessage()
                .replace("{time}", String.valueOf(remainingTime));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        return false;
    }

    private void updateCooldown(Player player, String worldName) {
        Map<UUID, Long> worldCooldowns = cooldowns.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>());
        worldCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void clearCooldown(Player player, String worldName) {
        Map<UUID, Long> worldCooldowns = cooldowns.get(worldName);
        if (worldCooldowns != null) {
            worldCooldowns.remove(player.getUniqueId());
        }
    }

    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    public void clearExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        cooldowns.forEach((world, playerCooldowns) -> {
            playerCooldowns.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) / 1000 > 
                plugin.getConfigManager().getWorldRules()
                    .getInt("worlds." + world + ".cooldown.time", 60)
            );
        });
    }
} 