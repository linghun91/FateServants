package cn.i7mc.fateservants.attributes.provider;

import org.bukkit.entity.LivingEntity;

/**
 * 属性提供者接口
 * 定义与各种属性插件交互的方法
 */
public interface AttributeProvider {
    /**
     * 获取提供者名称
     * @return 提供者名称
     */
    String getName();
    
    /**
     * 获取提供者优先级
     * 值越高，优先级越高
     * @return 优先级
     */
    int getPriority();
    
    /**
     * 检查提供者是否可用
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取实体的属性值
     * @param entity 实体
     * @param attribute 属性名称
     * @return 属性值
     */
    double getValue(LivingEntity entity, String attribute);
    
    /**
     * 设置实体的属性值
     * @param entity 实体
     * @param attribute 属性名称
     * @param value 属性值
     */
    void setValue(LivingEntity entity, String attribute, double value);
    
    /**
     * 为实体添加临时属性修饰符
     * @param entity 实体
     * @param attribute 属性名称
     * @param value 属性值
     * @param ticks 持续时间（游戏刻）
     */
    void addTemporaryModifier(LivingEntity entity, String attribute, double value, int ticks);
} 