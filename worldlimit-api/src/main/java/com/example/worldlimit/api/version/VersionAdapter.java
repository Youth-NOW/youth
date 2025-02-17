package com.example.worldlimit.api.version;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 版本适配器接口
 * 用于处理不同版本的Minecraft服务器特定功能
 */
public interface VersionAdapter {
    /**
     * 获取适配器版本名称
     * @return 适配器名称 (例如: "Legacy 1.8-1.12", "Modern 1.13+")
     */
    String getAdapterName();

    /**
     * 检查物品是否匹配
     * @param item 要检查的物品
     * @param matchData 匹配数据
     * @return 是否匹配
     */
    boolean matchItem(ItemStack item, String matchData);

    /**
     * 发送动作栏消息
     * @param player 目标玩家
     * @param message 消息内容
     */
    void sendActionBar(Player player, String message);

    /**
     * 获取世界边界大小
     * @param world 目标世界
     * @return 边界大小
     */
    double getWorldBorderSize(World world);

    /**
     * 检查是否支持特定功能
     * @param feature 功能名称
     * @return 是否支持
     */
    boolean supportsFeature(String feature);
} 