package com.example.worldlimit.command;

import com.example.worldlimit.WorldLimitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {
    private final WorldLimitPlugin plugin;
    private final DebugCommand debugCommand;
    private final List<String> subCommands = Arrays.asList(
        "debug", "reload", "help", "add", "remove", "list", "info", "lang"
    );

    public MainCommand(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.debugCommand = new DebugCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "debug":
                return debugCommand.onCommand(sender, command, label, subArgs);
            case "reload":
                return handleReload(sender);
            case "add":
                return handleAdd(sender, subArgs);
            case "remove":
                return handleRemove(sender, subArgs);
            case "list":
                return handleList(sender, subArgs);
            case "info":
                return handleInfo(sender, subArgs);
            case "lang":
                return handleLanguage(sender, subArgs);
            case "help":
            default:
                showHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("worldlimit.admin")) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.no_permission"));
            return true;
        }
        plugin.reloadConfig();
        plugin.getLanguageManager().loadLanguages();
        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.reload_success"));
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldlimit.admin")) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.no_permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.usage.world_add"));
            sender.sendMessage(ChatColor.YELLOW + "item, permission, variable, cooldown");
            return true;
        }

        String worldName = args[0];
        String type = args[1].toLowerCase();
        ConfigurationSection worldConfig = plugin.getConfigManager().getWorldConfig(worldName);
        
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.world.not_found")
                    .replace("%world%", worldName));
            return true;
        }

        switch (type) {
            case "item":
                return handleAddItem(sender, worldName, Arrays.copyOfRange(args, 2, args.length));
            case "permission":
                return handleAddPermission(sender, worldName, Arrays.copyOfRange(args, 2, args.length));
            case "variable":
                return handleAddVariable(sender, worldName, Arrays.copyOfRange(args, 2, args.length));
            case "cooldown":
                return handleAddCooldown(sender, worldName, Arrays.copyOfRange(args, 2, args.length));
            default:
                sender.sendMessage(ChatColor.RED + "未知的条件类型: " + type);
                return true;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getConditions(String worldName) {
        List<Map<?, ?>> rawList = plugin.getConfigManager().getWorldRules()
                .getMapList("worlds." + worldName + ".conditions");
        return rawList.stream()
                .map(map -> (Map<String, Object>) map)
                .collect(Collectors.toList());
    }

    private boolean handleAddItem(CommandSender sender, String worldName, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.usage.add_item"));
            return true;
        }

        String itemId = args[0];
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的数量: " + args[1]);
            return true;
        }

        List<Map<String, Object>> conditions = getConditions(worldName);
        
        Map<String, Object> newCondition = plugin.getConfig().createSection("temp").getValues(false);
        newCondition.put("type", "item");
        newCondition.put("item", itemId);
        newCondition.put("amount", amount);
        
        if (args.length > 2) {
            newCondition.put("name", args[2]);
        }
        if (args.length > 3) {
            newCondition.put("lore", Arrays.asList(Arrays.copyOfRange(args, 3, args.length)));
        }

        conditions.add(newCondition);
        plugin.getConfigManager().getWorldRules().set("worlds." + worldName + ".conditions", conditions);
        plugin.getConfigManager().saveWorldRules();

        sender.sendMessage(ChatColor.GREEN + "已添加物品检查条件");
        return true;
    }

    private boolean handleAddPermission(CommandSender sender, String worldName, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.usage.add_permission"));
            return true;
        }

        String permission = args[0];
        String message = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) 
                                       : "&c你没有权限进入这个世界！";

        List<Map<String, Object>> conditions = getConditions(worldName);
        
        Map<String, Object> newCondition = plugin.getConfig().createSection("temp").getValues(false);
        newCondition.put("type", "permission");
        newCondition.put("permission", permission);
        newCondition.put("message", message);

        conditions.add(newCondition);
        plugin.getConfigManager().getWorldRules().set("worlds." + worldName + ".conditions", conditions);
        plugin.getConfigManager().saveWorldRules();

        sender.sendMessage(ChatColor.GREEN + "已添加权限检查条件");
        return true;
    }

    private boolean handleAddVariable(CommandSender sender, String worldName, String[] args) {
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.usage.add_variable"));
            return true;
        }

        String variable = args[0];
        String operator = args[1];
        String value = args[2];
        String message = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) 
                                       : "&c你不满足进入条件！";

        List<Map<String, Object>> conditions = getConditions(worldName);
        
        Map<String, Object> newCondition = plugin.getConfig().createSection("temp").getValues(false);
        newCondition.put("type", "variable");
        newCondition.put("variable", variable);
        newCondition.put("operator", operator);
        
        if (operator.equals("range")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "范围检查需要两个值: 最小值和最大值");
                return true;
            }
            newCondition.put("min", Double.parseDouble(value));
            newCondition.put("max", Double.parseDouble(args[3]));
            message = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) 
                                    : message;
        } else {
            newCondition.put("value", value);
        }
        
        newCondition.put("message", message);

        conditions.add(newCondition);
        plugin.getConfigManager().getWorldRules().set("worlds." + worldName + ".conditions", conditions);
        plugin.getConfigManager().saveWorldRules();

        sender.sendMessage(ChatColor.GREEN + "已添加变量检查条件");
        return true;
    }

    private boolean handleAddCooldown(CommandSender sender, String worldName, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.usage.add_cooldown"));
            return true;
        }

        int time;
        try {
            time = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的时间: " + args[0]);
            return true;
        }

        List<Map<String, Object>> conditions = getConditions(worldName);
        
        Map<String, Object> newCondition = plugin.getConfig().createSection("temp").getValues(false);
        newCondition.put("type", "cooldown");
        newCondition.put("time", time);
        
        if (args.length > 1) {
            newCondition.put("permission_bypass", args[1]);
        }
        
        if (args.length > 2) {
            newCondition.put("message", String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        }

        conditions.add(newCondition);
        plugin.getConfigManager().getWorldRules().set("worlds." + worldName + ".conditions", conditions);
        plugin.getConfigManager().saveWorldRules();

        sender.sendMessage(ChatColor.GREEN + "已添加冷却检查条件");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldlimit.admin")) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.usage.world_remove"));
            return true;
        }

        String worldName = args[0];
        int index;
        try {
            index = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.world.invalid_index")
                    .replace("%value%", args[1]));
            return true;
        }

        List<Map<String, Object>> conditions = getConditions(worldName);
        
        if (index < 0 || index >= conditions.size()) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.world.invalid_index")
                    .replace("%max%", String.valueOf(conditions.size())));
            return true;
        }

        conditions.remove(index);
        plugin.getConfigManager().getWorldRules().set("worlds." + worldName + ".conditions", conditions);
        plugin.getConfigManager().saveWorldRules();

        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.world.condition_removed"));
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldlimit.admin")) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.no_permission"));
            return true;
        }

        String worldName = args.length > 0 ? args[0] : null;
        ConfigurationSection worldsSection = plugin.getConfigManager().getWorldRules()
                .getConfigurationSection("worlds");
        
        if (worldName != null) {
            // 显示特定世界的条件
            ConfigurationSection worldConfig = worldsSection.getConfigurationSection(worldName);
            if (worldConfig == null) {
                sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.world.not_found")
                    .replace("%world%", worldName));
                return true;
            }
            
            sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.world.conditions_header")
                .replace("%world%", worldName));
            List<Map<String, Object>> conditions = getConditions(worldName);
            if (conditions.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.world.no_conditions"));
                return true;
            }
            
            for (int i = 0; i < conditions.size(); i++) {
                sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + formatCondition(conditions.get(i)));
            }
        } else {
            // 显示所有世界
            sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.world.list_header"));
            for (String world : worldsSection.getKeys(false)) {
                boolean enabled = worldsSection.getBoolean(world + ".enabled", true);
                String status = enabled ? 
                    ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.world.status_enabled") :
                    ChatColor.RED + plugin.getLanguageManager().getMessage("command.world.status_disabled");
                sender.sendMessage(ChatColor.YELLOW + world + " " + status);
            }
        }
        
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldlimit.admin")) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.usage.world_info"));
            return true;
        }

        String worldName = args[0];
        ConfigurationSection worldConfig = plugin.getConfigManager().getWorldConfig(worldName);
        if (worldConfig == null) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.world.not_found")
                .replace("%world%", worldName));
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.world.info_header")
            .replace("%world%", worldName));
        
        boolean enabled = worldConfig.getBoolean("enabled", true);
        String status = enabled ? 
            ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.world.status_enabled") :
            ChatColor.RED + plugin.getLanguageManager().getMessage("command.world.status_disabled");
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.world.status") + " " + status);
        
        ConfigurationSection messages = worldConfig.getConfigurationSection("message");
        if (messages != null) {
            sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.world.deny_message") + " " + 
                messages.getString("deny", plugin.getLanguageManager().getMessage("command.world.default_message")));
            sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.world.cooldown_message") + " " + 
                messages.getString("cooldown", plugin.getLanguageManager().getMessage("command.world.default_message")));
        }

        List<Map<String, Object>> conditions = getConditions(worldName);
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.world.conditions_count") + " " + 
            conditions.size());
        for (int i = 0; i < conditions.size(); i++) {
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + formatCondition(conditions.get(i)));
        }

        return true;
    }

    private String formatCondition(Map<String, Object> condition) {
        StringBuilder sb = new StringBuilder();
        String type = (String) condition.get("type");
        sb.append("[").append(type).append("] ");

        switch (type.toLowerCase()) {
            case "item":
                sb.append("物品: ").append(condition.get("item"))
                  .append(" 数量: ").append(condition.get("amount"));
                if (condition.containsKey("name")) {
                    sb.append(" 名称: ").append(condition.get("name"));
                }
                break;
            case "permission":
                sb.append("权限: ").append(condition.get("permission"));
                break;
            case "variable":
                sb.append("变量: ").append(condition.get("variable"))
                  .append(" 操作符: ").append(condition.get("operator"));
                if (condition.get("operator").equals("range")) {
                    sb.append(" 范围: ").append(condition.get("min"))
                      .append("-").append(condition.get("max"));
                } else {
                    sb.append(" 值: ").append(condition.get("value"));
                }
                break;
            case "cooldown":
                sb.append("冷却时间: ").append(condition.get("time"));
                if (condition.containsKey("permission_bypass")) {
                    sb.append(" 绕过权限: ").append(condition.get("permission_bypass"));
                }
                break;
            default:
                sb.append("未知类型");
        }
        return sb.toString();
    }

    private boolean handleLanguage(CommandSender sender, String[] args) {
        plugin.getLogger().info("[DEBUG] 处理语言命令，参数数量: " + args.length);
        
        if (args.length < 1) {
            showLanguageList(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(); // 这是 "default", "set" 或 "list"
        plugin.getLogger().info("[DEBUG] 语言子命令: " + subCommand);

        if (subCommand.equals("default")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.usage.lang_default"));
                return true;
            }
            String langCode = args[1]; // 保持原始大小写
            plugin.getLogger().info("[DEBUG] 执行设置默认语言: " + langCode);
            return setDefaultLanguage(sender, langCode);
        } else if (subCommand.equals("set")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.usage.lang_set"));
                return true;
            }
            return setPlayerLanguage(sender, args[1], args[2]);
        } else if (subCommand.equals("list")) {
            showLanguageList(sender);
            return true;
        } else {
            plugin.getLogger().info("[DEBUG] 未知的语言子命令: " + subCommand);
            sender.sendMessage(ChatColor.RED + "用法: /wl lang <list|set|default>");
            return true;
        }
    }

    private void showLanguageList(CommandSender sender) {
        plugin.getLogger().info("[DEBUG] 显示语言列表");
        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.lang.list_header"));
        String defaultLang = plugin.getLanguageManager().getDefaultLanguage();
        plugin.getLogger().info("[DEBUG] 当前默认语言: " + defaultLang);
        
        for (String lang : plugin.getLanguageManager().getSupportedLanguages()) {
            boolean isDefault = lang.equals(defaultLang);
            plugin.getLogger().info("[DEBUG] 显示语言: " + lang + (isDefault ? " (默认)" : ""));
            sender.sendMessage(ChatColor.YELLOW + lang + 
                (isDefault ? ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.lang.default_mark") : ""));
        }
    }

    private boolean setPlayerLanguage(CommandSender sender, String lang, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.lang.player_not_found")
                .replace("%player%", playerName));
            return true;
        }

        if (!Arrays.asList(plugin.getLanguageManager().getSupportedLanguages()).contains(lang)) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.lang.not_found")
                .replace("%lang%", lang));
            showLanguageList(sender);
            return true;
        }

        plugin.getLanguageManager().setPlayerLanguage(target, lang);
        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.lang.player_set")
            .replace("%player%", target.getName())
            .replace("%lang%", lang));
        target.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.lang.player_set_self")
            .replace("%lang%", lang));
        return true;
    }

    private boolean setDefaultLanguage(CommandSender sender, String lang) {
        plugin.getLogger().info("[DEBUG] 开始设置默认语言: " + lang);
        
        // 检查语言是否存在
        boolean langExists = false;
        for (String supportedLang : plugin.getLanguageManager().getSupportedLanguages()) {
            if (supportedLang.equals(lang)) {
                langExists = true;
                break;
            }
        }

        if (!langExists) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.lang.not_found")
                    .replace("%lang%", lang));
            return true;
        }

        // 设置默认语言
        plugin.getLanguageManager().setDefaultLanguage(lang);
        
        // 发送成功消息
        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.lang.default_set")
                .replace("%lang%", lang));
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "add":
                case "remove":
                case "list":
                case "info":
                    return plugin.getServer().getWorlds().stream()
                            .map(World::getName)
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "debug":
                    return debugCommand.onTabComplete(sender, command, alias, 
                            Arrays.copyOfRange(args, 1, args.length));
                case "lang":
                    return Arrays.asList("list", "set", "default").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            return Arrays.asList("item", "permission", "variable", "cooldown").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("lang")) {
            if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("default")) {
                return Arrays.asList(plugin.getLanguageManager().getSupportedLanguages()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("lang") && args[1].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.help.header"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.help.reload"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.help.add"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.help.remove"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.help.list"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.help.info"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.help.lang"));
        sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.help.debug"));
    }
} 