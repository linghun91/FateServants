package cn.i7mc.fateservants.attributes;

import org.bukkit.configuration.ConfigurationSection;
import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.utils.MessageManager;
import java.util.*;

/**
 * 英灵品质类
 * 代表英灵的稀有度和品质系数
 */
public class ServantQuality {
    private static final Map<String, ServantQuality> qualities = new HashMap<>();
    private static final Map<String, ServantQuality> qualitiesByDisplayName = new HashMap<>();
    private static ServantQuality defaultQuality;
    
    private final String id;
    private final String displayName;
    private final String color;
    private final double attributeMultiplier;
    
    /**
     * 构造函数
     * 
     * @param id 品质ID
     * @param displayName 显示名称
     * @param color 颜色代码
     * @param attributeMultiplier 属性乘数
     */
    private ServantQuality(String id, String displayName, String color, double attributeMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.attributeMultiplier = attributeMultiplier;
    }
    
    /**
     * 获取品质ID
     * 
     * @return 品质ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取显示名称
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取颜色代码
     * 
     * @return 颜色代码
     */
    public String getColor() {
        return color;
    }
    
    /**
     * 获取属性乘数
     * 
     * @return 属性乘数
     */
    public double getAttributeMultiplier() {
        return attributeMultiplier;
    }
    
    /**
     * 获取带颜色的显示名称
     * 
     * @return 带颜色的显示名称
     */
    public String getColoredName() {
        return color + displayName;
    }
    
    /**
     * 从名称获取品质
     * 
     * @param name 品质名称
     * @return 对应的品质，如果不存在则返回默认品质
     */
    public static ServantQuality fromName(String name) {
        if (name == null) {
            return defaultQuality;
        }
        
        // 先尝试通过ID查找
        ServantQuality quality = qualities.get(name.toUpperCase());
        if (quality != null) {
            return quality;
        }
        
        // 再尝试通过显示名称查找
        quality = qualitiesByDisplayName.get(name);
        if (quality != null) {
            return quality;
        }
        
        // 记录未找到品质的调试信息
        if (FateServants.getInstance().getConfig().getBoolean("debug", false)) {
            MessageManager.debug("quality.not_found", name);
        }
        
        return defaultQuality;
    }
    
    /**
     * 兼容旧版API的方法
     */
    public static ServantQuality valueOf(String name) {
        return fromName(name);
    }
    
    /**
     * 获取所有可用的品质
     * 
     * @return 品质列表
     */
    public static Collection<ServantQuality> values() {
        return qualities.values();
    }
    
    /**
     * 从配置文件加载品质设置
     * 
     * @param config 配置文件节
     */
    public static void loadQualities(ConfigurationSection config) {
        qualities.clear();
        qualitiesByDisplayName.clear();
        
        if (config == null) {
            FateServants.getInstance().getLogger().warning("无法加载品质配置：配置节点为空");
            createDefaultQuality();
            return;
        }
        
        FateServants.getInstance().getLogger().info("开始加载英灵品质配置...");
        
        for (String qualityId : config.getKeys(false)) {
            ConfigurationSection qualitySection = config.getConfigurationSection(qualityId);
            if (qualitySection != null) {
                double multiplier = qualitySection.getDouble("multiplier", 1.0);
                String displayName = qualitySection.getString("display_name", qualityId);
                
                // 从display_name提取颜色代码
                String color = "§f"; // 默认白色
                if (displayName.contains("§")) {
                    int colorIndex = displayName.indexOf("§");
                    if (colorIndex + 1 < displayName.length()) {
                        color = displayName.substring(colorIndex, colorIndex + 2);
                        displayName = displayName.substring(colorIndex + 2);
                    }
                }
                
                ServantQuality quality = new ServantQuality(qualityId, displayName, color, multiplier);
                qualities.put(qualityId, quality);
                qualities.put(qualityId.toUpperCase(), quality); // 同时添加大写版本，便于查找
                qualitiesByDisplayName.put(displayName, quality);
                
                // 设置品质乘数最小的为默认品质，或使用第一个加载的品质作为默认
                if (defaultQuality == null || multiplier < defaultQuality.getAttributeMultiplier()) {
                    defaultQuality = quality;
                    if (FateServants.getInstance().getConfig().getBoolean("debug", false)) {
                        MessageManager.debug("quality.default_set", qualityId, String.valueOf(multiplier));
                    }
                }
                
                // 记录调试信息
                if (FateServants.getInstance().getConfig().getBoolean("debug", false)) {
                    MessageManager.debug("quality.display_name", qualityId, displayName, color);
                    MessageManager.debug("quality.multiplier", qualityId, String.valueOf(multiplier));
                }
                
                FateServants.getInstance().getLogger().info("已加载品质: " + qualityId + 
                    " (显示名称: " + displayName + ", 乘数: " + multiplier + ")");
            }
        }
        
        // 确保至少有一个默认品质
        if (qualities.isEmpty()) {
            createDefaultQuality();
        }
        
        // 记录加载的品质数量
        if (FateServants.getInstance().getConfig().getBoolean("debug", false)) {
            MessageManager.debug("quality.loaded", qualities.size());
        }
    }
    
    /**
     * 创建默认品质配置（当配置文件中没有定义时使用）
     */
    private static void createDefaultQuality() {
        FateServants.getInstance().getLogger().warning("使用默认品质配置");
        
        // 默认品质ID和属性
        String defaultId = "BASIC";
        String defaultDisplayName = "基础";
        String defaultColor = "§7";
        double defaultMultiplier = 1.0;
        
        // 创建一个默认品质
        ServantQuality defaultQualityObj = new ServantQuality(defaultId, defaultDisplayName, defaultColor, defaultMultiplier);
        qualities.put(defaultId, defaultQualityObj);
        qualities.put(defaultId.toUpperCase(), defaultQualityObj);
        qualitiesByDisplayName.put(defaultDisplayName, defaultQualityObj);
        
        // 设置默认品质
        ServantQuality.defaultQuality = defaultQualityObj;
        
        // 记录使用默认配置
        if (FateServants.getInstance().getConfig().getBoolean("debug", false)) {
            MessageManager.debug("quality.using_defaults", defaultId, defaultDisplayName, String.valueOf(defaultMultiplier));
        }
    }
} 