package cn.i7mc.fateservants.model;

import org.bukkit.configuration.ConfigurationSection;
import cn.i7mc.fateservants.FateServants;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import cn.i7mc.fateservants.utils.DebugUtils;

public class ServantClass {
    private final String id;
    private final String displayName;
    private final String skinName;
    private final String description;
    private final String qualityRangeMin;
    private final String qualityRangeMax;
    private final double baseHealth;
    private final double baseMovementSpeed;
    private final int maxLevel;
    private final double baseMana;
    private final int attackSpeed;  // 攻击速度
    private final Map<String, Double> attributes;
    private final Map<String, String> attributeGrowthRanges;
    private final List<SkillConfig> skills;
    private final Map<String, Double> baseAttributes = new HashMap<>();

    public ServantClass(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.skinName = config.getString("skin_name", "Steve");
        this.description = config.getString("description", "");
        
        // 读取品质范围
        ConfigurationSection qualitySection = config.getConfigurationSection("quality_range");
        DebugUtils.debug("servantclass.quality_range", id, (qualitySection != null));
        if (qualitySection != null) {
            this.qualityRangeMin = qualitySection.getString("min", "C");
            this.qualityRangeMax = qualitySection.getString("max", "SSR");
            DebugUtils.debug("servantclass.quality_range_loaded", id, this.qualityRangeMin, this.qualityRangeMax);
        } else {
            this.qualityRangeMin = "C";
            this.qualityRangeMax = "SSR";
            FateServants.getInstance().getLogger().warning("Using default quality range for " + id + ": min=C, max=SSR");
        }
        
        // 加载基础属性
        ConfigurationSection baseSection = config.getConfigurationSection("base");
        if (baseSection != null) {
            this.baseHealth = baseSection.getDouble("health", 0.0);
            this.baseMovementSpeed = baseSection.getDouble("movement_speed", 0.0);
            this.maxLevel = (int) baseSection.getDouble("max_level", 0.0);
            this.baseMana = baseSection.getDouble("max_mana", 100.0);  // 加载基础魔法值
            this.attackSpeed = baseSection.getInt("attack_speed", 20);  // 加载攻击速度，默认20tick
        } else {
            this.baseHealth = 0.0;
            this.baseMovementSpeed = 0.0;
            this.maxLevel = 0;
            this.baseMana = 100.0;
            this.attackSpeed = 20;
        }
        
        // 加载战斗属性和成长资质范围
        this.attributes = new HashMap<>();
        this.attributeGrowthRanges = new HashMap<>();
        ConfigurationSection attrSection = config.getConfigurationSection("attributes");
        if (attrSection != null) {
            for (String key : attrSection.getKeys(false)) {
                if (!key.endsWith("_growth")) {
                    double value = attrSection.getDouble(key);
                    this.attributes.put(key, value);
                    this.baseAttributes.put(key, value); // 同时填充baseAttributes
                    String growthKey = key + "_growth";
                    if (attrSection.contains(growthKey)) {
                        this.attributeGrowthRanges.put(key, attrSection.getString(growthKey));
                    }
                }
            }
        }
        
        // 添加核心属性到baseAttributes
        this.baseAttributes.put("health", this.baseHealth);
        this.baseAttributes.put("movement_speed", this.baseMovementSpeed);
        this.baseAttributes.put("mana", this.baseMana);
        
        // 加载技能
        this.skills = new ArrayList<>();
        if (config.contains("skills")) {
            List<Map<?, ?>> skillsList = config.getMapList("skills");
            for (Map<?, ?> skillMap : skillsList) {
                String name = String.valueOf(skillMap.get("name"));
                double chance = skillMap.containsKey("chance") ? 
                    Double.parseDouble(String.valueOf(skillMap.get("chance"))) : 100.0;
                String desc = String.valueOf(skillMap.get("description"));
                this.skills.add(new SkillConfig(name, chance, desc));
            }
        }
    }

    // 基本信息获取方法
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getSkinName() { return skinName; }
    public String getDescription() { return description; }
    
    // 基础属性获取方法
    public double getBaseHealth() {
        return baseHealth;
    }

    public double getBaseMovementSpeed() {
        return baseMovementSpeed;
    }

    public double getBaseMana() {
        return baseMana;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getAttackSpeed() {
        return attackSpeed;
    }

    // 品质范围获取方法
    public String getQualityRangeMin() {
        DebugUtils.debug("servantclass.get_min_quality", id, qualityRangeMin);
        return qualityRangeMin;
    }

    public String getQualityRangeMax() {
        DebugUtils.debug("servantclass.get_max_quality", id, qualityRangeMax);
        return qualityRangeMax;
    }
    
    // 战斗属性获取方法
    public Map<String, Double> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    public double getAttribute(String attributeName) {
        return attributes.getOrDefault(attributeName, 0.0);
    }

    // 成长资质相关方法
    public String getAttributeGrowthRange(String attributeName) {
        return attributeGrowthRanges.get(attributeName);
    }
    
    public static double generateRandomGrowth(String range) {
        if (range == null || range.isEmpty()) return 1.0;
        
        String[] parts = range.split("-");
        if (parts.length != 2) return 1.0;
        
        try {
            double min = Double.parseDouble(parts[0]) / 100.0;
            double max = Double.parseDouble(parts[1]) / 100.0;
            return min + (max - min) * Math.random();
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    // 技能相关方法
    public List<SkillConfig> getSkills() {
        return new ArrayList<>(skills);
    }

    public static ServantClass fromConfig(String id, ConfigurationSection config) {
        if (config == null) return null;
        return new ServantClass(id, config);
    }

    // 内部类定义
    public static class SkillConfig {
        private final String name;
        private final double chance;
        private final String description;
        
        public SkillConfig(String name, double chance, String description) {
            this.name = name;
            this.chance = chance;
            this.description = description;
        }
        
        public String getName() { return name; }
        public double getChance() { return chance; }
        public String getDescription() { return description; }
    }

    /**
     * 获取所有基础属性
     * @return 基础属性映射
     */
    public Map<String, Double> getBaseAttributes() {
        return new HashMap<>(baseAttributes);
    }
} 