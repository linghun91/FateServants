package cn.i7mc.fateservants.config;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.utils.MessageManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 配置管理器，统一管理所有配置文件
 */
public class ConfigManager {
    private final FateServants plugin;
    private final Map<String, FileConfiguration> configMap = new HashMap<>();
    private final Map<String, File> configFileMap = new HashMap<>();
    
    // 配置文件列表
    private static final String[] CONFIG_FILES = {
        "config.yml", "classes.yml", "gui.yml", "stats.yml", "message.yml", "debugmessage.yml"
    };
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ConfigManager(FateServants plugin) {
        this.plugin = plugin;
        loadAllConfigs();
    }
    
    /**
     * 加载所有配置文件
     */
    public void loadAllConfigs() {
        for (String fileName : CONFIG_FILES) {
            loadConfig(fileName);
        }
    }
    
    /**
     * 加载指定配置文件
     * @param fileName 配置文件名
     */
    public void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configMap.put(fileName, config);
        configFileMap.put(fileName, file);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("已加载配置文件: " + fileName);
        }
    }
    
    /**
     * 重新加载所有配置文件
     */
    public void reloadAllConfigs() {
        for (String fileName : CONFIG_FILES) {
            reloadConfig(fileName);
        }
    }
    
    /**
     * 重新加载指定配置文件
     * @param fileName 配置文件名
     */
    public void reloadConfig(String fileName) {
        File file = configFileMap.get(fileName);
        
        if (file == null) {
            loadConfig(fileName);
            return;
        }
        
        configMap.put(fileName, YamlConfiguration.loadConfiguration(file));
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("已重新加载配置文件: " + fileName);
        }
    }
    
    /**
     * 保存指定配置文件
     * @param fileName 配置文件名
     */
    public void saveConfig(String fileName) {
        File file = configFileMap.get(fileName);
        FileConfiguration config = configMap.get(fileName);
        
        if (file == null || config == null) {
            plugin.getLogger().warning("无法保存配置文件，文件不存在: " + fileName);
            return;
        }
        
        try {
            config.save(file);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("已保存配置文件: " + fileName);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存配置文件: " + fileName, e);
        }
    }
    
    /**
     * 获取配置文件
     * @param fileName 配置文件名
     * @return 配置文件对象
     */
    public FileConfiguration getConfig(String fileName) {
        FileConfiguration config = configMap.get(fileName);
        
        if (config == null) {
            loadConfig(fileName);
            config = configMap.get(fileName);
        }
        
        return config;
    }
    
    /**
     * 获取配置节
     * @param fileName 配置文件名
     * @param path 路径
     * @return 配置节对象
     */
    public ConfigurationSection getConfigSection(String fileName, String path) {
        return getConfig(fileName).getConfigurationSection(path);
    }
    
    /**
     * 获取字符串，如果不存在则返回默认值
     * @param fileName 配置文件名
     * @param path 路径
     * @param defaultValue 默认值
     * @return 字符串值
     */
    public String getString(String fileName, String path, String defaultValue) {
        FileConfiguration config = getConfig(fileName);
        
        if (config.contains(path)) {
            return config.getString(path, defaultValue);
        } else {
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("config.missing_path", fileName, path, defaultValue);
            }
            return defaultValue;
        }
    }
    
    /**
     * 获取整数，如果不存在则返回默认值
     * @param fileName 配置文件名
     * @param path 路径
     * @param defaultValue 默认值
     * @return 整数值
     */
    public int getInt(String fileName, String path, int defaultValue) {
        FileConfiguration config = getConfig(fileName);
        
        if (config.contains(path)) {
            return config.getInt(path, defaultValue);
        } else {
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("config.missing_path", fileName, path, defaultValue);
            }
            return defaultValue;
        }
    }
    
    /**
     * 获取双精度浮点数，如果不存在则返回默认值
     * @param fileName 配置文件名
     * @param path 路径
     * @param defaultValue 默认值
     * @return 双精度浮点数值
     */
    public double getDouble(String fileName, String path, double defaultValue) {
        FileConfiguration config = getConfig(fileName);
        
        if (config.contains(path)) {
            return config.getDouble(path, defaultValue);
        } else {
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("config.missing_path", fileName, path, defaultValue);
            }
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔值，如果不存在则返回默认值
     * @param fileName 配置文件名
     * @param path 路径
     * @param defaultValue 默认值
     * @return 布尔值
     */
    public boolean getBoolean(String fileName, String path, boolean defaultValue) {
        FileConfiguration config = getConfig(fileName);
        
        if (config.contains(path)) {
            return config.getBoolean(path, defaultValue);
        } else {
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("config.missing_path", fileName, path, defaultValue);
            }
            return defaultValue;
        }
    }
} 