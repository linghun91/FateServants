package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 材质适配器，用于处理不同Minecraft版本的材质兼容性
 */
public class MaterialAdapter {
    private static final Map<String, Material> materialMap = new HashMap<>();
    private static final Map<String, String> fallbackMap = new HashMap<>();
    private static FateServants plugin;
    private static boolean initialized = false;
    
    /**
     * 初始化材质适配器
     * @param pluginInstance 插件实例
     */
    public static void init(FateServants pluginInstance) {
        plugin = pluginInstance;
        loadMaterialMappings();
        initialized = true;
    }
    
    /**
     * 从配置文件加载材质映射
     */
    private static void loadMaterialMappings() {
        try {
            FileConfiguration config = plugin.getConfig();
            ConfigurationSection materialSection = config.getConfigurationSection("materials.compatibility");
            
            if (materialSection == null) {
                // 创建默认映射
                createDefaultMappings();
                return;
            }
            
            // 加载自定义映射
            for (String key : materialSection.getKeys(false)) {
                String fallback = materialSection.getString(key);
                if (fallback != null) {
                    fallbackMap.put(key.toUpperCase(), fallback.toUpperCase());
                }
            }
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("material.loaded_mappings", fallbackMap.size());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "加载材质兼容映射时出错", e);
            createDefaultMappings();
        }
    }
    
    /**
     * 创建默认的材质映射
     */
    private static void createDefaultMappings() {
        // 添加1.12及更早版本的材质映射
        fallbackMap.put("NETHERITE_SWORD", "DIAMOND_SWORD");
        fallbackMap.put("NETHERITE_AXE", "DIAMOND_AXE");
        fallbackMap.put("NETHERITE_HELMET", "DIAMOND_HELMET");
        fallbackMap.put("NETHERITE_CHESTPLATE", "DIAMOND_CHESTPLATE");
        fallbackMap.put("NETHERITE_LEGGINGS", "DIAMOND_LEGGINGS");
        fallbackMap.put("NETHERITE_BOOTS", "DIAMOND_BOOTS");
        fallbackMap.put("TRIDENT", "DIAMOND_SWORD");
        fallbackMap.put("CROSSBOW", "BOW");
        fallbackMap.put("SHIELD", "IRON_CHESTPLATE");
        fallbackMap.put("ENCHANTED_GOLDEN_APPLE", "GOLDEN_APPLE");
        fallbackMap.put("KNOWLEDGE_BOOK", "BOOK");
        
        // 添加1.8及更早版本的材质映射
        fallbackMap.put("GOLDEN_SWORD", "GOLD_SWORD");
        fallbackMap.put("GOLDEN_AXE", "GOLD_AXE");
        fallbackMap.put("GOLDEN_HELMET", "GOLD_HELMET");
        fallbackMap.put("GOLDEN_CHESTPLATE", "GOLD_CHESTPLATE");
        fallbackMap.put("GOLDEN_LEGGINGS", "GOLD_LEGGINGS");
        fallbackMap.put("GOLDEN_BOOTS", "GOLD_BOOTS");
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            MessageManager.debug("material.created_default_mappings", fallbackMap.size());
        }
    }
    
    /**
     * 获取兼容的材质
     * @param materialName 材质名称
     * @return 兼容的材质
     */
    public static Material getMaterial(String materialName) {
        if (!initialized) {
            plugin.getLogger().warning("MaterialAdapter未初始化就被调用");
            init(FateServants.getInstance());
        }
        
        if (materialName == null) {
            return Material.STONE; // 默认材质
        }
        
        String matName = materialName.toUpperCase();
        
        // 检查缓存
        if (materialMap.containsKey(matName)) {
            return materialMap.get(matName);
        }
        
        // 尝试直接获取材质
        Material material = null;
        try {
            material = Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            // 材质不存在，使用备用材质
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("material.not_found", matName);
            }
        }
        
        // 如果材质不存在，尝试使用备用材质
        if (material == null && fallbackMap.containsKey(matName)) {
            String fallback = fallbackMap.get(matName);
            try {
                material = Material.valueOf(fallback);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    MessageManager.debug("material.using_fallback", matName, fallback);
                }
            } catch (IllegalArgumentException e) {
                // 备用材质也不存在，使用默认材质
                material = Material.STONE;
                if (plugin.getConfig().getBoolean("debug", false)) {
                    MessageManager.debug("material.fallback_not_found", fallback);
                }
            }
        }
        
        // 如果仍然为null，使用默认材质
        if (material == null) {
            material = Material.STONE;
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("material.using_default", matName);
            }
        }
        
        // 缓存并返回
        materialMap.put(matName, material);
        return material;
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        materialMap.clear();
        if (plugin.getConfig().getBoolean("debug", false)) {
            MessageManager.debug("material.cache_cleared");
        }
    }
} 