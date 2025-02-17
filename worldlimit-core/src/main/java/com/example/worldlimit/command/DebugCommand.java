package com.example.worldlimit.command;

import com.example.worldlimit.WorldLimitPlugin;
import com.example.worldlimit.api.variable.VariableProvider;
import com.example.worldlimit.api.version.VersionAdapter;
import com.example.worldlimit.api.version.VersionDetector;
import com.example.worldlimit.async.WorldCheckTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Map;

public class DebugCommand implements CommandExecutor, TabCompleter {
    private final WorldLimitPlugin plugin;
    private final List<String> subCommands = Arrays.asList("version", "player", "var", "perf", "stress", "monitor");

    public DebugCommand(WorldLimitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("worldlimit.debug")) {
            sender.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("command.debug.no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.debug.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        if (!subCommands.contains(subCommand)) {
            sender.sendMessage(ChatColor.YELLOW + plugin.getLanguageManager().getMessage("command.debug.usage"));
            return true;
        }

        switch (subCommand) {
            case "version":
                showVersionInfo(sender);
                return true;
            case "player":
                return handlePlayerDebug(sender, args);
            case "var":
                return handleVariableDebug(sender, args);
            case "perf":
                return handlePerformanceDebug(sender);
            case "stress":
                return handleStressTest(sender, args);
            case "monitor":
                return handleMonitorTest(sender, args);
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("worldlimit.debug")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("player") || args[0].equalsIgnoreCase("var"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void showVersionInfo(CommandSender sender) {
        VersionDetector detector = plugin.getVersionManager().getDetector();
        VersionAdapter adapter = plugin.getVersionManager().getAdapter();

        String version = detector.getVersion(plugin.getServer());
        String nms = detector.getNMSVersion(plugin.getServer());
        String adapterName = adapter.getAdapterName();
        boolean isModern = detector.isModernVersion(plugin.getServer());

        sender.sendMessage(ChatColor.GREEN + plugin.getLanguageManager().getMessage("command.debug.version_info"));
        
        sender.sendMessage(ChatColor.WHITE + plugin.getLanguageManager().getMessage("command.debug.server_version")
                .replace("%version%", version));
        sender.sendMessage(ChatColor.WHITE + plugin.getLanguageManager().getMessage("command.debug.nms_version")
                .replace("%nms%", nms));
        sender.sendMessage(ChatColor.WHITE + plugin.getLanguageManager().getMessage("command.debug.adapter_name")
                .replace("%adapter%", adapterName));
        sender.sendMessage(ChatColor.WHITE + plugin.getLanguageManager().getMessage("command.debug.modern_support")
                .replace("%modern%", String.valueOf(isModern)));
    }

    private boolean handlePlayerDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "/wl debug player <玩家名>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[1]);
            return true;
        }

        // 显示玩家基本信息
        sender.sendMessage(ChatColor.GREEN + "=== 玩家调试信息 ===");
        sender.sendMessage(ChatColor.WHITE + "名称: " + target.getName());
        sender.sendMessage(ChatColor.WHITE + "世界: " + target.getWorld().getName());
        sender.sendMessage(ChatColor.WHITE + "生命值: " + target.getHealth() + "/" + target.getMaxHealth());
        sender.sendMessage(ChatColor.WHITE + "等级: " + target.getLevel());
        sender.sendMessage(ChatColor.WHITE + "游戏模式: " + target.getGameMode());
        sender.sendMessage(ChatColor.WHITE + "权限: worldlimit.bypass = " + target.hasPermission("worldlimit.bypass"));

        return true;
    }

    private boolean handleVariableDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "/wl debug var <玩家名> [变量名]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[1]);
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "=== 变量调试信息 ===");
        
        if (args.length >= 3) {
            // 检查特定变量
            String varName = args[2];
            if (!varName.startsWith("%")) {
                varName = "%" + varName;
            }
            if (!varName.endsWith("%")) {
                varName = varName + "%";
            }

            String value = plugin.getVariableEngine().parseVariable(varName, target);
            Double numValue = plugin.getVariableEngine().parseNumericVariable(varName, target);
            
            sender.sendMessage(ChatColor.WHITE + "变量: " + varName);
            sender.sendMessage(ChatColor.WHITE + "文本值: " + value);
            sender.sendMessage(ChatColor.WHITE + "数值: " + (numValue != null ? numValue : "非数值"));
            sender.sendMessage(ChatColor.WHITE + "可用: " + plugin.getVariableEngine().isVariableAvailable(varName));
        } else {
            // 显示所有内置变量
            sender.sendMessage(ChatColor.YELLOW + "内置变量:");
            for (String varName : Arrays.asList(
                    "%player_health%", "%player_max_health%", "%player_food%",
                    "%player_level%", "%player_exp%", "%player_world%",
                    "%player_gamemode%", "%player_flying%"
            )) {
                String value = plugin.getVariableEngine().parseVariable(varName, target);
                sender.sendMessage(ChatColor.WHITE + varName + " = " + value);
            }
        }

        return true;
    }

    private boolean handlePerformanceDebug(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行");
            return true;
        }

        Player player = (Player) sender;
        plugin.getAsyncManager().getPerformanceStats().sendTo(player);
        return true;
    }

    private boolean handleStressTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "用法: /wl debug stress <次数>");
            sender.sendMessage(ChatColor.YELLOW + "例如: /wl debug stress 1000");
            return true;
        }

        int count;
        try {
            count = Integer.parseInt(args[1]);
            if (count <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "请输入有效的测试次数");
            return true;
        }

        Player player = (Player) sender;
        String worldName = player.getWorld().getName();
        
        // 显示初始状态
        plugin.getAsyncManager().getPerformanceStats().sendTo(player);
        
        // 开始测试
        sender.sendMessage(ChatColor.GREEN + "开始执行 " + count + " 次异步检查测试...");
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger successful = new AtomicInteger(0);

        // 批量提交测试任务
        for (int i = 0; i < count; i++) {
            plugin.getAsyncManager().submitTask(new WorldCheckTask(
                plugin,
                player.getName(),
                worldName,
                result -> {
                    if (result) successful.incrementAndGet();
                    if (completed.incrementAndGet() == count) {
                        // 所有任务完成后显示结果
                        long duration = System.currentTimeMillis() - startTime;
                        showTestResults(player, count, successful.get(), duration);
                    }
                },
                error -> completed.incrementAndGet()
            ));
        }

        return true;
    }

    private void showTestResults(Player player, int total, int successful, long duration) {
        player.sendMessage(ChatColor.GREEN + "=== 压力测试结果 ===");
        player.sendMessage(ChatColor.YELLOW + "总请求数: " + total);
        player.sendMessage(ChatColor.YELLOW + "成功请求: " + successful);
        player.sendMessage(ChatColor.YELLOW + "失败请求: " + (total - successful));
        player.sendMessage(ChatColor.YELLOW + "总耗时: " + duration + "ms");
        player.sendMessage(ChatColor.YELLOW + "平均耗时: " + String.format("%.2f", (double)duration / total) + "ms");
        player.sendMessage(ChatColor.YELLOW + "每秒处理: " + String.format("%.2f", (double)total / (duration / 1000.0)));
        
        // 显示线程池状态
        plugin.getAsyncManager().getPerformanceStats().sendTo(player);
    }

    private boolean handleMonitorTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "用法: /wl debug monitor <version|variable|thread>");
            return true;
        }

        String type = args[1].toUpperCase();
        switch (type) {
            case "VERSION":
                // 模拟版本适配异常
                for (int i = 0; i < 11; i++) {
                    plugin.getExceptionMonitor().handleException("VERSION", 
                        new RuntimeException("测试版本适配异常 #" + i));
                }
                sender.sendMessage(ChatColor.GREEN + "已触发版本适配异常测试");
                break;

            case "VARIABLE":
                // 模拟变量解析异常
                for (int i = 0; i < 4; i++) {
                    plugin.getExceptionMonitor().handleException("VARIABLE", 
                        new RuntimeException("变量不可用: %test_var%"));
                }
                sender.sendMessage(ChatColor.GREEN + "已触发变量解析异常测试");
                break;

            case "THREAD":
                // 模拟线程阻塞异常
                for (int i = 0; i < 11; i++) {
                    plugin.getExceptionMonitor().handleException("THREAD", 
                        new RuntimeException("线程池队列已满"));
                }
                sender.sendMessage(ChatColor.GREEN + "已触发线程阻塞异常测试");
                break;

            default:
                sender.sendMessage(ChatColor.RED + "未知的异常类型: " + type);
                return true;
        }

        // 显示异常统计
        Map<String, Integer> stats = plugin.getExceptionMonitor().getExceptionStats();
        sender.sendMessage(ChatColor.GREEN + "=== 异常监控统计 ===");
        stats.forEach((exType, count) -> 
            sender.sendMessage(ChatColor.YELLOW + exType + ": " + count + " 次"));

        return true;
    }
} 