package com.example.worldlimit.v1_13;

import com.example.worldlimit.api.version.VersionDetector;
import org.bukkit.Server;

public class ModernVersionDetector implements VersionDetector {
    @Override
    public String getVersion(Server server) {
        String version = server.getBukkitVersion();
        return version.split("-")[0];
    }

    @Override
    public boolean isModernVersion(Server server) {
        return true; // 1.13+ 版本始终返回true
    }

    @Override
    public String getNMSVersion(Server server) {
        String packageName = server.getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }
} 