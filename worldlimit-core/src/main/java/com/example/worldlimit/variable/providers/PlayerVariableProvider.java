package com.example.worldlimit.variable.providers;

import com.example.worldlimit.api.variable.VariableProvider;
import org.bukkit.entity.Player;

public class PlayerVariableProvider implements VariableProvider {
    private final String name;
    private final String description;
    private final VariableType type;
    private final ValueGetter valueGetter;

    @FunctionalInterface
    private interface ValueGetter {
        String get(Player player);
    }

    private PlayerVariableProvider(String name, String description, VariableType type, ValueGetter valueGetter) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.valueGetter = valueGetter;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getValue(Player player) {
        return valueGetter.get(player);
    }

    @Override
    public Double getNumericValue(Player player) {
        if (type != VariableType.NUMBER) {
            return null;
        }
        try {
            return Double.parseDouble(getValue(player));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public VariableType getType() {
        return type;
    }

    // 内置变量提供者工厂方法
    public static VariableProvider[] createDefaultProviders() {
        return new VariableProvider[] {
            new PlayerVariableProvider(
                "player_health",
                "玩家当前生命值",
                VariableType.NUMBER,
                player -> String.valueOf(player.getHealth())
            ),
            new PlayerVariableProvider(
                "player_max_health",
                "玩家最大生命值",
                VariableType.NUMBER,
                player -> String.valueOf(player.getMaxHealth())
            ),
            new PlayerVariableProvider(
                "player_food",
                "玩家饥饿值",
                VariableType.NUMBER,
                player -> String.valueOf(player.getFoodLevel())
            ),
            new PlayerVariableProvider(
                "player_level",
                "玩家经验等级",
                VariableType.NUMBER,
                player -> String.valueOf(player.getLevel())
            ),
            new PlayerVariableProvider(
                "player_exp",
                "玩家经验值",
                VariableType.NUMBER,
                player -> String.valueOf(player.getExp())
            ),
            new PlayerVariableProvider(
                "player_world",
                "玩家所在世界名称",
                VariableType.TEXT,
                player -> player.getWorld().getName()
            ),
            new PlayerVariableProvider(
                "player_gamemode",
                "玩家游戏模式",
                VariableType.TEXT,
                player -> player.getGameMode().name()
            ),
            new PlayerVariableProvider(
                "player_flying",
                "玩家是否在飞行",
                VariableType.BOOLEAN,
                player -> String.valueOf(player.isFlying())
            )
        };
    }
} 