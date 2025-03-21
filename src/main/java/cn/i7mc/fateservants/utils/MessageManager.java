package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * 消息管理器，统一管理插件中的消息
 */
public class MessageManager {
    private static FateServants plugin;
    private static FileConfiguration messageConfig;
    private static FileConfiguration debugMessageConfig;
    
    /**
     * 初始化消息管理器
     * @param pluginInstance 插件实例
     */
    public static void init(FateServants pluginInstance) {
        plugin = pluginInstance;
        reload();
    }
    
    /**
     * 重新加载消息配置
     */
    public static void reload() {
        try {
            if (plugin == null) {
                return;
            }
            
            messageConfig = plugin.getMessageConfig();
            
            // 加载debugmessage.yml
            File debugFile = new File(plugin.getDataFolder(), "debugmessage.yml");
            if (!debugFile.exists()) {
                plugin.saveResource("debugmessage.yml", false);
            }
            debugMessageConfig = YamlConfiguration.loadConfiguration(debugFile);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().severe("重新加载消息配置时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取消息
     * @param key 消息键
     * @return 格式化后的消息
     */
    public static String get(String key) {
        if (messageConfig == null) {
            return ChatColor.RED + "消息配置未加载: " + key;
        }
        
        String message = messageConfig.getString(key);
        if (message == null) {
            return ChatColor.RED + "消息未配置：" + key;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 获取带参数的消息
     * @param key 消息键
     * @param args 参数
     * @return 格式化后的消息
     */
    public static String get(String key, Object... args) {
        String message = get(key);
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return message;
    }
    
    /**
     * 向玩家发送消息
     * @param player 玩家
     * @param key 消息键
     * @param args 参数
     */
    public static void send(Player player, String key, Object... args) {
        player.sendMessage(get(key, args));
    }
    
    /**
     * 记录调试消息
     * @param key 调试消息键
     * @param args 参数
     */
    public static void debug(String key, Object... args) {
        if (!plugin.getConfig().getBoolean("debug", false)) {
            return;
        }
        
        String message = debugMessageConfig.getString(key);
        if (message == null) {
            message = "调试消息未配置：" + key;
        } else {
            message = ChatColor.translateAlternateColorCodes('&', message);
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        
        plugin.getLogger().info("[DEBUG] " + message);
    }
} 