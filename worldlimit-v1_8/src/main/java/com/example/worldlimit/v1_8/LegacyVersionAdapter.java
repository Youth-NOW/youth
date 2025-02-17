package com.example.worldlimit.v1_8;

import com.example.worldlimit.api.version.VersionAdapter;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class LegacyVersionAdapter implements VersionAdapter {
    @Override
    public String getAdapterName() {
        return "Legacy 1.8-1.12";
    }

    @Override
    public boolean matchItem(ItemStack item, String matchData) {
        if (item == null) return false;
        String[] parts = matchData.split(":");
        if (parts.length != 2) return false;
        
        String material = parts[0];
        short data = Short.parseShort(parts[1]);
        
        return item.getType().name().equals(material) && 
               (data == -1 || item.getDurability() == data);
    }

    @Override
    public void sendActionBar(Player player, String message) {
        try {
            // 获取NMS版本
            String version = player.getClass().getPackage().getName().split("\\.")[3];
            
            // 获取必要的类
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> iChatBaseComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            
            // 创建IChatBaseComponent
            Method a = chatSerializerClass.getDeclaredMethod("a", String.class);
            Object cbc = a.invoke(null, "{\"text\": \"" + message + "\"}");
            
            // 创建PacketPlayOutChat
            Constructor<?> packetConstructor = packetPlayOutChatClass.getDeclaredConstructor(iChatBaseComponentClass, byte.class);
            Object packet = packetConstructor.newInstance(cbc, (byte) 2);
            
            // 获取玩家连接并发送数据包
            Object craftPlayer = craftPlayerClass.cast(player);
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacket.invoke(connection, packet);
        } catch (Exception e) {
            // 如果反射失败，使用聊天消息作为备用方案
            player.sendMessage(message);
        }
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
                return true;
            default:
                return false;
        }
    }
} 