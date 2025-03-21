package cn.i7mc.fateservants.attributes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 属性容器类
 * 用于存储和管理属性值及修饰符
 */
public class AttributeContainer {
    private final Map<String, Double> baseValues = new HashMap<>();
    private final Map<String, Map<UUID, AttributeModifier>> modifiers = new HashMap<>();
    private final Map<String, Double> cachedValues;
    private boolean isDirty;
    private final Map<String, String> displayNames = new HashMap<>();
    private final Map<String, String> descriptions = new HashMap<>();
    private final Map<String, String> units = new HashMap<>();

    public AttributeContainer(Map<String, Double> baseValues) {
        this.baseValues.putAll(baseValues);
        this.cachedValues = new ConcurrentHashMap<>();
        this.isDirty = true;

        // 初始化所有属性
        for (String attribute : baseValues.keySet()) {
            modifiers.put(attribute, new ConcurrentHashMap<>());
        }

        recalculateAll();
        initializeBaseAttributes();
    }

    /**
     * 初始化基础属性
     */
    private void initializeBaseAttributes() {
        // 基础属性
        baseValues.put("health", 20.0);
        baseValues.put("mana", 100.0);
        baseValues.put("attack", 5.0);
        baseValues.put("defense", 5.0);
        baseValues.put("speed", 0.2);
        
        // 战斗相关属性
        baseValues.put("crit_rate", 5.0);
        baseValues.put("crit_damage", 150.0);
        baseValues.put("dodge", 5.0);
        baseValues.put("accuracy", 95.0);
        
        // 元素属性
        baseValues.put("fire_resistance", 0.0);
        baseValues.put("ice_resistance", 0.0);
        baseValues.put("lightning_resistance", 0.0);
        
        // 属性显示名称
        displayNames.put("health", "生命值");
        displayNames.put("mana", "魔力值");
        displayNames.put("attack", "攻击力");
        displayNames.put("defense", "防御力");
        displayNames.put("speed", "速度");
        displayNames.put("crit_rate", "暴击率");
        displayNames.put("crit_damage", "暴击伤害");
        displayNames.put("dodge", "闪避率");
        displayNames.put("accuracy", "命中率");
        displayNames.put("fire_resistance", "火焰抗性");
        displayNames.put("ice_resistance", "冰霜抗性");
        displayNames.put("lightning_resistance", "雷电抗性");
        
        // 单位
        units.put("crit_rate", "%");
        units.put("crit_damage", "%");
        units.put("dodge", "%");
        units.put("accuracy", "%");
        units.put("fire_resistance", "%");
        units.put("ice_resistance", "%");
        units.put("lightning_resistance", "%");
    }

    /**
     * 获取属性的基础值
     * @param attributeName 属性名称
     * @return 基础值，如果不存在则返回0
     */
    public double getBaseValue(String attributeName) {
        return baseValues.getOrDefault(attributeName, 0.0);
    }
    
    /**
     * 设置属性的基础值
     * @param attributeName 属性名称
     * @param value 基础值
     */
    public void setBaseValue(String attributeName, double value) {
        baseValues.put(attributeName, value);
    }
    
    /**
     * 获取所有属性值
     */
    public Map<String, Double> getAll() {
        if (isDirty) {
            recalculateAll();
        }
        return new HashMap<>(cachedValues);
    }

    /**
     * 获取所有属性及其值
     * @return 属性名称和值的Map
     */
    public Map<String, Double> getAllAttributes() {
        return new HashMap<>(baseValues);
    }

    /**
     * 获取属性当前值
     */
    public double getValue(String attribute) {
        if (isDirty) {
            recalculateAll();
        }
        return cachedValues.getOrDefault(attribute, 0.0);
    }

    /**
     * 添加属性修饰符
     * @param attributeName 属性名称
     * @param modifier 修饰符
     */
    public void addModifier(String attributeName, AttributeModifier modifier) {
        modifiers.computeIfAbsent(attributeName, k -> new ConcurrentHashMap<>()).put(modifier.getId(), modifier);
    }
    
    /**
     * 移除属性修饰符
     * @param attributeName 属性名称
     * @param modifierId 修饰符ID
     * @return 是否成功移除
     */
    public boolean removeModifier(String attributeName, UUID modifierId) {
        Map<UUID, AttributeModifier> attrModifiers = modifiers.get(attributeName);
        if (attrModifiers != null) {
            return attrModifiers.remove(modifierId) != null;
        }
        return false;
    }
    
    /**
     * 获取属性的最终值（基础值+所有修饰符）
     * @param attributeName 属性名称
     * @return 最终值
     */
    public double getFinalValue(String attributeName) {
        double baseValue = getBaseValue(attributeName);
        
        // 如果没有修饰符，直接返回基础值
        Map<UUID, AttributeModifier> attrModifiers = modifiers.get(attributeName);
        if (attrModifiers == null || attrModifiers.isEmpty()) {
            return baseValue;
        }
        
        // 计算所有修饰符的影响
        double additiveValue = 0.0;
        double multiplicativeValue = 1.0;
        
        for (AttributeModifier modifier : attrModifiers.values()) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADD) {
                additiveValue += modifier.getAmount();
            } else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY) {
                multiplicativeValue *= (1 + modifier.getAmount());
            }
        }
        
        // 计算最终值: (基础值 + 加法修饰符) * 乘法修饰符
        return (baseValue + additiveValue) * multiplicativeValue;
    }
    
    /**
     * 获取属性的所有修饰符
     * @param attributeName 属性名称
     * @return 修饰符集合
     */
    public Set<AttributeModifier> getModifiers(String attributeName) {
        Map<UUID, AttributeModifier> attrModifiers = modifiers.get(attributeName);
        if (attrModifiers != null) {
            return new HashSet<>(attrModifiers.values());
        }
        return new HashSet<>();
    }
    
    /**
     * 清除属性的所有修饰符
     * @param attributeName 属性名称
     */
    public void clearModifiers(String attributeName) {
        modifiers.remove(attributeName);
    }
    
    /**
     * 清除所有属性的所有修饰符
     */
    public void clearAllModifiers() {
        modifiers.clear();
    }
    
    /**
     * 获取所有已设置的属性名称
     * @return 属性名称集合
     */
    public Set<String> getAttributeNames() {
        Set<String> names = new HashSet<>(baseValues.keySet());
        names.addAll(modifiers.keySet());
        return names;
    }
    
    /**
     * 检查属性是否已设置
     * @param attributeName 属性名称
     * @return 是否已设置
     */
    public boolean hasAttribute(String attributeName) {
        return baseValues.containsKey(attributeName) || modifiers.containsKey(attributeName);
    }

    /**
     * 标记需要重新计算
     */
    private void markDirty() {
        isDirty = true;
    }

    /**
     * 重新计算所有属性值
     */
    private void recalculateAll() {
        cachedValues.clear();
        for (String attribute : baseValues.keySet()) {
            recalculate(attribute);
        }
        isDirty = false;
    }

    private void recalculate(String attribute) {
        double base = getBaseValue(attribute);
        double finalValue = base;

        Map<UUID, AttributeModifier> attrModifiers = modifiers.get(attribute);
        if (attrModifiers != null) {
            for (AttributeModifier modifier : attrModifiers.values()) {
                finalValue = modifier.apply(base, finalValue);
            }
        }

        cachedValues.put(attribute, finalValue);
    }

    /**
     * 获取属性描述
     */
    public List<String> getAttributeDescription(String attribute) {
        List<String> description = new ArrayList<>();
        double value = getValue(attribute);
        double baseValue = getBaseValue(attribute);
        
        String displayName = attribute.substring(0, 1).toUpperCase() + attribute.substring(1);
        String format = attribute.equals("health") ? 
            String.format("%s: %.1f/%.1f (%.1f%%)", displayName, value, baseValue, (value / baseValue * 100)) :
            String.format("%s: %.1f", displayName, value);
        
        description.add(format);

        Set<AttributeModifier> mods = getModifiers(attribute);
        if (!mods.isEmpty()) {
            description.add("§7修饰符:");
            for (AttributeModifier modifier : mods) {
                String prefix = modifier.getValue() > 0 ? "§a+" : "§c";
                String valueStr;
                switch (modifier.getOperation()) {
                    case ADD:
                        valueStr = String.format("%s%.1f", prefix, modifier.getValue());
                        break;
                    case MULTIPLY:
                        valueStr = String.format("%s%.1f%%", prefix, modifier.getValue() * 100);
                        break;
                    case MULTIPLY_BASE:
                        valueStr = String.format("%s%.1f%% §7(基础值)", prefix, modifier.getValue() * 100);
                        break;
                    default:
                        valueStr = String.format("%s%.1f", prefix, modifier.getValue());
                }
                description.add(String.format("§8- §f%s: %s", modifier.getName(), valueStr));
            }
        }

        return description;
    }

    public void addTemporaryModifier(String attribute, double value) {
        AttributeModifier modifier = new AttributeModifier(
            "临时修饰符",
            attribute,
            value,
            AttributeModifier.Operation.ADD,
            100
        );
        addModifier(attribute, modifier);
    }

    public void clearTemporaryModifiers() {
        modifiers.clear();
        recalculateAll();
    }

    /**
     * 获取属性显示名称
     * @param attribute 属性名
     * @return 显示名称，如果没有则返回属性名本身
     */
    public String getDisplayName(String attribute) {
        return displayNames.getOrDefault(attribute, attribute);
    }

    /**
     * 设置属性显示名称
     * @param attribute 属性名
     * @param displayName 显示名称
     */
    public void setDisplayName(String attribute, String displayName) {
        displayNames.put(attribute, displayName);
    }

    /**
     * 获取属性单位
     * @param attribute 属性名
     * @return 单位，如果没有则返回空字符串
     */
    public String getUnit(String attribute) {
        return units.getOrDefault(attribute, "");
    }

    /**
     * 设置属性单位
     * @param attribute 属性名
     * @param unit 单位
     */
    public void setUnit(String attribute, String unit) {
        units.put(attribute, unit);
    }

    /**
     * 获取属性描述
     * @param attribute 属性名
     * @return 描述，如果没有则返回空字符串
     */
    public String getDescription(String attribute) {
        return descriptions.getOrDefault(attribute, "");
    }

    /**
     * 设置属性描述
     * @param attribute 属性名
     * @param description 描述
     */
    public void setDescription(String attribute, String description) {
        descriptions.put(attribute, description);
    }

    /**
     * 设置属性值（同时更新基础值）
     * @param attribute 属性名
     * @param value 属性值
     */
    public void setValue(String attribute, double value) {
        baseValues.put(attribute, value);
        markDirty();
    }

    /**
     * 从另一个AttributeContainer复制属性值
     * @param source 源属性容器
     */
    public void copyFrom(AttributeContainer source) {
        if (source == null) return;
        
        // 复制基础属性值
        for (Map.Entry<String, Double> entry : source.baseValues.entrySet()) {
            this.baseValues.put(entry.getKey(), entry.getValue());
        }
        
        // 复制修饰符
        for (Map.Entry<String, Map<UUID, AttributeModifier>> entry : source.modifiers.entrySet()) {
            String attributeName = entry.getKey();
            Map<UUID, AttributeModifier> attributeModifiers = entry.getValue();
            
            Map<UUID, AttributeModifier> targetModifiers = this.modifiers.computeIfAbsent(
                attributeName, k -> new ConcurrentHashMap<>()
            );
            
            for (AttributeModifier modifier : attributeModifiers.values()) {
                targetModifiers.put(modifier.getId(), modifier);
            }
        }
        
        // 复制显示名称、描述和单位
        this.displayNames.putAll(source.displayNames);
        this.descriptions.putAll(source.descriptions);
        this.units.putAll(source.units);
        
        // 标记为脏，以便下次获取属性值时重新计算
        markDirty();
    }
} 