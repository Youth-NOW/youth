package com.example.worldlimit.listener;

import com.example.worldlimit.WorldLimitPlugin;
import com.example.worldlimit.async.WorldCheckTask;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class WorldChangeListener implements Listener {
    private final WorldLimitPlugin plugin;
    private final boolean debug;

    public WorldChangeListener(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfigManager().isDebugEnabled();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        checkWorldAccess(event.getPlayer(), event.getPlayer().getWorld().getName(), null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            checkWorldAccess(event.getPlayer(), event.getTo().getWorld().getName(), 
                result -> {
                    if (!result) {
                        event.setCancelled(true);
                    }
                });
        }
    }

    private void checkWorldAccess(Player player, String worldName, java.util.function.Consumer<Boolean> callback) {
        // 检查是否有调试权限
        if (player.hasPermission("worldlimit.bypass")) {
            if (debug) {
                player.sendMessage(ChatColor.YELLOW + "[调试] 你有权限绕过世界限制");
            }
            if (callback != null) {
                callback.accept(true);
            }
            return;
        }

        // 获取世界配置
        ConfigurationSection worldConfig = plugin.getConfigManager().getWorldConfig(worldName);
        if (worldConfig == null || !worldConfig.getBoolean("enabled", true)) {
            if (callback != null) {
                callback.accept(true);
            }
            return;
        }

        // 创建异步检查任务
        WorldCheckTask task = new WorldCheckTask(
            plugin,
            player.getName(),
            worldName,
            result -> {
                if (!result) {
                    String message = worldConfig.getString("message.deny", 
                            plugin.getConfigManager().getDefaultDenyMessage());
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
                if (callback != null) {
                    callback.accept(result);
                }
            },
            error -> {
                plugin.getLogger().warning("检查世界访问权限时发生错误: " + error.getMessage());
                if (callback != null) {
                    callback.accept(true); // 出错时允许访问
                }
            }
        );

        // 提交异步任务
        plugin.getAsyncManager().submitTask(task);
    }
} 