package com.example.worldlimit.api.variable;

import org.bukkit.entity.Player;

/**
 * 变量提供者接口
 */
public interface VariableProvider {
    /**
     * 获取变量名称
     * @return 变量名称（例如：player_health, player_level）
     */
    String getName();

    /**
     * 获取变量描述
     * @return 变量描述
     */
    String getDescription();

    /**
     * 获取变量值
     * @param player 目标玩家
     * @return 变量值
     */
    String getValue(Player player);

    /**
     * 获取变量的数值
     * @param player 目标玩家
     * @return 数值，如果变量不是数值类型则返回null
     */
    Double getNumericValue(Player player);

    /**
     * 检查变量是否可用
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取变量类型
     * @return 变量类型
     */
    VariableType getType();

    /**
     * 变量类型枚举
     */
    enum VariableType {
        NUMBER,     // 数值类型（用于比较大小）
        TEXT,       // 文本类型（用于相等比较）
        BOOLEAN     // 布尔类型（用于条件判断）
    }
} 