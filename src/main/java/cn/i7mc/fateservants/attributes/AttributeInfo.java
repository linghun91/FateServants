package cn.i7mc.fateservants.attributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 属性信息类
 * 存储单个属性的所有配置信息
 */
public class AttributeInfo {
    private final String name;           // 属性内部名称
    private final String displayName;    // 属性显示名称
    private final String symbol;         // 属性符号
    private final String material;       // 物品材质
    private final double minValue;       // 最小值
    private final double maxValue;       // 最大值
    private final String format;         // 显示格式
    private final Map<String, String> mappings = new HashMap<>(); // 与其他插件的映射
    
    /**
     * 构造方法
     * @param name 属性名称
     * @param displayName 显示名称
     * @param symbol 属性符号
     * @param material 物品材质
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param format 显示格式
     */
    public AttributeInfo(String name, String displayName, String symbol, String material, 
                        double minValue, double maxValue, String format) {
        this.name = name;
        this.displayName = displayName;
        this.symbol = symbol;
        this.material = material;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.format = format;
    }
    
    /**
     * 获取属性名称
     * @return 属性名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取属性符号
     * @return 属性符号
     */
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * 获取物品材质
     * @return 物品材质
     */
    public String getMaterial() {
        return material;
    }
    
    /**
     * 获取最小值
     * @return 最小值
     */
    public double getMinValue() {
        return minValue;
    }
    
    /**
     * 获取最大值
     * @return 最大值
     */
    public double getMaxValue() {
        return maxValue;
    }
    
    /**
     * 获取显示格式
     * @return 显示格式
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * 添加映射
     * @param pluginName 插件名称
     * @param mappedName 映射名称
     */
    public void addMapping(String pluginName, String mappedName) {
        mappings.put(pluginName.toLowerCase(), mappedName);
    }
    
    /**
     * 获取映射名称
     * @param pluginName 插件名称
     * @return 映射名称
     */
    public String getMappedName(String pluginName) {
        return mappings.getOrDefault(pluginName.toLowerCase(), null);
    }
    
    /**
     * 检查是否有指定插件的映射
     * @param pluginName 插件名称
     * @return 是否有映射
     */
    public boolean hasMapping(String pluginName) {
        return mappings.containsKey(pluginName.toLowerCase());
    }
} 