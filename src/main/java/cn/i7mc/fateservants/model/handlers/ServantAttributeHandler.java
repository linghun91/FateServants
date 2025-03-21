package cn.i7mc.fateservants.model.handlers;

import cn.i7mc.fateservants.attributes.AttributeContainer;
import cn.i7mc.fateservants.attributes.ServantQuality;
import cn.i7mc.fateservants.attributes.provider.AttributePlusProvider;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantClass;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.data.AttributeData;
import org.serverct.ersha.attribute.data.AttributeSource;

import java.util.HashMap;
import java.util.Map;

/**
 * 英灵属性处理器
 * 负责处理英灵的属性计算、修改和应用
 */
public class ServantAttributeHandler {
    private final Servant servant;
    private final AttributeContainer attributes;
    private final Map<String, Double> attributeGrowths;
    private AttributePlusProvider provider;

    /**
     * 构造函数
     * @param servant 所属的英灵
     * @param attributes 属性容器
     * @param attributeGrowths 属性成长资质
     */
    public ServantAttributeHandler(Servant servant, AttributeContainer attributes, Map<String, Double> attributeGrowths) {
        this.servant = servant;
        this.attributes = attributes;
        this.attributeGrowths = new HashMap<>(attributeGrowths);
        
        // 初始化AttributePlus提供者
        this.provider = new AttributePlusProvider(cn.i7mc.fateservants.FateServants.getInstance());
    }

    /**
     * 获取属性容器
     * @return 属性容器
     */
    public AttributeContainer getAttributes() {
        return attributes;
    }

    /**
     * 获取指定属性的成长资质
     * @param attributeName 属性名
     * @return 成长资质值
     */
    public double getAttributeGrowth(String attributeName) {
        return attributeGrowths.getOrDefault(attributeName, 0.0);
    }

    /**
     * 获取所有属性的成长资质
     * @return 属性成长资质的Map
     */
    public Map<String, Double> getAttributeGrowths() {
        return new HashMap<>(attributeGrowths);
    }

    /**
     * 设置属性成长资质
     * @param attributeName 属性名
     * @param growth 成长资质值
     */
    public void setAttributeGrowth(String attributeName, double growth) {
        attributeGrowths.put(attributeName, growth);
    }

    /**
     * 计算最大生命值
     * @return 计算后的最大生命值
     */
    public double calculateMaxHealth() {
        double baseHealth = servant.getServantClass().getBaseHealth();
        double qualityMultiplier = servant.getQuality().getAttributeMultiplier();
        double levelMultiplier = 1.0 + ((servant.getLevel() - 1) * 0.1);
        
        // 考虑成长资质
        double growthRate = getAttributeGrowth("health");
        if (growthRate <= 0) growthRate = 1.0;
        
        DebugUtils.log("attribute.calculation", "MaxHealth", 
            String.format("基础值=%.2f, 品质系数=%.2f, 等级系数=%.2f, 成长资质=%.2f", 
            baseHealth, qualityMultiplier, levelMultiplier, growthRate));
            
        return baseHealth * qualityMultiplier * levelMultiplier * growthRate;
    }

    /**
     * 计算最大魔法值
     * @return 计算后的最大魔法值
     */
    public double calculateMaxMana() {
        double baseMana = servant.getServantClass().getBaseMana();
        double qualityMultiplier = servant.getQuality().getAttributeMultiplier();
        double levelMultiplier = 1.0 + ((servant.getLevel() - 1) * 0.05);
        
        // 考虑成长资质
        double growthRate = getAttributeGrowth("mana");
        if (growthRate <= 0) growthRate = 1.0;
        
        DebugUtils.log("attribute.calculation", "MaxMana", 
            String.format("基础值=%.2f, 品质系数=%.2f, 等级系数=%.2f, 成长资质=%.2f", 
            baseMana, qualityMultiplier, levelMultiplier, growthRate));
            
        return baseMana * qualityMultiplier * levelMultiplier * growthRate;
    }

    /**
     * 计算普通攻击伤害
     * @return 计算后的攻击伤害
     */
    public double calculateAttackDamage() {
        double baseAttack = attributes.getValue("attack");
        DebugUtils.log("attribute.calculation", "AttackDamage", baseAttack);
        return baseAttack;
    }

    /**
     * 使用AttributePlus处理攻击
     * @param targetEntity 目标实体
     * @param damage 基础伤害
     * @return 是否成功处理
     */
    public boolean applyAttributePlusDamage(LivingEntity targetEntity, double damage) {
        Player owner = servant.getOwner();
        if (owner == null || !owner.isOnline() || targetEntity == null) return false;
        
        try {
            DebugUtils.log("attribute.ap_attack_start", servant.getServantClass().getDisplayName());
            
            // 获取主人的AttributePlus数据
            AttributeData ownerData = AttributeAPI.getAttrData(owner);
            if (ownerData == null) {
                DebugUtils.log("attribute.ap_data_null", owner.getName());
                return false;
            }
            
            // 保存主人原有的属性源
            Map<String, AttributeSource> originalSources = new HashMap<>(ownerData.getApiSourceAttribute());
            
            // 清除主人当前所有属性
            ownerData.clearApiAttribute();
            
            // 创建英灵属性源的映射
            Map<String, Number[]> servantAttributes = new HashMap<>();
            // 映射英灵属性到AttributePlus属性
            attributes.getAll().forEach((key, value) -> {
                String mappedAttr = provider.getMappedAttributeName(key);
                if (mappedAttr != null) {
                    double attrValue = value;
                    DebugUtils.log("attribute.provider_mapping", key, mappedAttr);
                    servantAttributes.put(mappedAttr, new Number[]{attrValue, attrValue});
                }
            });
            
            // 创建英灵属性源并添加到主人身上
            AttributeSource servantSource = AttributeAPI.createStaticAttributeSource(
                new HashMap<>(servantAttributes), 
                new HashMap<>()
            );
            
            ownerData.operationApiAttribute("servant_attributes", servantSource, 
                AttributeSource.OperationType.ADD, true);
            
            // 让主人攻击目标，使AttributePlus的属性生效
            targetEntity.damage(damage, owner);
            
            // 恢复主人原有属性
            ownerData.clearApiAttribute();
            originalSources.forEach((key, source) -> {
                ownerData.operationApiAttribute(key, source, AttributeSource.OperationType.ADD, true);
            });
            
            DebugUtils.log("attribute.ap_attack_complete", 
                servant.getServantClass().getDisplayName(), targetEntity.getName(), damage);
            
            return true;
        } catch (Exception e) {
            DebugUtils.logObject(e, "AttributePlus处理攻击异常");
            return false;
        }
    }

    /**
     * 根据等级更新属性
     * @param newLevel 新等级
     */
    public void updateAttributesForLevel(int newLevel) {
        ServantClass servantClass = servant.getServantClass();
        ServantQuality quality = servant.getQuality();
        
        // 获取品质乘数
        double qualityMultiplier = quality.getAttributeMultiplier();
        
        // 计算等级系数（每级增加5%）
        double levelMultiplier = 1.0 + ((newLevel - 1) * 0.05);
        
        DebugUtils.log("attribute.level_update", 
            servant.getServantClass().getDisplayName(), newLevel, 
            String.format("品质系数=%.2f, 等级系数=%.2f", qualityMultiplier, levelMultiplier));
        
        // 更新所有属性
        for (String attributeName : servantClass.getAttributes().keySet()) {
            double baseValue = servantClass.getAttribute(attributeName);
            double growthRate = getAttributeGrowth(attributeName);
            if (growthRate <= 0) growthRate = 1.0;
            
            double newValue = baseValue * qualityMultiplier * levelMultiplier * growthRate;
            attributes.setValue(attributeName, newValue);
            
            DebugUtils.log("attribute.update", attributeName, 
                String.format("基础值=%.2f -> 新值=%.2f", baseValue, newValue));
        }
    }
} 