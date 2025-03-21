package cn.i7mc.fateservants.model;

/**
 * 英灵状态枚举
 * 表示英灵当前的行为状态
 */
public enum ServantState {
    /**
     * 空闲状态
     * 英灵不执行任何特定行为，只是站在原地
     */
    IDLE,
    
    /**
     * 跟随状态
     * 英灵跟随主人移动
     */
    FOLLOWING,
    
    /**
     * 战斗状态
     * 英灵正在与敌人战斗
     */
    COMBAT,
    
    /**
     * 返回状态
     * 英灵正在返回主人身边
     */
    RETURN
} 