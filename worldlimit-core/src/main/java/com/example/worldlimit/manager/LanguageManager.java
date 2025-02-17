package com.example.worldlimit.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final Map<String, YamlConfiguration> languages;
    private final Map<UUID, String> playerLanguages;
    private String defaultLanguage;
    private static final String[] SUPPORTED_LANGUAGES = {"en_US", "zh_CN", "ja_JP"};

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        this.playerLanguages = new HashMap<>();
        this.defaultLanguage = "en_US";
    }

    public void loadLanguages() {
        plugin.getLogger().info("[DEBUG] 开始加载语言文件");
        plugin.getLogger().info("[DEBUG] 清空现有语言缓存");
        languages.clear();
        
        // 加载所有支持的语言文件
        for (String lang : SUPPORTED_LANGUAGES) {
            plugin.getLogger().info("[DEBUG] 加载语言文件: " + lang);
            loadLanguage(lang);
        }

        // 从配置文件获取默认语言设置
        String configLang = plugin.getConfig().getString("language", "en_US");
        plugin.getLogger().info("[DEBUG] 从配置文件读取默认语言: " + configLang);
        
        if (languages.containsKey(configLang)) {
            defaultLanguage = configLang;
            plugin.getLogger().info("[DEBUG] 设置默认语言为: " + defaultLanguage);
        } else {
            plugin.getLogger().warning("[DEBUG] 配置的语言不存在，使用默认语言: " + defaultLanguage);
        }
    }

    private void loadLanguage(String lang) {
        try {
            String resourcePath = "lang/" + lang + ".yml";
            File langFile = new File(plugin.getDataFolder(), resourcePath);
            plugin.getLogger().info("[DEBUG] 尝试加载语言文件: " + resourcePath);

            YamlConfiguration langConfig;
            if (!langFile.exists()) {
                plugin.getLogger().info("[DEBUG] 文件不存在，从jar中加载: " + resourcePath);
                InputStream defaultLangStream = plugin.getResource(resourcePath);
                if (defaultLangStream != null) {
                    langConfig = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(defaultLangStream, StandardCharsets.UTF_8));
                    languages.put(lang, langConfig);
                    plugin.getLogger().info("[DEBUG] 成功从jar加载语言: " + lang);
                } else {
                    plugin.getLogger().warning("[DEBUG] 在jar中找不到语言文件: " + resourcePath);
                }
            } else {
                plugin.getLogger().info("[DEBUG] 从文件系统加载语言文件: " + langFile.getAbsolutePath());
                langConfig = YamlConfiguration.loadConfiguration(langFile);
                languages.put(lang, langConfig);
                plugin.getLogger().info("[DEBUG] 成功加载语言文件: " + lang);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[DEBUG] 加载语言文件失败: " + lang + ", 错误: " + e.getMessage());
        }
    }

    public String getMessage(String path) {
        return getMessage(path, null);
    }

    public String getMessage(String path, Player player) {
        String lang = player != null ? getPlayerLanguage(player) : defaultLanguage;
        YamlConfiguration langConfig = languages.get(lang);

        if (langConfig != null && langConfig.contains(path)) {
            return langConfig.getString(path);
        }

        // 如果当前语言没有该消息，尝试从默认语言获取
        YamlConfiguration defaultLang = languages.get("en_US");
        if (defaultLang != null && defaultLang.contains(path)) {
            return defaultLang.getString(path);
        }

        return path; // 如果找不到消息，返回路径
    }

    public void setPlayerLanguage(Player player, String lang) {
        if (languages.containsKey(lang)) {
            playerLanguages.put(player.getUniqueId(), lang);
            // 保存玩家语言偏好
            File playerDataFile = new File(plugin.getDataFolder(), "player_languages.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
            config.set(player.getUniqueId().toString(), lang);
            try {
                config.save(playerDataFile);
            } catch (Exception e) {
                plugin.getLogger().warning("无法保存玩家语言偏好: " + e.getMessage());
            }
        }
    }

    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
    }

    public void loadPlayerLanguages() {
        File playerDataFile = new File(plugin.getDataFolder(), "player_languages.yml");
        if (playerDataFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
            for (String uuid : config.getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuid);
                    String lang = config.getString(uuid);
                    if (languages.containsKey(lang)) {
                        playerLanguages.put(playerUUID, lang);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的玩家UUID: " + uuid);
                }
            }
        }
    }

    public String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES.clone();
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String lang) {
        plugin.getLogger().info("[DEBUG] 开始设置默认语言: " + lang);
        plugin.getLogger().info("[DEBUG] 当前默认语言: " + defaultLanguage);
        
        // 检查语言是否存在（不转换大小写）
        boolean langExists = false;
        for (String supportedLang : SUPPORTED_LANGUAGES) {
            if (supportedLang.equals(lang)) {
                langExists = true;
                break;
            }
        }
        
        if (langExists) {
            plugin.getLogger().info("[DEBUG] 语言文件存在，准备更新配置");
            defaultLanguage = lang;
            
            plugin.getLogger().info("[DEBUG] 更新配置文件中的语言设置");
            plugin.getConfig().set("language", lang);
            plugin.saveConfig();
            
            plugin.getLogger().info("[DEBUG] 重新加载语言文件");
            loadLanguages();
            
            plugin.getLogger().info("[DEBUG] 清除玩家语言缓存");
            playerLanguages.clear();
            
            plugin.getLogger().info("[DEBUG] 重新加载玩家语言设置");
            loadPlayerLanguages();
            
            plugin.getLogger().info("[DEBUG] 语言切换完成，当前默认语言: " + defaultLanguage);
        } else {
            plugin.getLogger().warning("[DEBUG] 语言文件不存在: " + lang);
        }
    }

    public void clearPlayerLanguage(Player player) {
        playerLanguages.remove(player.getUniqueId());
        File playerDataFile = new File(plugin.getDataFolder(), "player_languages.yml");
        if (playerDataFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
            config.set(player.getUniqueId().toString(), null);
            try {
                config.save(playerDataFile);
            } catch (Exception e) {
                plugin.getLogger().warning("无法删除玩家语言偏好: " + e.getMessage());
            }
        }
    }
} 