package cn.i7mc.fateservants.attributes;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.utils.MessageManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 统一属性系统管理器
 * 所有属性均从stats.yml加载，不再有任何硬编码
 */
public class AttributeManager {
    private final FateServants plugin;
    private final Map<String, AttributeInfo> attributeInfoMap = new HashMap<>();
    
    /**
     * 构造方法
     * @param plugin 插件实例
     */
    public AttributeManager(FateServants plugin) {
        this.plugin = plugin;
        loadAttributes();
    }
    
    /**
     * 加载所有属性配置
     * 包括生命值、魔法值等所有属性
     */
    public void loadAttributes() {
        File statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            plugin.saveResource("stats.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection attributesSection = config.getConfigurationSection("attributes");
        
        if (attributesSection == null) {
            plugin.getLogger().warning("stats.yml中缺少attributes配置节点");
            return;
        }
        
        attributeInfoMap.clear();
        
        // 加载所有属性，包括之前分开处理的生命值、魔力等核心属性
        for (String attrKey : attributesSection.getKeys(false)) {
            ConfigurationSection attrSection = attributesSection.getConfigurationSection(attrKey);
            if (attrSection == null) continue;
            
            AttributeInfo info = new AttributeInfo(
                attrKey,
                attrSection.getString("display_name", attrKey),
                attrSection.getString("symbol", ""),
                attrSection.getString("material", "STONE"),
                attrSection.getDouble("min_value", 0.0),
                attrSection.getDouble("max_value", Double.MAX_VALUE),
                attrSection.getString("format", "§7{display_name}: §f{value}{symbol}")
            );
            
            // 加载属性与其他插件的映射
            ConfigurationSection mappingSection = attrSection.getConfigurationSection("mapping");
            if (mappingSection != null) {
                for (String pluginName : mappingSection.getKeys(false)) {
                    info.addMapping(pluginName, mappingSection.getString(pluginName));
                }
            }
            
            attributeInfoMap.put(attrKey, info);
            
            // 调试输出
            plugin.getLogger().info("加载属性: " + attrKey + ", 显示名称: " + info.getDisplayName());
            if (mappingSection != null) {
                for (String pluginName : mappingSection.getKeys(false)) {
                    plugin.getLogger().info("  映射: " + pluginName + " -> " + mappingSection.getString(pluginName));
                }
            }
        }
        
        plugin.getLogger().info(String.format("已加载 %d 个属性定义", attributeInfoMap.size()));
    }
    
    /**
     * 重新加载所有属性
     */
    public void reload() {
        loadAttributes();
    }
    
    /**
     * 获取属性信息
     * @param attributeName 属性名称
     * @return 属性信息
     */
    public AttributeInfo getAttributeInfo(String attributeName) {
        return attributeInfoMap.getOrDefault(attributeName, null);
    }
    
    /**
     * 获取所有属性名称
     * @return 属性名称集合
     */
    public Set<String> getAllAttributeNames() {
        return new HashSet<>(attributeInfoMap.keySet());
    }
    
    /**
     * 获取属性与其他插件的映射名称
     * @param attributeName 属性名称
     * @param pluginName 插件名称
     * @return 映射名称
     */
    public String getMappedName(String attributeName, String pluginName) {
        AttributeInfo info = getAttributeInfo(attributeName);
        if (info == null) return null;
        return info.getMappedName(pluginName);
    }
    
    /**
     * 通过显示名称查找属性
     * @param displayName 显示名称
     * @return 属性名称
     */
    public String getAttributeByDisplayName(String displayName) {
        for (AttributeInfo info : attributeInfoMap.values()) {
            if (info.getDisplayName().equals(displayName)) {
                return info.getName();
            }
        }
        return null;
    }
    
    /**
     * 检查属性是否存在
     * @param attributeName 属性名称
     * @return 是否存在
     */
    public boolean hasAttribute(String attributeName) {
        return attributeInfoMap.containsKey(attributeName);
    }
    
    /**
     * 获取属性总数
     * @return 属性总数
     */
    public int getAttributeCount() {
        return attributeInfoMap.size();
    }
} 