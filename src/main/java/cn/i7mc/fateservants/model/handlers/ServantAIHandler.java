package cn.i7mc.fateservants.model.handlers;

import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantState;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.entity.Player;

/**
 * 英灵AI处理器
 * 负责处理英灵的AI行为逻辑
 */
public class ServantAIHandler {
    private final Servant servant;
    private final ServantCombatHandler combatHandler;
    private final ServantMovementHandler movementHandler;
    
    private ServantState currentState = ServantState.IDLE;
    private int stateTimer = 0;
    private int regenerationTimer = 0;
    
    // AI配置
    private static final int IDLE_TIME = 100; // 5秒
    private static final int SEARCH_INTERVAL = 20; // 1秒
    private static final int REGEN_INTERVAL = 60; // 3秒
    private static final double MANA_REGEN_AMOUNT = 5.0;
    private static final double HEALTH_REGEN_AMOUNT = 2.0;
    
    /**
     * 构造函数
     * @param servant 所属的英灵
     * @param combatHandler 战斗处理器
     * @param movementHandler 移动处理器
     */
    public ServantAIHandler(Servant servant, 
                            ServantCombatHandler combatHandler, 
                            ServantMovementHandler movementHandler) {
        this.servant = servant;
        this.combatHandler = combatHandler;
        this.movementHandler = movementHandler;
    }
    
    /**
     * 获取当前状态
     * @return 当前状态
     */
    public ServantState getCurrentState() {
        return currentState;
    }
    
    /**
     * 设置当前状态
     * @param state 新状态
     */
    public void setState(ServantState state) {
        if (this.currentState != state) {
            DebugUtils.log("ai.state_change", 
                servant.getServantClass().getDisplayName(),
                this.currentState.toString(),
                state.toString());
                
            this.currentState = state;
            this.stateTimer = 0;
        }
    }
    
    /**
     * 更新AI
     */
    public void updateAI() {
        // 更新计时器
        stateTimer++;
        regenerationTimer++;
        
        // 处理生命值和魔力恢复
        if (regenerationTimer >= REGEN_INTERVAL) {
            regenerateStats();
            regenerationTimer = 0;
        }
        
        // 更新战斗冷却
        combatHandler.updateCooldowns();
        
        // 根据当前状态执行对应行为
        switch (currentState) {
            case IDLE:
                handleIdleState();
                break;
            case FOLLOWING:
                handleFollowingState();
                break;
            case COMBAT:
                handleCombatState();
                break;
            case RETURN:
                handleReturnState();
                break;
        }
    }
    
    /**
     * 处理空闲状态
     */
    private void handleIdleState() {
        // 检查是否有目标
        if (stateTimer % SEARCH_INTERVAL == 0) {
            boolean foundTarget = combatHandler.searchTarget();
            if (foundTarget) {
                setState(ServantState.COMBAT);
                return;
            }
        }
        
        // 超过空闲时间，切换到跟随状态
        if (stateTimer > IDLE_TIME) {
            setState(ServantState.FOLLOWING);
        }
    }
    
    /**
     * 处理跟随状态
     */
    private void handleFollowingState() {
        // 检查主人是否有效
        Player owner = servant.getOwner();
        if (owner == null || !owner.isOnline()) {
            setState(ServantState.IDLE);
            return;
        }
        
        // 跟随主人
        boolean followSuccess = movementHandler.followOwner();
        if (!followSuccess) {
            DebugUtils.log("ai.follow_failed", 
                servant.getServantClass().getDisplayName());
        }
        
        // 检查是否有目标
        if (stateTimer % SEARCH_INTERVAL == 0) {
            boolean foundTarget = combatHandler.searchTarget();
            if (foundTarget) {
                setState(ServantState.COMBAT);
            }
        }
    }
    
    /**
     * 处理战斗状态
     */
    private void handleCombatState() {
        // 检查目标是否有效
        if (!combatHandler.isTargetValid()) {
            setState(ServantState.RETURN);
            return;
        }
        
        // 移动到目标
        movementHandler.moveTowardsTarget(combatHandler);
        
        // 尝试施放技能
        boolean castSkill = combatHandler.attemptCastSkill();
        
        // 如果没有施放技能，尝试普通攻击
        if (!castSkill) {
            combatHandler.performNormalAttack();
        }
        
        // 检查与主人的距离，如果过远则返回
        Player owner = servant.getOwner();
        if (owner != null && owner.isOnline() && servant.getLocation() != null) {
            double distanceToOwner = servant.getLocation().distance(owner.getLocation());
            if (distanceToOwner > 20.0) { // 超过20格距离，返回主人身边
                setState(ServantState.RETURN);
            }
        }
    }
    
    /**
     * 处理返回状态
     */
    private void handleReturnState() {
        // 清除目标
        combatHandler.setTarget(null);
        
        // 跟随主人
        boolean followSuccess = movementHandler.followOwner();
        
        // 如果已经靠近主人，回到跟随状态
        if (followSuccess) {
            Player owner = servant.getOwner();
            if (owner != null && owner.isOnline() && servant.getLocation() != null) {
                double distanceToOwner = servant.getLocation().distance(owner.getLocation());
                if (distanceToOwner < 5.0) { // 距离小于5格，恢复跟随状态
                    setState(ServantState.FOLLOWING);
                }
            }
        } else {
            // 如果无法跟随，切换到空闲状态
            setState(ServantState.IDLE);
        }
    }
    
    /**
     * 恢复生命值和魔力
     */
    private void regenerateStats() {
        // 非战斗状态下恢复更多
        double healthRegenMultiplier = currentState == ServantState.COMBAT ? 0.5 : 1.0;
        double manaRegenMultiplier = currentState == ServantState.COMBAT ? 0.5 : 1.0;
        
        // 恢复生命值
        double currentHealth = servant.getCurrentHealth();
        double maxHealth = servant.getMaxHealth();
        if (currentHealth < maxHealth) {
            double newHealth = Math.min(currentHealth + HEALTH_REGEN_AMOUNT * healthRegenMultiplier, maxHealth);
            servant.setHealth(newHealth);
            
            DebugUtils.log("ai.health_regen", 
                servant.getServantClass().getDisplayName(),
                currentHealth,
                newHealth);
        }
        
        // 恢复魔力
        double currentMana = servant.getCurrentMana();
        double maxMana = servant.getMaxMana();
        if (currentMana < maxMana) {
            double newMana = Math.min(currentMana + MANA_REGEN_AMOUNT * manaRegenMultiplier, maxMana);
            servant.setCurrentMana(newMana);
            
            DebugUtils.log("ai.mana_regen", 
                servant.getServantClass().getDisplayName(),
                currentMana,
                newMana);
        }
    }
} 