package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.attributes.ServantQuality;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * 英灵生成器 - 提供增强的英灵生成算法
 * 主要功能：
 * 1. 基于权重的职阶和品质随机选择
 * 2. 属性平衡生成，避免出现过于极端的属性分配
 * 3. 成长系数的合理分配
 */
public class ServantGenerator {
    private static final FateServants plugin = FateServants.getInstance();
    
    // 成长分布权重
    private static final double[] GROWTH_DISTRIBUTION = {0.85, 0.9, 0.95, 1.0, 1.05, 1.1, 1.15};
    private static final int[] GROWTH_WEIGHTS = {1, 3, 5, 7, 5, 3, 1};
    
    // 职阶权重和品质范围
    private static final Map<String, Double> classWeights = new HashMap<>();
    private static final Map<String, Map<ServantQuality, double[]>> qualityRanges = new HashMap<>();
    
    static {
        initWeights();
    }
    
    /**
     * 初始化各职阶权重和品质范围
     */
    public static void initWeights() {
        classWeights.clear();
        qualityRanges.clear();
        
        FileConfiguration config = plugin.getClassesConfig();
        if (config == null) {
            DebugUtils.log("generator.no_classes_config");
            return;
        }
        
        ConfigurationSection classesSection = config.getConfigurationSection("classes");
        if (classesSection == null) {
            DebugUtils.log("generator.no_classes_config");
            return;
        }
        
        // 获取默认的品质配置
        Collection<ServantQuality> allQualities = ServantQuality.values();
        
        // 获取默认的高级品质配置，从配置文件中读取，而不是硬编码
        ConfigurationSection qualitiesConfig = plugin.getConfig().getConfigurationSection("qualities");
        List<String> highTierQualities = new ArrayList<>();
        
        if (qualitiesConfig != null) {
            // 通过配置中的multiplier值来判断高级品质
            // 假设multiplier大于4的为高级品质
            for (String qualityId : qualitiesConfig.getKeys(false)) {
                double multiplier = qualitiesConfig.getDouble(qualityId + ".multiplier", 1.0);
                if (multiplier > 4.0) {
                    highTierQualities.add(qualityId);
                    DebugUtils.log("generator.high_tier_quality", qualityId, String.valueOf(multiplier));
                }
            }
        }
        
        // 如果配置中没有找到高级品质，基于现有品质的乘数动态确定高级品质
        if (highTierQualities.isEmpty()) {
            // 获取所有品质并按乘数值排序
            List<ServantQuality> sortedQualities = new ArrayList<>(ServantQuality.values());
            sortedQualities.sort((q1, q2) -> Double.compare(q2.getAttributeMultiplier(), q1.getAttributeMultiplier()));
            
            // 取前30%的品质作为高级品质（或至少1个）
            int highTierCount = Math.max(1, (int)(sortedQualities.size() * 0.3));
            for (int i = 0; i < highTierCount && i < sortedQualities.size(); i++) {
                highTierQualities.add(sortedQualities.get(i).getId());
            }
            
            DebugUtils.log("generator.dynamic_high_tier_qualities", 
                String.join(", ", highTierQualities), 
                String.valueOf(highTierCount), 
                String.valueOf(sortedQualities.size()));
        }
        
        for (String className : classesSection.getKeys(false)) {
            ConfigurationSection classSection = classesSection.getConfigurationSection(className);
            if (classSection == null) continue;
            
            // 读取职阶权重
            double weight = classSection.getDouble("weight", 1.0);
            classWeights.put(className, weight);
            DebugUtils.log("generator.class_weight", className, String.valueOf(weight));
            
            // 读取品质范围
            Map<ServantQuality, double[]> qualityRange = new HashMap<>();
            ConfigurationSection qualitySection = classSection.getConfigurationSection("quality_range");
            if (qualitySection != null) {
                for (String qualityName : qualitySection.getKeys(false)) {
                    try {
                        ServantQuality quality = ServantQuality.fromName(qualityName);
                        double min = qualitySection.getDouble(qualityName + ".min", 0);
                        double max = qualitySection.getDouble(qualityName + ".max", 0);
                        qualityRange.put(quality, new double[]{min, max});
                    } catch (Exception e) {
                        DebugUtils.log("generator.invalid_quality", className, qualityName);
                    }
                }
            }
            
            // 如果没有配置品质范围，使用默认品质范围
            if (qualityRange.isEmpty()) {
                // 使用配置中的所有品质，分配均匀的概率范围
                int qualityCount = allQualities.size();
                if (qualityCount > 0) {
                    double rangePerQuality = 100.0 / qualityCount;
                    double currentRange = 0.0;
                    
                    for (ServantQuality quality : allQualities) {
                        double rangeStart = currentRange;
                        double rangeEnd = currentRange + rangePerQuality;
                        qualityRange.put(quality, new double[]{rangeStart, rangeEnd});
                        currentRange = rangeEnd;
                    }
                    
                    DebugUtils.log("generator.default_quality_range", className, 
                            String.valueOf(qualityCount), String.valueOf(rangePerQuality));
                }
            }
            
            qualityRanges.put(className, qualityRange);
            DebugUtils.log("generator.quality_range", className, qualityRange.toString());
        }
        
        DebugUtils.log("generator.init_complete", String.valueOf(classWeights.size()));
    }
    
    /**
     * 随机选择一个职阶，基于权重
     * @return 选择的职阶名称
     */
    public static String randomClass() {
        if (classWeights.isEmpty()) {
            initWeights();
            if (classWeights.isEmpty()) {
                return null;
            }
        }
        
        double totalWeight = classWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        
        double cumulativeWeight = 0;
        for (Map.Entry<String, Double> entry : classWeights.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (random < cumulativeWeight) {
                String selectedClass = entry.getKey();
                DebugUtils.log("generator.random_class", selectedClass);
                return selectedClass;
            }
        }
        
        // 默认返回第一个职阶
        String defaultClass = classWeights.keySet().iterator().next();
        DebugUtils.log("generator.random_class", defaultClass);
        return defaultClass;
    }
    
    /**
     * 根据职阶随机选择品质
     * @param className 职阶名称
     * @return 选择的品质
     */
    public static ServantQuality randomQuality(String className) {
        if (!qualityRanges.containsKey(className)) {
            DebugUtils.log("generator.class_not_found", className);
            return ServantQuality.fromName("E"); // 默认返回E级品质
        }
        
        Map<ServantQuality, double[]> ranges = qualityRanges.get(className);
        double random = ThreadLocalRandom.current().nextDouble(100);
        
        // 计算高品质的生成概率
        double highTierChance = 0;
        double lowTierChance = 0;
        
        // 通过配置识别高级品质，计算概率分布
        ConfigurationSection qualitiesConfig = plugin.getConfig().getConfigurationSection("qualities");
        List<String> highTierQualities = new ArrayList<>();
        
        if (qualitiesConfig != null) {
            // 通过配置中的multiplier值来判断高级品质
            // 假设multiplier大于4的为高级品质
            for (String qualityId : qualitiesConfig.getKeys(false)) {
                double multiplier = qualitiesConfig.getDouble(qualityId + ".multiplier", 1.0);
                if (multiplier > 4.0) {
                    highTierQualities.add(qualityId);
                    DebugUtils.log("generator.high_tier_quality", qualityId, String.valueOf(multiplier));
                }
            }
        }
        
        // 如果配置中没有找到高级品质，基于现有品质的乘数动态确定高级品质
        if (highTierQualities.isEmpty()) {
            // 获取所有品质并按乘数值排序
            List<ServantQuality> sortedQualities = new ArrayList<>(ServantQuality.values());
            sortedQualities.sort((q1, q2) -> Double.compare(q2.getAttributeMultiplier(), q1.getAttributeMultiplier()));
            
            // 取前30%的品质作为高级品质（或至少1个）
            int highTierCount = Math.max(1, (int)(sortedQualities.size() * 0.3));
            for (int i = 0; i < highTierCount && i < sortedQualities.size(); i++) {
                highTierQualities.add(sortedQualities.get(i).getId());
            }
            
            DebugUtils.log("generator.dynamic_high_tier_qualities", 
                String.join(", ", highTierQualities), 
                String.valueOf(highTierCount), 
                String.valueOf(sortedQualities.size()));
        }
        
        // 将品质按ID分类并计算概率
        for (Map.Entry<ServantQuality, double[]> entry : ranges.entrySet()) {
            ServantQuality quality = entry.getKey();
            String qualityId = quality.getId();
            double[] range = entry.getValue();
            double chance = range[1] - range[0];
            
            // 使用配置确定的高级品质列表进行判断
            if (highTierQualities.contains(qualityId)) {
                highTierChance += chance;
            } else {
                lowTierChance += chance;
            }
        }
        
        DebugUtils.log("generator.quality_distribution", className, 
                String.format("高级品质=%.2f%%, 低级品质=%.2f%%", highTierChance, lowTierChance));
        
        // 尝试按随机值选择品质
        for (Map.Entry<ServantQuality, double[]> entry : ranges.entrySet()) {
            double[] range = entry.getValue();
            if (random >= range[0] && random < range[1]) {
                ServantQuality quality = entry.getKey();
                DebugUtils.log("generator.random_quality", quality.getId(), className);
                return quality;
            }
        }
        
        // 默认返回默认品质（通常是E级）
        ServantQuality defaultQuality = ServantQuality.fromName("E");
        DebugUtils.log("generator.random_quality", defaultQuality.getId(), className);
        return defaultQuality;
    }
    
    /**
     * 随机生成成长系数，基于给定的分布和权重
     * @return 成长系数
     */
    public static double randomGrowthFactor() {
        int totalWeight = Arrays.stream(GROWTH_WEIGHTS).sum();
        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        
        int cumulativeWeight = 0;
        for (int i = 0; i < GROWTH_WEIGHTS.length; i++) {
            cumulativeWeight += GROWTH_WEIGHTS[i];
            if (random < cumulativeWeight) {
                return GROWTH_DISTRIBUTION[i];
            }
        }
        
        return GROWTH_DISTRIBUTION[GROWTH_DISTRIBUTION.length / 2]; // 返回中间值作为默认
    }
    
    /**
     * 生成平衡的属性，确保不会出现极端的属性分配
     * @param baseAttributes 基础属性
     * @param quality 品质
     * @return 生成的属性
     */
    public static Map<String, Double> generateBalancedAttributes(Map<String, Double> baseAttributes, ServantQuality quality) {
        Map<String, Double> attributes = new HashMap<>();
        double qualityMultiplier = quality.getAttributeMultiplier();
        
        // 计算每个属性的权重和波动范围
        double totalVariation = 0;
        Map<String, Double> variations = new HashMap<>();
        
        for (String attr : baseAttributes.keySet()) {
            // 生成-15%到+15%的随机波动
            double variation = ThreadLocalRandom.current().nextDouble(-0.15, 0.15);
            variations.put(attr, variation);
            totalVariation += Math.abs(variation);
        }
        
        // 规范化波动，使总波动平衡为0
        if (totalVariation > 0) {
            double normalizedTotal = 0;
            for (Map.Entry<String, Double> entry : variations.entrySet()) {
                double value = entry.getValue();
                value /= totalVariation;
                entry.setValue(value);
                normalizedTotal += value;
            }
        }
        
        // 应用波动生成最终属性
        double overallChange = 0;
        for (Map.Entry<String, Double> entry : baseAttributes.entrySet()) {
            String attr = entry.getKey();
            double base = entry.getValue();
            double growth = randomGrowthFactor();
            double variation = variations.getOrDefault(attr, 0.0);
            
            // 应用品质乘数、成长系数和波动
            double finalValue = base * qualityMultiplier * growth * (1 + variation);
            attributes.put(attr, finalValue);
            
            DebugUtils.log("generator.attribute_generated", attr, String.format("%.2f", finalValue), String.format("%.2f", growth));
            overallChange += Math.abs(variation);
        }
        
        DebugUtils.log("generator.balanced_attributes", 
            baseAttributes.toString(), 
            attributes.toString(), 
            String.format("%.2f", overallChange * 100));
        
        return attributes;
    }
    
    /**
     * 为玩家生成随机英灵
     * @param player 玩家
     * @param forcedClass 强制职阶，为null时随机选择
     * @return 生成的英灵
     */
    public static Servant generateRandomServant(Player player, String forcedClass) {
        // 选择职阶
        String className = forcedClass;
        if (className == null) {
            className = randomClass();
        } else {
            DebugUtils.log("generator.force_class", className);
        }
        
        if (className == null) {
            return null;
        }
        
        // 获取职阶对象
        ServantClass servantClass = plugin.getServantClassManager().getClass(className);
        if (servantClass == null) {
            DebugUtils.log("generator.class_not_found", className);
            return null;
        }
        
        // 随机选择品质
        ServantQuality quality = randomQuality(className);
        
        // 创建英灵
        Servant servant = new Servant(player, servantClass);
        servant.setQuality(quality);
        
        // 获取职阶基础属性
        Map<String, Double> baseAttributes = servantClass.getAttributes();
        
        // 生成平衡的属性
        Map<String, Double> attributes = generateBalancedAttributes(baseAttributes, quality);
        
        // 设置属性
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            servant.getAttributes().setValue(entry.getKey(), entry.getValue());
        }
        
        DebugUtils.log("generator.servant_created", 
            player.getName(), 
            className, 
            quality.getId(), 
            String.format("%.2f", quality.getAttributeMultiplier()), 
            String.valueOf(attributes.size()));
        
        return servant;
    }
    
    /**
     * 重新加载配置
     */
    public static void reload() {
        initWeights();
    }
} 