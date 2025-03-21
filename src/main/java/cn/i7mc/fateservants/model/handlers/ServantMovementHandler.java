package cn.i7mc.fateservants.model.handlers;

import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 英灵移动处理器
 * 负责处理英灵的移动相关功能
 */
public class ServantMovementHandler {
    private final Servant servant;
    
    // 移动配置
    private static final double FOLLOW_DISTANCE = 2.0;
    private static final double MAX_TELEPORT_DISTANCE = 30.0;
    
    /**
     * 构造函数
     * @param servant 所属的英灵
     */
    public ServantMovementHandler(Servant servant) {
        this.servant = servant;
    }
    
    /**
     * 移动到目标位置
     * @param targetLocation 目标位置
     * @return 是否成功移动
     */
    public boolean moveTo(Location targetLocation) {
        if (targetLocation == null || servant.getLocation() == null) {
            DebugUtils.debug("movement.invalid_location", 
                servant.getServantClass().getDisplayName(),
                targetLocation == null ? "目标位置为null" : "当前位置为null");
            return false;
        }
        
        // 记录起始位置
        Location startLoc = servant.getLocation().clone();
        
        // 计算移动向量
        Vector direction = targetLocation.toVector().subtract(servant.getLocation().toVector());
        
        // 如果距离小于阈值，则认为已经到达
        if (direction.length() < 0.5) {
            DebugUtils.debug("movement.already_arrived", 
                servant.getServantClass().getDisplayName(),
                formatLocation(targetLocation),
                direction.length());
            return true;
        }
        
        // 获取移动速度，确保使用正确的速度值
        double movementSpeed = servant.getMovementSpeed();
        
        // 归一化并设置速度
        direction.normalize().multiply(movementSpeed);
        
        // 应用移动
        Location newLocation = servant.getLocation().add(direction);
        servant.teleport(newLocation);
        
        // 详细记录移动过程
        DebugUtils.debug("movement.move_details", 
            servant.getServantClass().getDisplayName(),
            formatLocation(startLoc),
            formatLocation(newLocation),
            direction.length(),
            direction.getX(), direction.getY(), direction.getZ(),
            movementSpeed,
            "已执行");
            
        return true;
    }
    
    /**
     * 跟随主人
     * @return 是否成功跟随
     */
    public boolean followOwner() {
        Player owner = servant.getOwner();
        if (owner == null || !owner.isOnline() || servant.getLocation() == null) {
            return false;
        }
        
        Location ownerLocation = owner.getLocation();
        double distance = servant.getLocation().distance(ownerLocation);
        
        // 如果距离过远，直接传送
        if (distance > MAX_TELEPORT_DISTANCE) {
            Location teleportLocation = findSafeLocationNear(ownerLocation);
            servant.teleport(teleportLocation);
            
            DebugUtils.log("movement.teleport", 
                servant.getServantClass().getDisplayName(),
                formatLocation(teleportLocation),
                distance);
                
            return true;
        }
        
        // 如果距离适中，则移动
        if (distance > FOLLOW_DISTANCE) {
            // 获取目标位置 (在主人后方)
            Vector ownerDirection = ownerLocation.getDirection().normalize();
            Location targetLocation = ownerLocation.clone().subtract(ownerDirection.multiply(FOLLOW_DISTANCE));
            
            return moveTo(targetLocation);
        }
        
        return true;
    }
    
    /**
     * 在指定位置附近寻找安全的传送点
     * @param baseLocation 基准位置
     * @return 安全的传送点
     */
    private Location findSafeLocationNear(Location baseLocation) {
        // 简单实现：在基准位置附近寻找安全位置
        // 可以根据需要扩展更复杂的安全位置检测
        
        Location safeLocation = baseLocation.clone();
        
        // 确保不在方块内部
        while (!safeLocation.getBlock().isEmpty() && 
               !safeLocation.getBlock().isLiquid()) {
            safeLocation.add(0, 1, 0);
        }
        
        return safeLocation;
    }
    
    /**
     * 根据战斗目标移动
     * @param combatHandler 战斗处理器
     * @return 是否成功移动
     */
    public boolean moveTowardsTarget(ServantCombatHandler combatHandler) {
        if (combatHandler == null || !combatHandler.isTargetValid() || servant.getLocation() == null) {
            return false;
        }
        
        // 获取目标位置
        Location targetLocation = combatHandler.getTarget().getLocation();
        double distance = servant.getLocation().distance(targetLocation);
        
        // 如果已经在攻击范围内，不需要移动
        if (combatHandler.isInAttackRange()) {
            // 调整面向目标
            faceLocation(targetLocation);
            return true;
        }
        
        // 获取移动方向
        Vector direction = combatHandler.getMoveDirection();
        if (direction == null) {
            return false;
        }
        
        // 应用移动
        Location newLocation = servant.getLocation().add(direction.multiply(servant.getMovementSpeed()));
        servant.teleport(newLocation);
        
        // 调整面向目标
        faceLocation(targetLocation);
        
        DebugUtils.log("movement.combat_move", 
            servant.getServantClass().getDisplayName(),
            formatLocation(newLocation),
            distance);
            
        return true;
    }
    
    /**
     * 使英灵面向特定位置
     * @param targetLocation 目标位置
     */
    public void faceLocation(Location targetLocation) {
        if (targetLocation == null || servant.getLocation() == null) {
            return;
        }
        
        // 计算朝向向量
        Vector direction = targetLocation.toVector().subtract(servant.getLocation().toVector()).normalize();
        
        // 设置朝向
        Location newLocation = servant.getLocation().clone();
        newLocation.setDirection(direction);
        
        // 仅更新朝向，不改变位置
        servant.teleport(newLocation);
    }
    
    /**
     * 格式化位置信息
     * @param location 位置
     * @return 格式化的位置字符串
     */
    private String formatLocation(Location location) {
        if (location == null) {
            return "null";
        }
        
        return String.format("(%.2f, %.2f, %.2f, %s)", 
            location.getX(), location.getY(), location.getZ(),
            location.getWorld() != null ? location.getWorld().getName() : "null");
    }
} 