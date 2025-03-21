package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.config.StatsConfig;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 优化工具类，提供各种计算优化功能
 */
public class OptimizationUtils {
    private static final Map<Integer, Integer> expCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private static final int UPDATE_INTERVAL_MS = 500; // 更新间隔毫秒
    
    /**
     * 获取指定等级所需的经验值（使用缓存）
     * @param level 等级
     * @return 所需经验值
     */
    public static int getRequiredExperience(int level) {
        // 从缓存获取
        if (expCache.containsKey(level)) {
            DebugUtils.log("optimization.exp_cache_hit", level, expCache.get(level));
            return expCache.get(level);
        }
        
        // 计算经验值
        int exp = calculateRequiredExperience(level);
        
        // 缓存结果
        expCache.put(level, exp);
        DebugUtils.log("optimization.exp_precalc", level, exp);
        
        return exp;
    }
    
    /**
     * 计算指定等级所需的经验值
     * @param level 等级
     * @return 所需经验值
     */
    private static int calculateRequiredExperience(int level) {
        // 默认基础经验值和成长因子
        double baseExp = 100.0;
        double expGrowth = 0.1;
        
        // 从配置文件中读取
        try {
            baseExp = FateServants.getInstance().getConfig().getDouble("stats.base_exp_requirement", baseExp);
            expGrowth = FateServants.getInstance().getConfig().getDouble("stats.exp_growth_factor", expGrowth);
        } catch (Exception e) {
            // 使用默认值
        }
        
        // 计算公式: baseExp * (1 + (level - 1) * expGrowth)
        int requiredExp = (int) Math.round(baseExp * (1 + (level - 1) * expGrowth));
        return Math.max(1, requiredExp);
    }
    
    /**
     * 清除经验缓存
     */
    public static void clearExpCache() {
        expCache.clear();
        DebugUtils.log("cache.clear", "经验值缓存");
    }
    
    /**
     * 预热经验缓存（预先计算指定范围的等级所需经验值）
     * @param maxLevel 最大等级
     */
    public static void warmupExpCache(int maxLevel) {
        for (int i = 1; i <= maxLevel; i++) {
            getRequiredExperience(i);
        }
    }
    
    /**
     * 检查是否应该更新（根据时间间隔）
     * @param key 更新键
     * @return 是否应该更新
     */
    public static boolean shouldUpdate(String key) {
        long now = System.currentTimeMillis();
        long lastUpdate = lastUpdateTime.getOrDefault(key, 0L);
        
        if (now - lastUpdate >= UPDATE_INTERVAL_MS) {
            lastUpdateTime.put(key, now);
            return true;
        }
        
        DebugUtils.log("optimization.calculation_skip", key, "更新间隔小于" + UPDATE_INTERVAL_MS + "ms");
        return false;
    }
    
    /**
     * 重置更新时间
     * @param key 更新键
     */
    public static void resetUpdateTime(String key) {
        lastUpdateTime.remove(key);
    }
    
    /**
     * 获取视距范围内的玩家
     * @param location 中心位置
     * @param maxDistance 最大距离
     * @return 范围内的玩家
     */
    public static Collection<Player> getNearbyPlayers(Location location, double maxDistance) {
        return location.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(location) <= maxDistance)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查实体是否在玩家视距范围内
     * @param entity 实体
     * @param player 玩家
     * @return 是否在视距内
     */
    public static boolean isEntityInPlayerViewDistance(Entity entity, Player player) {
        Location entityLoc = entity.getLocation();
        Location playerLoc = player.getLocation();
        
        // 不同世界直接返回false
        if (!entityLoc.getWorld().equals(playerLoc.getWorld())) {
            return false;
        }
        
        // 根据服务器配置的视距设置计算最大距离
        // 默认视距8区块
        int viewDistance = 8;
        try {
            // 尝试从服务器配置获取视距设置
            viewDistance = FateServants.getInstance().getServer().getViewDistance();
        } catch (Exception e) {
            // 使用默认值
        }
        
        double maxViewDistance = viewDistance * 16.0; // 区块转方块距离
        double distance = entityLoc.distance(playerLoc);
        
        boolean inRange = distance <= maxViewDistance;
        DebugUtils.log("optimization.range_check", entity.getType().toString(), player.getName(), distance, inRange);
        
        return inRange;
    }
    
    /**
     * 批量更新属性（如属性批量更新而不是单独更新每个属性）
     * @param entityId 实体ID
     * @param attributes 属性映射
     */
    public static void batchUpdateAttributes(String entityId, Map<String, Double> attributes) {
        if (attributes.isEmpty()) {
            return;
        }
        
        DebugUtils.log("optimization.attribute_update", entityId, String.join(", ", attributes.keySet()));
        
        // 这里只是记录调试信息，实际批量更新逻辑需要在相应的Handler类中实现
    }
    
    /**
     * 批量发送数据包工具方法
     * @param packetType 数据包类型
     * @param packetsCount 数据包数量
     * @param receivers 接收者数量
     */
    public static void logPacketBatch(String packetType, int packetsCount, int receivers) {
        DebugUtils.log("optimization.packet_batch", packetType, packetsCount, receivers);
    }
} 