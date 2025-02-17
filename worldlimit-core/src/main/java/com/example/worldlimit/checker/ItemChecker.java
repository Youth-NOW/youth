package com.example.worldlimit.checker;

import com.example.worldlimit.WorldLimitPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemChecker {
    private final WorldLimitPlugin plugin;
    private final boolean debug;

    public ItemChecker(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfigManager().isDebugEnabled();
    }

    public boolean checkCondition(Player player, ConfigurationSection condition) {
        String itemId = condition.getString("item");
        if (itemId == null || itemId.isEmpty()) {
            plugin.getLogger().warning("物品条件配置错误：未指定物品ID");
            return true;
        }

        int amount = condition.getInt("amount", 1);
        String name = condition.getString("name");
        List<String> lore = condition.getStringList("lore");

        if (debug) {
            player.sendMessage(ChatColor.YELLOW + "[调试] 检查物品: " + itemId + " x" + amount);
            if (name != null) {
                player.sendMessage(ChatColor.YELLOW + "[调试] 物品名称: " + name);
            }
        }

        // 检查玩家背包中的每个物品
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && matchItem(item, itemId, name, lore)) {
                found += item.getAmount();
                if (found >= amount) {
                    return true;
                }
            }
        }

        if (debug) {
            player.sendMessage(ChatColor.YELLOW + "[调试] 找到物品数量: " + found + "/" + amount);
        }

        return false;
    }

    private boolean matchItem(ItemStack item, String itemId, String name, List<String> lore) {
        // 检查物品ID和数据值
        String[] parts = itemId.split(":");
        String material = parts[0];
        short data = parts.length > 1 ? Short.parseShort(parts[1]) : 0;

        // 检查物品类型
        if (!item.getType().name().equals(material)) {
            return false;
        }

        // 检查数据值（如果指定）
        if (parts.length > 1 && item.getDurability() != data) {
            return false;
        }

        // 如果需要检查名称或描述
        if (name != null || (lore != null && !lore.isEmpty())) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            }

            // 检查名称
            if (name != null) {
                String itemName = meta.getDisplayName();
                if (!ChatColor.translateAlternateColorCodes('&', name).equals(itemName)) {
                    return false;
                }
            }

            // 检查描述
            if (lore != null && !lore.isEmpty()) {
                List<String> itemLore = meta.getLore();
                if (itemLore == null || itemLore.size() != lore.size()) {
                    return false;
                }

                for (int i = 0; i < lore.size(); i++) {
                    String expectedLore = ChatColor.translateAlternateColorCodes('&', lore.get(i));
                    if (!expectedLore.equals(itemLore.get(i))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
} 