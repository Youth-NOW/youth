package com.example.worldlimit.manager;

import com.example.worldlimit.WorldLimitPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {
    private final WorldLimitPlugin plugin;
    private final Map<String, FileConfiguration> configFiles;
    private FileConfiguration worldRules;
    private File worldRulesFile;
    private final Set<String> modifiedPaths;

    public ConfigManager(WorldLimitPlugin plugin) {
        this.plugin = plugin;
        this.configFiles = new HashMap<>();
        this.modifiedPaths = new HashSet<>();
    }

    public void loadConfigs() {
        // 加载主配置文件
        saveDefaultConfig();
        reloadConfig();
        configFiles.put("config.yml", plugin.getConfig());

        // 加载世界规则配置
        loadWorldRules();
    }

    private void saveDefaultConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        // 使用UTF-8重新加载配置
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)
            );
            plugin.getConfig().setDefaults(config);
        } catch (IOException e) {
            plugin.getLogger().warning("无法加载配置文件: " + e.getMessage());
        }
    }

    private void reloadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            // 使用UTF-8编码读取配置文件
            YamlConfiguration config = new YamlConfiguration();
            config.load(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

            // 获取默认配置
            final InputStream defConfigStream = plugin.getResource("config.yml");
            if (defConfigStream != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)
                ));
            }

            // 设置到插件配置中
            plugin.getConfig().setDefaults(config);
            for (String key : config.getKeys(true)) {
                plugin.getConfig().set(key, config.get(key));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("无法重新加载配置文件: " + e.getMessage());
        }
    }

    private void loadWorldRules() {
        if (worldRulesFile == null) {
            worldRulesFile = new File(plugin.getDataFolder(), "world_rules.yml");
        }

        // 如果文件不存在，保存默认配置
        if (!worldRulesFile.exists()) {
            plugin.saveResource("world_rules.yml", false);
            plugin.getLogger().info("Created default world_rules.yml");
        }

        // 加载配置文件
        worldRules = YamlConfiguration.loadConfiguration(worldRulesFile);

        // 获取默认配置用于比对和补全
        InputStream defaultStream = plugin.getResource("world_rules.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            
            // 在设置默认值之前检查差异
            checkConfigurationDifferences(worldRules, defaultConfig);
            
            worldRules.setDefaults(defaultConfig);
            worldRules.options().copyDefaults(true);
            saveWorldRules();
        }

        configFiles.put("world_rules.yml", worldRules);
        
        // 验证配置
        validateConfiguration();
    }

    private void checkConfigurationDifferences(FileConfiguration current, FileConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!current.contains(key)) {
                modifiedPaths.add(key);
                plugin.getLogger().info("配置项 '" + key + "' 不存在，将使用默认值");
            }
        }
    }

    private void validateConfiguration() {
        boolean hasErrors = false;
        ConfigurationSection worldsSection = worldRules.getConfigurationSection("worlds");
        
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection worldConfig = worldsSection.getConfigurationSection(worldName);
                if (worldConfig != null) {
                    // 验证基本结构
                    if (!worldConfig.contains("enabled")) {
                        plugin.getLogger().warning("世界 '" + worldName + "' 缺少 'enabled' 设置");
                        hasErrors = true;
                    }

                    // 验证条件列表
                    if (worldConfig.contains("conditions")) {
                        List<Map<?, ?>> conditions = worldConfig.getMapList("conditions");
                        for (int i = 0; i < conditions.size(); i++) {
                            Map<?, ?> condition = conditions.get(i);
                            // 检查是否包含至少一个有效的条件字段
                            if (!hasValidConditionField(condition)) {
                                plugin.getLogger().warning("世界 '" + worldName + "' 的第 " + (i + 1) + " 个条件缺少有效的条件字段");
                                hasErrors = true;
                            }
                        }
                    }
                }
            }
        }

        if (hasErrors) {
            plugin.getLogger().warning("配置文件验证完成，发现一些问题需要修复");
        } else {
            plugin.getLogger().info("配置文件验证完成，未发现问题");
        }
    }

    private boolean hasValidConditionField(Map<?, ?> condition) {
        // 检查是否包含任意一个有效的条件字段
        return condition.containsKey("permission") ||
               condition.containsKey("item") ||
               condition.containsKey("variable") ||
               condition.containsKey("time");
    }

    public void saveWorldRules() {
        if (worldRules == null || worldRulesFile == null) {
            return;
        }
        try {
            worldRules.save(worldRulesFile);
            if (!modifiedPaths.isEmpty()) {
                plugin.getLogger().info("以下配置项已更新：");
                for (String path : modifiedPaths) {
                    plugin.getLogger().info("- " + path);
                }
                modifiedPaths.clear();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存world_rules.yml", e);
        }
    }

    public void reloadWorldRules() {
        loadWorldRules();
    }

    public FileConfiguration getWorldRules() {
        if (worldRules == null) {
            loadWorldRules();
        }
        return worldRules;
    }

    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    public boolean isWorldEnabled(String worldName) {
        return getWorldRules().getBoolean("worlds." + worldName + ".enabled", true);
    }

    public Set<String> getConfiguredWorlds() {
        ConfigurationSection worldsSection = getWorldRules().getConfigurationSection("worlds");
        return worldsSection != null ? worldsSection.getKeys(false) : new HashSet<>();
    }

    public ConfigurationSection getWorldConfig(String worldName) {
        return getWorldRules().getConfigurationSection("worlds." + worldName);
    }

    public String getDefaultDenyMessage() {
        return getWorldRules().getString("settings.default_messages.deny", "&c你不能进入这个世界！");
    }

    public String getDefaultCooldownMessage() {
        return getWorldRules().getString("settings.default_messages.cooldown", 
                "&e你需要等待 {time} 秒才能再次进入！");
    }

    public boolean isVariableCacheEnabled() {
        return plugin.getConfig().getBoolean("cache.variable_enabled", true);
    }

    public long getVariableCacheDuration() {
        return plugin.getConfig().getLong("cache.variable_duration", 1000);
    }

    public Set<String> getModifiedPaths() {
        return Collections.unmodifiableSet(modifiedPaths);
    }
} 