package cn.i7mc.fateservants.attributes.provider;

import cn.i7mc.fateservants.FateServants;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.data.AttributeData;
import org.serverct.ersha.attribute.data.AttributeSource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * AttributePlus属性提供者
 * 用于处理FateServants属性与AttributePlus属性之间的映射和转换
 */
public class AttributePlusProvider implements AttributeProvider {
    private final FateServants plugin;
    private final Map<String, String> attributeMapping;

    public AttributePlusProvider(FateServants plugin) {
        this.plugin = plugin;
        this.attributeMapping = new HashMap<>();
        loadMappings();
    }

    private void loadMappings() {
        File configFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!configFile.exists()) {
            plugin.saveResource("stats.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection attributesSection = config.getConfigurationSection("attributes");
        
        if (attributesSection != null) {
            for (String key : attributesSection.getKeys(false)) {
                ConfigurationSection attrSection = attributesSection.getConfigurationSection(key);
                if (attrSection == null) continue;
                
                ConfigurationSection mappingSection = attrSection.getConfigurationSection("mapping");
                if (mappingSection != null && mappingSection.contains("attributeplus")) {
                    String mappedName = mappingSection.getString("attributeplus");
                    attributeMapping.put(key, mappedName);
                    plugin.getLogger().info("Loaded AttributePlus mapping: " + key + " -> " + mappedName);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "AttributePlus";
    }

    @Override
    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("AttributePlus") != null;
    }

    @Override
    public double getValue(LivingEntity entity, String attribute) {
        String mappedAttribute = attributeMapping.get(attribute);
        if (mappedAttribute == null) return 0.0;

        AttributeData data = AttributeAPI.getAttrData(entity);
        if (data == null) return 0.0;

        try {
            Number[] values = data.getCentral().getAttributeValue(mappedAttribute, null);
            if (values != null && values.length > 0) {
                return values[0].doubleValue();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "获取属性值时出错: " + attribute, e);
        }
        return 0.0;
    }

    @Override
    public void setValue(LivingEntity entity, String attribute, double value) {
        String mappedAttribute = attributeMapping.get(attribute);
        if (mappedAttribute == null) return;

        AttributeData data = AttributeAPI.getAttrData(entity);
        if (data == null) return;

        try {
            HashMap<String, Number[]> attrMap = new HashMap<>();
            attrMap.put(mappedAttribute, new Number[]{value});
            AttributeSource source = AttributeAPI.createStaticAttributeSource(attrMap, new HashMap<>());
            data.operationApiAttribute("servant_" + attribute, source, AttributeSource.OperationType.ADD, true);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "设置属性值时出错: " + attribute, e);
        }
    }

    @Override
    public void addTemporaryModifier(LivingEntity entity, String attribute, double value, int ticks) {
        String mappedAttribute = attributeMapping.get(attribute);
        if (mappedAttribute == null) return;

        AttributeData data = AttributeAPI.getAttrData(entity);
        if (data == null) return;

        try {
            HashMap<String, Number[]> attrMap = new HashMap<>();
            attrMap.put(mappedAttribute, new Number[]{value});
            AttributeSource source = AttributeAPI.createStaticAttributeSource(attrMap, new HashMap<>());
            String sourceId = "servant_temp_" + attribute;
            data.operationApiAttribute(sourceId, source, AttributeSource.OperationType.ADD, true);
            
            // 设置定时器移除临时属性
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (data.getApiSourceAttribute().containsKey(sourceId)) {
                    data.clearApiAttribute(); // 清除所有临时属性
                }
            }, ticks);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "添加临时属性时出错: " + attribute, e);
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }

    /**
     * 获取映射的 AttributePlus 属性名
     * @param attribute 原始属性名
     * @return 映射的属性名，如果没有映射则返回 null
     */
    public String getMappedAttributeName(String attribute) {
        return attributeMapping.get(attribute);
    }

    /**
     * 将FateServants属性映射到AttributePlus属性
     * @param attributes FateServants属性集合
     * @return 映射后的AttributePlus属性集合
     */
    public Map<String, Number[]> mapAttributes(Map<String, Double> attributes) {
        Map<String, Number[]> result = new HashMap<>();
        
        attributes.forEach((key, value) -> {
            String mappedName = getMappedAttributeName(key);
            if (mappedName != null) {
                // AttributePlus使用 Number[] 格式，第一个元素是最小值，第二个是最大值
                result.put(mappedName, new Number[]{value, value});
            }
        });
        
        return result;
    }
    
    /**
     * 从AttributePlus属性映射回FateServants属性
     * @param attributes AttributePlus属性集合
     * @return 映射后的FateServants属性集合
     */
    public Map<String, Double> mapAttributesBack(Map<String, Number[]> attributes) {
        Map<String, Double> result = new HashMap<>();
        
        // 创建反向映射
        Map<String, String> reverseMapping = new HashMap<>();
        attributeMapping.forEach((key, value) -> reverseMapping.put(value, key));
        
        attributes.forEach((key, value) -> {
            String originalName = reverseMapping.get(key);
            if (originalName != null && value.length > 0) {
                // 使用最大值（通常是第二个元素）
                double attributeValue = value.length > 1 ? value[1].doubleValue() : value[0].doubleValue();
                result.put(originalName, attributeValue);
            }
        });
        
        return result;
    }
} 