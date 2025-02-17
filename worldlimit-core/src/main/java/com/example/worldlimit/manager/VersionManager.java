package com.example.worldlimit.manager;

import com.example.worldlimit.api.version.VersionAdapter;
import com.example.worldlimit.api.version.VersionDetector;
import com.example.worldlimit.v1_8.LegacyVersionAdapter;
import com.example.worldlimit.v1_8.LegacyVersionDetector;
import com.example.worldlimit.v1_13.ModernVersionAdapter;
import com.example.worldlimit.v1_13.ModernVersionDetector;
import org.bukkit.plugin.java.JavaPlugin;

public class VersionManager {
    private final JavaPlugin plugin;
    private VersionDetector detector;
    private VersionAdapter adapter;
    private boolean isBasicMode = false;

    public VersionManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            // 首先尝试使用现代版本检测器
            this.detector = new ModernVersionDetector();
            if (detector.isModernVersion(plugin.getServer())) {
                this.adapter = new ModernVersionAdapter();
                return true;
            }

            // 如果不是现代版本，使用旧版本检测器
            this.detector = new LegacyVersionDetector();
            if (!detector.isModernVersion(plugin.getServer())) {
                this.adapter = new LegacyVersionAdapter();
                return true;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize version system: " + e.getMessage());
            return false;
        }
    }

    public void downgradeToBasicMode() {
        if (isBasicMode) {
            return; // 已经在基础模式下
        }

        plugin.getLogger().warning("正在切换到基础模式...");
        
        // 切换到旧版本适配器，因为它具有更好的兼容性
        this.detector = new LegacyVersionDetector();
        this.adapter = new LegacyVersionAdapter();
        this.isBasicMode = true;

        // 禁用高级功能
        disableAdvancedFeatures();
        
        plugin.getLogger().info("已切换到基础模式，部分高级功能将被禁用");
    }

    private void disableAdvancedFeatures() {
        // 禁用现代版本特有的功能
        if (adapter instanceof LegacyVersionAdapter) {
            plugin.getLogger().info("- 禁用现代物品ID支持");
            plugin.getLogger().info("- 禁用高级动作栏消息");
            plugin.getLogger().info("- 切换到基础NBT处理");
        }
    }

    public boolean isBasicMode() {
        return isBasicMode;
    }

    public VersionDetector getDetector() {
        return detector;
    }

    public VersionAdapter getAdapter() {
        return adapter;
    }
} 