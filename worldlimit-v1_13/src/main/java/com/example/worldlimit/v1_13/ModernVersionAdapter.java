package com.example.worldlimit.v1_13;

import com.example.worldlimit.api.version.VersionAdapter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ModernVersionAdapter implements VersionAdapter {
    @Override
    public String getAdapterName() {
        return "Modern 1.13+";
    }

    @Override
    public boolean matchItem(ItemStack item, String matchData) {
        if (item == null) return false;
        
        // 1.13+ 使用命名空间ID (namespace:key)
        try {
            String[] parts = matchData.split(":");
            if (parts.length < 2) return false;
            
            String type = item.getType().getKey().toString();
            return type.equals(matchData);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    @Override
    public double getWorldBorderSize(World world) {
        return world.getWorldBorder().getSize();
    }

    @Override
    public boolean supportsFeature(String feature) {
        switch (feature.toLowerCase()) {
            case "actionbar":
            case "worldborder":
            case "modern_items":
                return true;
            default:
                return false;
        }
    }
} 