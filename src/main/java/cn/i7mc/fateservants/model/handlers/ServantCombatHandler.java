package cn.i7mc.fateservants.model.handlers;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.skills.Skill;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 英灵战斗处理器
 * 负责处理英灵的战斗相关功能
 */
public class ServantCombatHandler {
    private final Servant servant;
    private Monster target;
    private int attackCooldown = 0;
    private int normalAttackCooldown = 0;
    private final Map<String, Integer> skillCooldowns = new HashMap<>();
    
    // 战斗距离配置
    private static final double ATTACK_RANGE = 2.0;
    private static final double TARGET_SEARCH_RANGE = 10.0;
    
    /**
     * 构造函数
     * @param servant 所属的英灵
     */
    public ServantCombatHandler(Servant servant) {
        this.servant = servant;
    }
    
    /**
     * 设置目标
     * @param target 目标生物
     */
    public void setTarget(Monster target) {
        this.target = target;
    }
    
    /**
     * 获取当前目标
     * @return 目标生物
     */
    public Monster getTarget() {
        return target;
    }
    
    /**
     * 重置所有冷却
     */
    public void resetCooldowns() {
        attackCooldown = 0;
        normalAttackCooldown = 0;
        skillCooldowns.clear();
    }
    
    /**
     * 更新冷却
     */
    public void updateCooldowns() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
        if (normalAttackCooldown > 0) {
            normalAttackCooldown--;
        }
        
        // 更新技能冷却
        skillCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }
    
    /**
     * 检查攻击目标是否有效
     * @return 目标是否有效
     */
    public boolean isTargetValid() {
        return target != null && !target.isDead() && target.isValid();
    }
    
    /**
     * 计算与目标的距离
     * @return 距离，如果目标无效则返回-1
     */
    public double getDistanceToTarget() {
        if (!isTargetValid() || servant.getLocation() == null) {
            return -1;
        }
        
        return servant.getLocation().distance(target.getLocation());
    }
    
    /**
     * 检查是否在攻击范围内
     * @return 是否在攻击范围内
     */
    public boolean isInAttackRange() {
        double distance = getDistanceToTarget();
        return distance >= 0 && distance <= ATTACK_RANGE;
    }
    
    /**
     * 搜索附近怪物作为目标
     * @return 是否找到目标
     */
    public boolean searchTarget() {
        if (servant.getLocation() == null) {
            return false;
        }
        
        Collection<Monster> nearbyMonsters = servant.getLocation().getWorld().getEntitiesByClass(Monster.class);
        Monster closest = null;
        double minDistance = TARGET_SEARCH_RANGE;
        
        for (Monster monster : nearbyMonsters) {
            if (monster.isDead() || !monster.isValid()) {
                continue;
            }
            
            double distance = servant.getLocation().distance(monster.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
                closest = monster;
            }
        }
        
        if (closest != null) {
            setTarget(closest);
            DebugUtils.log("combat.target_found", closest.getType().name(), minDistance);
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取移动方向向量
     * @return 移动方向，如果目标无效则返回null
     */
    public Vector getMoveDirection() {
        if (!isTargetValid() || servant.getLocation() == null) {
            return null;
        }
        
        return target.getLocation().toVector().subtract(servant.getLocation().toVector()).normalize();
    }
    
    /**
     * 执行普通攻击
     * @return 是否成功攻击
     */
    public boolean performNormalAttack() {
        if (!isTargetValid() || !isInAttackRange() || normalAttackCooldown > 0) {
            return false;
        }
        
        // 设置普通攻击冷却
        normalAttackCooldown = servant.getServantClass().getAttackSpeed();
        DebugUtils.log("combat.normal_attack", 
            servant.getServantClass().getDisplayName(), 
            target.getType().name(), 
            normalAttackCooldown);
            
        // 计算基础伤害
        double damage = servant.getAttributeHandler().calculateAttackDamage();
        
        // 应用属性增益
        servant.getAttributeHandler().applyAttributePlusDamage(target, damage);
        
        // 触发战斗特效
        playAttackEffect(target.getLocation());
        
        return true;
    }
    
    /**
     * 尝试施放技能
     * @return 是否成功施放
     */
    public boolean attemptCastSkill() {
        if (!isTargetValid() || attackCooldown > 0) {
            return false;
        }
        
        List<Skill> skills = servant.getSkills();
        if (skills.isEmpty()) {
            return false;
        }
        
        // 随机选择一个可用技能
        Skill selectedSkill = null;
        for (Skill skill : skills) {
            if (canCastSkill(skill)) {
                selectedSkill = skill;
                break;
            }
        }
        
        if (selectedSkill == null) {
            return false;
        }
        
        // 施放技能
        boolean success = selectedSkill.cast(servant, target); // 使用简化的cast方法
        
        if (success) {
            // 设置技能冷却
            int cooldown = selectedSkill.getCooldown();
            skillCooldowns.put(selectedSkill.getName(), cooldown);
            
            // 设置全局攻击冷却
            attackCooldown = 20; // 1秒的全局冷却
            
            DebugUtils.log("combat.skill_cast", 
                selectedSkill.getName(), 
                target.getType().name(),
                cooldown);
                
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查技能是否可用
     * @param skill 技能
     * @return 是否可用
     */
    private boolean canCastSkill(Skill skill) {
        // 检查冷却
        if (skillCooldowns.containsKey(skill.getName()) && skillCooldowns.get(skill.getName()) > 0) {
            return false;
        }
        
        // 检查魔法值
        if (skill.getManaCost() > servant.getCurrentMana()) {
            return false;
        }
        
        // 检查施放条件
        return skill.canCast(servant);
    }
    
    /**
     * 播放攻击特效
     * @param targetLocation 目标位置
     */
    private void playAttackEffect(Location targetLocation) {
        if (targetLocation == null) {
            return;
        }
        
        // 在目标位置播放粒子效果
        targetLocation.getWorld().spawnParticle(
            org.bukkit.Particle.CRIT, 
            targetLocation.clone().add(0, 1, 0), 
            10, 0.3, 0.3, 0.3, 0.1
        );
    }
    
    /**
     * 处理被袭击事件
     * @param attacker 攻击者
     * @param damage 伤害值
     */
    public void handleAttacked(LivingEntity attacker, double damage) {
        if (attacker instanceof Monster && target == null) {
            // 如果攻击者是怪物且当前没有目标，将其设为目标
            setTarget((Monster) attacker);
            DebugUtils.log("combat.attacked", 
                servant.getServantClass().getDisplayName(), 
                attacker.getType().name(), 
                damage);
        }
        
        // 减少生命值
        double newHealth = servant.getCurrentHealth() - damage;
        servant.setHealth(Math.max(0, newHealth));
        
        // 检查是否死亡
        if (newHealth <= 0) {
            handleDeath();
        }
    }
    
    /**
     * 处理英灵死亡
     */
    private void handleDeath() {
        DebugUtils.log("combat.death", servant.getServantClass().getDisplayName());
        
        // 通知主人
        Player owner = servant.getOwner();
        if (owner != null && owner.isOnline()) {
            owner.sendMessage("§c你的英灵已阵亡！");
        }
        
        // 添加死亡特效
        Location loc = servant.getLocation();
        if (loc != null) {
            loc.getWorld().spawnParticle(
                org.bukkit.Particle.EXPLOSION_LARGE, 
                loc, 1, 0, 0, 0, 0
            );
        }
    }
    
    /**
     * 获取技能的剩余冷却时间
     * @param skillId 技能ID
     * @return 剩余冷却时间
     */
    public int getSkillCooldown(String skillId) {
        return skillCooldowns.getOrDefault(skillId, 0);
    }
} 