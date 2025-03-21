package cn.i7mc.fateservants.ai;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;

public class ServantAIController {
    private final Servant servant;
    private ServantBehavior currentBehavior = ServantBehavior.FOLLOW;
    private Entity target;
    private final double FOLLOW_DISTANCE;
    private final double COMBAT_RANGE;
    private final double DEFEND_RANGE;
    private final double ATTACK_RANGE;
    private final FateServants plugin;
    private long lastTeleportTime = 0; // 添加上次传送时间记录
    private static final long TELEPORT_COOLDOWN = 10; // 传送后的目标搜索冷却时间（tick）
    private LivingEntity currentTarget = null;  // 添加当前目标字段

    public ServantAIController(Servant servant) {
        this.servant = servant;
        this.plugin = FateServants.getInstance();
        
        // 从配置文件加载所有距离参数
        FileConfiguration config = plugin.getConfig();
        this.FOLLOW_DISTANCE = config.getDouble("servants.follow_distance", 2.0);
        this.COMBAT_RANGE = config.getDouble("servants.combat_search_range", 16.0);
        this.DEFEND_RANGE = config.getDouble("servants.defend_search_range", 8.0);
        this.ATTACK_RANGE = config.getDouble("servants.attack_range", 2.0);
    }

    public void setBehavior(ServantBehavior behavior) {
        this.currentBehavior = behavior;
        this.target = null; // 切换行为时重置目标
    }

    public ServantBehavior getCurrentBehavior() {
        return currentBehavior;
    }

    public void tick() {
        Player owner = servant.getOwner();
        if (owner == null || !owner.isOnline()) {
            return;
        }

        switch (currentBehavior) {
            case IDLE:
                // 在待机状态下，只需要保持在原地
                break;
            case FOLLOW:
                handleFollow();
                break;
            case COMBAT:
                handleCombat();
                break;
            case DEFEND:
                handleDefend();
                break;
        }
    }

    private void moveToLocation(Location targetLoc, Location currentLoc) {
        // 统一的移动逻辑
        Vector direction = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
        Location newLoc = currentLoc.clone().add(direction.multiply(servant.getMovementSpeed()));
        
        // 更新位置
        servant.teleport(newLoc);
        
        // 更新朝向
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        servant.setRotation(yaw, currentLoc.getPitch());
    }

    private void handleFollow() {
        Player owner = servant.getOwner();
        Location servantLoc = servant.getLocation();
        
        if (owner == null || !owner.isOnline()) return;

        Location ownerLoc = owner.getLocation();
        double distance = ownerLoc.distance(servantLoc);
        
        // 如果超出最大跟随距离，直接传送到主人身边并清空目标
        if (distance > plugin.getConfig().getDouble("servants.max_follow_distance", 16)) {
            this.target = null;
            this.lastTeleportTime = System.currentTimeMillis();
            servant.teleport(ownerLoc.clone());
            return;
        }
        
        if (distance > FOLLOW_DISTANCE) {
            moveToLocation(ownerLoc, servantLoc);
        }
    }

    private void handleCombat() {
        if (System.currentTimeMillis() - lastTeleportTime < TELEPORT_COOLDOWN * 50) {
            return;
        }

        Location servantLoc = servant.getLocation();
        
        // 寻找最近的怪物作为目标
        Monster target = findNearestMonster(servantLoc);
        currentTarget = target;  // 更新当前目标
        
        // 如果找到目标，进行追击和攻击
        if (target != null && target.isValid() && !target.isDead()) {
            Location targetLoc = target.getLocation();
            double distance = targetLoc.distance(servantLoc);
            
            if (distance > ATTACK_RANGE) {
                moveToLocation(targetLoc, servantLoc);
            } else {
                servant.attack(target);
            }
        } else {
            // 无目标时，保持在主人附近
            Player owner = servant.getOwner();
            if (owner != null && owner.isOnline()) {
                Location ownerLoc = owner.getLocation();
                double distance = ownerLoc.distance(servantLoc);
                
                if (distance > FOLLOW_DISTANCE) {
                    moveToLocation(ownerLoc, servantLoc);
                }
            }
        }
    }

    private void handleDefend() {
        if (System.currentTimeMillis() - lastTeleportTime < TELEPORT_COOLDOWN * 50) {
            return;
        }

        Player owner = servant.getOwner();
        Location servantLoc = servant.getLocation();
        
        if (owner == null || !owner.isOnline()) return;

        // 如果没有目标或目标无效，检查是否有敌对生物接近主人
        if (target == null || !target.isValid() || target.isDead()) {
            // 只获取附近的实体
            Collection<Entity> nearbyEntities = owner.getLocation().getWorld().getNearbyEntities(owner.getLocation(), DEFEND_RANGE, DEFEND_RANGE, DEFEND_RANGE);
            
            double closestDistance = Double.MAX_VALUE;
            Entity closestEnemy = null;
            
            for (Entity entity : nearbyEntities) {
                if (entity instanceof Monster && !entity.isDead()) {
                    double distanceToOwner = entity.getLocation().distance(owner.getLocation());
                    if (distanceToOwner < DEFEND_RANGE && distanceToOwner < closestDistance) {
                        closestDistance = distanceToOwner;
                        closestEnemy = entity;
                    }
                }
            }
            
            target = closestEnemy;
        }

        // 如果找到威胁目标，进行防御
        if (target != null && target.isValid() && !target.isDead()) {
            Location targetLoc = target.getLocation();
            double distance = targetLoc.distance(servantLoc);
            
            if (distance > ATTACK_RANGE) {
                moveToLocation(targetLoc, servantLoc);
            } else {
                servant.attack((Monster)target);
            }
        } else {
            // 没有威胁时保持在主人附近
            Location ownerLoc = owner.getLocation();
            double distance = ownerLoc.distance(servantLoc);
            
            if (distance > FOLLOW_DISTANCE) {
                moveToLocation(ownerLoc, servantLoc);
            }
        }
    }

    /**
     * 获取当前目标
     * @return 当前目标实体，如果没有目标则返回null
     */
    public LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    /**
     * 寻找最近的怪物目标
     * @param location 搜索的中心位置
     * @return 最近的怪物，如果没有找到则返回null
     */
    private Monster findNearestMonster(Location location) {
        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, COMBAT_RANGE, COMBAT_RANGE, COMBAT_RANGE);
        
        double closestDistance = Double.MAX_VALUE;
        Monster closestMonster = null;
        
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Monster && !entity.isDead()) {
                double distance = entity.getLocation().distance(location);
                if (distance < COMBAT_RANGE && distance < closestDistance) {
                    closestDistance = distance;
                    closestMonster = (Monster) entity;
                }
            }
        }
        
        return closestMonster;
    }
}
