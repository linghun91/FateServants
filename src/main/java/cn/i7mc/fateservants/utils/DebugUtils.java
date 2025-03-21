package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Logger;

/**
 * 调试工具类，用于统一处理插件中的调试信息
 */
public class DebugUtils {
    private static FateServants plugin;
    private static FileConfiguration debugMessageConfig;
    private static boolean debugEnabled = false;
    private static Logger logger;
    
    /**
     * 初始化调试工具类
     * @param pluginInstance 插件实例
     */
    public static void init(FateServants pluginInstance) {
        plugin = pluginInstance;
        logger = plugin.getLogger();
        reload();
    }
    
    /**
     * 重新加载调试配置
     */
    public static void reload() {
        // 读取config.yml中的debug设置
        debugEnabled = plugin.getConfig().getBoolean("debug", false);
        
        // 加载debugmessage.yml
        File debugFile = new File(plugin.getDataFolder(), "debugmessage.yml");
        if (!debugFile.exists()) {
            plugin.saveResource("debugmessage.yml", false);
        }
        debugMessageConfig = YamlConfiguration.loadConfiguration(debugFile);
    }
    
    /**
     * 输出调试信息，仅在debug=true时生效
     * @param key 调试消息键
     * @param args 参数
     */
    public static void debug(String key, Object... args) {
        if (!debugEnabled) {
            return;
        }
        
        String message = debugMessageConfig.getString(key);
        if (message == null) {
            message = "调试消息未配置：" + key;
        } else {
            // 替换参数
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        
        logger.info("[DEBUG] " + message);
    }
    
    /**
     * 输出严重错误信息，不受debug开关控制
     * @param message 错误消息
     * @param throwable 异常对象
     */
    public static void severe(String message, Throwable throwable) {
        logger.severe(message);
        if (throwable != null && debugEnabled) {
            throwable.printStackTrace();
        }
    }
    
    /**
     * 输出警告信息，不受debug开关控制
     * @param message 警告消息
     */
    public static void warning(String message) {
        logger.warning(message);
    }
    
    /**
     * 记录性能调试信息
     * @param operation 操作名称
     * @param startTime 开始时间
     */
    public static void logPerformance(String operation, long startTime) {
        if (!debugEnabled) {
            return;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("[PERF] " + operation + " - 耗时: " + duration + "ms");
    }
    
    /**
     * 记录异常调试信息
     * @param key 调试消息键
     * @param e 异常
     * @param args 参数
     */
    public static void logException(String key, Exception e, Object... args) {
        if (!debugEnabled) {
            return;
        }
        
        try {
            debug(key, args);
            logger.warning("[DEBUG-ERROR] " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug_stacktrace", false)) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            logger.severe("记录异常时出错: " + ex.getMessage());
        }
    }
    
    /**
     * 记录异步操作调试信息
     * @param operation 操作名称
     * @param args 参数
     */
    public static void logAsync(String operation, Object... args) {
        if (!debugEnabled) {
            return;
        }
        
        StringBuilder paramsStr = new StringBuilder();
        for (Object arg : args) {
            paramsStr.append(arg).append(", ");
        }
        
        String params = paramsStr.length() > 0 ? paramsStr.substring(0, paramsStr.length() - 2) : "";
        logger.info("[DEBUG-ASYNC] " + operation + " - 参数: [" + params + "]");
    }
    
    /**
     * 记录缓存操作调试信息
     * @param cacheName 缓存名称
     * @param operation 操作类型
     * @param key 键
     */
    public static void logCache(String cacheName, String operation, Object key) {
        if (!debugEnabled) {
            return;
        }
        
        logger.info("[DEBUG-CACHE] " + cacheName + " - " + operation + ": " + key);
    }
    
    /**
     * 判断是否启用调试模式
     * @return 是否启用
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * 输出对象信息（用于调试）
     * 向后兼容原有代码
     * @param obj 要输出的对象
     * @param label 标签
     */
    public static void logObject(Object obj, String label) {
        if (!debugEnabled) {
            return;
        }
        
        logger.info("[DEBUG] " + label + ": " + (obj == null ? "null" : obj.toString()));
    }
    
    /**
     * 输出异常信息（用于调试）
     * 向后兼容原有代码
     * @param e 异常
     * @param label 标签
     */
    public static void logObject(Exception e, String label) {
        if (!debugEnabled) {
            return;
        }
        
        logger.warning("[DEBUG-ERROR] " + label + ": " + e.getMessage());
        if (plugin.getConfig().getBoolean("debug_stacktrace", false)) {
            e.printStackTrace();
        }
    }
    
    /**
     * 向后兼容方法，调用debug方法
     * @param key 调试消息键
     * @param args 参数
     */
    public static void log(String key, Object... args) {
        debug(key, args);
    }
} 