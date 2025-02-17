package com.example.worldlimit.variable;

import com.example.worldlimit.WorldLimitPlugin;
import com.example.worldlimit.api.variable.Condition;
import com.example.worldlimit.variable.VariableEngine;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class VariableChecker {
    private final WorldLimitPlugin plugin;
    private final VariableEngine engine;

    public VariableChecker(WorldLimitPlugin plugin, VariableEngine engine) {
        this.plugin = plugin;
        this.engine = engine;
    }

    public boolean checkCondition(Player player, ConfigurationSection config) {
        if (!config.getString("type", "").equals("variable")) {
            return true;
        }

        String variable = config.getString("variable");
        if (variable == null || variable.isEmpty()) {
            plugin.getLogger().warning("变量条件配置错误：未指定变量名");
            return false;
        }

        // 检查变量是否可用
        if (!engine.isVariableAvailable(variable)) {
            String action = plugin.getConfigManager().getWorldRules()
                    .getString("settings.variable_check.unavailable_action", "DENY");
            switch (action.toUpperCase()) {
                case "ALLOW":
                    return true;
                case "SKIP":
                    return true;
                case "DENY":
                default:
                    return false;
            }
        }

        // 创建条件
        Condition condition;
        String operator = config.getString("operator", "==");
        
        if (operator.equalsIgnoreCase("range")) {
            double min = config.getDouble("min", Double.MIN_VALUE);
            double max = config.getDouble("max", Double.MAX_VALUE);
            condition = new SimpleCondition(min, max);
        } else {
            String value = config.getString("value", "");
            condition = new SimpleCondition(Condition.Operator.fromString(operator), value);
        }

        // 获取变量值并检查
        String value = engine.parseVariable(variable, player);
        Double numericValue = engine.parseNumericVariable(variable, player);

        // 如果是数值类型的操作符，优先使用数值比较
        if (isNumericOperator(operator)) {
            return numericValue != null && condition.testNumeric(numericValue);
        }

        return condition.test(value);
    }

    private boolean isNumericOperator(String operator) {
        switch (operator.toLowerCase()) {
            case ">":
            case "<":
            case ">=":
            case "<=":
            case "range":
                return true;
            default:
                return false;
        }
    }
} 