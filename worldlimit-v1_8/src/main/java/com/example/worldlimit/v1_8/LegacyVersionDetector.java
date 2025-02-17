package com.example.worldlimit.v1_8;

import com.example.worldlimit.api.version.VersionDetector;
import org.bukkit.Server;

public class LegacyVersionDetector implements VersionDetector {
    @Override
    public String getVersion(Server server) {
        String version = server.getBukkitVersion();
        return version.split("-")[0];
    }

    @Override
    public boolean isModernVersion(Server server) {
        String version = getVersion(server);
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            int major = Integer.parseInt(parts[1]);
            return major >= 13;
        }
        return false;
    }

    @Override
    public String getNMSVersion(Server server) {
        String packageName = server.getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }
} 