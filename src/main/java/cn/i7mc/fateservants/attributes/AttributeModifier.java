package cn.i7mc.fateservants.attributes;

import java.util.Objects;
import java.util.UUID;

/**
 * 属性修饰符类
 * 用于修改属性的基础值
 */
public class AttributeModifier {
    /**
     * 操作类型
     */
    public enum Operation {
        /**
         * 加法操作
         */
        ADD,
        
        /**
         * 乘法操作
         */
        MULTIPLY,
        
        /**
         * 基础值乘法操作
         */
        MULTIPLY_BASE
    }
    
    private final UUID id;
    private final String name;
    private final double amount;
    private final Operation operation;
    
    /**
     * 构造方法
     * @param id 修饰符ID
     * @param name 修饰符名称
     * @param amount 修饰符数值
     * @param operation 操作类型
     */
    public AttributeModifier(UUID id, String name, double amount, Operation operation) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.operation = operation;
    }
    
    /**
     * 构造方法（自动生成UUID）
     * @param name 修饰符名称
     * @param amount 修饰符数值
     * @param operation 操作类型
     */
    public AttributeModifier(String name, double amount, Operation operation) {
        this(UUID.randomUUID(), name, amount, operation);
    }
    
    /**
     * 构造方法（带优先级）
     * @param name 修饰符名称
     * @param attribute 属性名称
     * @param amount 修饰符数值
     * @param operation 操作类型
     * @param priority 优先级
     */
    public AttributeModifier(String name, String attribute, double amount, Operation operation, int priority) {
        this(UUID.randomUUID(), name, amount, operation);
    }
    
    /**
     * 获取修饰符ID
     * @return 修饰符ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * 获取修饰符名称
     * @return 修饰符名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取修饰符数值
     * @return 修饰符数值
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * 获取修饰符数值（等同于getAmount()）
     * @return 修饰符数值
     */
    public double getValue() {
        return amount;
    }
    
    /**
     * 获取操作类型
     * @return 操作类型
     */
    public Operation getOperation() {
        return operation;
    }
    
    /**
     * 应用修饰符
     * @param baseValue 基础值
     * @param currentValue 当前值
     * @return 应用后的值
     */
    public double apply(double baseValue, double currentValue) {
        if (operation == Operation.ADD) {
            return currentValue + amount;
        } else if (operation == Operation.MULTIPLY) {
            return currentValue * (1 + amount);
        } else if (operation == Operation.MULTIPLY_BASE) {
            return currentValue + (baseValue * amount);
        }
        return currentValue;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeModifier that = (AttributeModifier) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("AttributeModifier{id=%s, name='%s', amount=%s, operation=%s}",
                id, name, amount, operation);
    }
} 