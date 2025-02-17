package com.example.worldlimit.api.version;

import org.bukkit.Server;

/**
 * 服务器版本检测器
 */
public interface VersionDetector {
    /**
     * 获取服务器版本
     * @param server Bukkit服务器实例
     * @return 版本字符串 (例如: "1.8.8", "1.12.2", "1.20.1")
     */
    String getVersion(Server server);

    /**
     * 检查是否为现代版本 (1.13+)
     * @param server Bukkit服务器实例
     * @return 如果是1.13或更高版本返回true
     */
    boolean isModernVersion(Server server);

    /**
     * 获取NMS版本字符串
     * @param server Bukkit服务器实例
     * @return NMS版本字符串 (例如: "v1_8_R3", "v1_12_R1")
     */
    String getNMSVersion(Server server);
} 