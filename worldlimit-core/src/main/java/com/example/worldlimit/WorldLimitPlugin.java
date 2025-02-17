package com.example.worldlimit;

import com.example.worldlimit.api.variable.VariableProvider;
import com.example.worldlimit.checker.CooldownChecker;
import com.example.worldlimit.checker.ItemChecker;
import com.example.worldlimit.command.MainCommand;
import com.example.worldlimit.listener.WorldChangeListener;
import com.example.worldlimit.manager.ConfigManager;
import com.example.worldlimit.manager.LanguageManager;
import com.example.worldlimit.manager.VersionManager;
import com.example.worldlimit.variable.VariableEngine;
import com.example.worldlimit.variable.VariableChecker;
import com.example.worldlimit.variable.providers.PlayerVariableProvider;
import com.example.worldlimit.async.AsyncManager;
import com.example.worldlimit.monitor.ExceptionMonitor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class WorldLimitPlugin extends JavaPlugin {
    private VersionManager versionManager;
    private LanguageManager languageManager;
    private ConfigManager configManager;
    private VariableEngine variableEngine;
    private VariableChecker variableChecker;
    private ItemChecker itemChecker;
    private CooldownChecker cooldownChecker;
    private AsyncManager asyncManager;
    private ExceptionMonitor exceptionMonitor;

    @Override
    public void onEnable() {
        // Initialize bStats
        int pluginId = 24811;
        Metrics metrics = new Metrics(this, pluginId);

        // 确保配置目录存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 保存默认配置
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResource("config.yml");
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                
                char[] buffer = new char[1024];
                int length;
                while ((length = reader.read(buffer)) > 0) {
                    writer.write(buffer, 0, length);
                }
            } catch (IOException e) {
                getLogger().warning("无法保存默认配置文件: " + e.getMessage());
            }
        }

        // 初始化配置管理器
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();

        // 初始化语言管理器
        this.languageManager = new LanguageManager(this);
        this.languageManager.loadLanguages();
        this.languageManager.loadPlayerLanguages();

        // 初始化版本管理器
        this.versionManager = new VersionManager(this);
        if (!this.versionManager.initialize()) {
            getLogger().severe(languageManager.getMessage("error.version_not_supported")
                    .replace("%version%", getServer().getBukkitVersion()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 初始化变量引擎
        this.variableEngine = new VariableEngine(this);
        this.variableChecker = new VariableChecker(this, variableEngine);

        // 初始化其他检查器
        this.itemChecker = new ItemChecker(this);
        this.cooldownChecker = new CooldownChecker(this);

        // 初始化异步管理器
        this.asyncManager = new AsyncManager(this);

        // 初始化异常监控器
        this.exceptionMonitor = new ExceptionMonitor(this);

        // 注册内置变量提供者
        for (VariableProvider provider : PlayerVariableProvider.createDefaultProviders()) {
            variableEngine.registerProvider(provider);
        }

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);

        // 注册命令
        PluginCommand command = getCommand("wl");
        if (command != null) {
            MainCommand mainCommand = new MainCommand(this);
            command.setExecutor(mainCommand);
            command.setTabCompleter(mainCommand);
        } else {
            getLogger().severe("Failed to register command 'wl'!");
        }

        // 输出版本信息
        String version = versionManager.getDetector().getVersion(getServer());
        String adapterName = versionManager.getAdapter().getAdapterName();
        getLogger().info(languageManager.getMessage("version.detected")
                .replace("%version%", version));
        getLogger().info(languageManager.getMessage("version.adapter_loaded")
                .replace("%adapter%", adapterName));
    }

    @Override
    public void onDisable() {
        // 保存配置
        if (configManager != null) {
            configManager.saveWorldRules();
        }

        // 清理冷却数据
        if (cooldownChecker != null) {
            cooldownChecker.clearAllCooldowns();
        }

        // 关闭异步管理器
        if (asyncManager != null) {
            asyncManager.shutdown();
        }
    }

    @Override
    public void reloadConfig() {
        getLogger().info("[DEBUG] 开始重载配置");
        super.reloadConfig();
        
        if (configManager != null) {
            getLogger().info("[DEBUG] 重载配置管理器");
            configManager.loadConfigs();
        }
        
        if (languageManager != null) {
            getLogger().info("[DEBUG] 重载语言管理器");
            String oldLang = languageManager.getDefaultLanguage();
            languageManager.loadLanguages();
            languageManager.loadPlayerLanguages();
            String newLang = languageManager.getDefaultLanguage();
            getLogger().info("[DEBUG] 语言设置: " + oldLang + " -> " + newLang);
        }
        
        if (variableEngine != null) {
            getLogger().info("[DEBUG] 重载变量引擎");
            variableEngine.reloadSettings();
        }
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public VariableEngine getVariableEngine() {
        return variableEngine;
    }

    public VariableChecker getVariableChecker() {
        return variableChecker;
    }

    public ItemChecker getItemChecker() {
        return itemChecker;
    }

    public CooldownChecker getCooldownChecker() {
        return cooldownChecker;
    }

    public AsyncManager getAsyncManager() {
        return asyncManager;
    }

    public ExceptionMonitor getExceptionMonitor() {
        return exceptionMonitor;
    }
} 