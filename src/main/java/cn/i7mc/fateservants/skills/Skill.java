package cn.i7mc.fateservants.skills;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 技能类，代表一个英灵技能
 */
public class Skill {
    private final String name;
    private final String castType;  // "active" 或 "passive"
    private final double manaCost;
    private final int cooldown;     // ticks
    private final double chance;
    private final String description;
    
    private long lastCastTime = 0;  // 上次施法时间

    public Skill(String name, String castType, double manaCost, int cooldown, double chance, String description) {
        this.name = name;
        this.castType = castType;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.chance = chance;
        this.description = description;
    }

    /**
     * 检查是否可以施放技能
     * @param servant 施法者
     * @return 是否可以施放
     */
    public boolean canCast(Servant servant) {
        // 检查冷却
        if (isOnCooldown()) {
            return false;
        }

        // 检查魔法值
        if (servant.getCurrentMana() < manaCost) {
            return false;
        }

        return true;
    }

    /**
     * 主动施放技能
     * @param servant 施法者
     * @param target 目标实体
     * @return 是否施放成功
     */
    public boolean cast(Servant servant, LivingEntity target) {
        DebugUtils.log("skill.cast_attempt", name);
        
        if (!castType.equals("active")) {
            DebugUtils.log("skill.cast_failed", name, "不是主动技能");
            return false;
        }

        if (isOnCooldown()) {
            DebugUtils.log("skill.cooldown", name, getRemainingCooldown() / 20.0);
            return false;
        }

        if (servant.getCurrentMana() < manaCost) {
            DebugUtils.log("skill.cast_failed", name, "魔法值不足");
            return false;
        }

        // 消耗魔法值
        double oldMana = servant.getCurrentMana();
        servant.setCurrentMana(oldMana - manaCost);
        DebugUtils.log("skill.mana_cost", name, manaCost, servant.getCurrentMana(), servant.getMaxMana());
        
        // 更新冷却时间
        lastCastTime = System.currentTimeMillis();

        // 使用MythicMobs执行技能
        boolean success = FateServants.getInstance().getSkillManager().castSkill(
            servant.getOwner(),  // 使用主人作为施法者
            name,
            1.0f  // 默认威力
        );

        // 如果技能释放成功,发送提示给主人并播放特效
        if (success && servant.getOwner() instanceof Player) {
            Player owner = (Player) servant.getOwner();
            owner.sendMessage(MessageManager.get("skills.cast_success", name));
            
            // 发送魔法值和经验值信息到ActionBar
            sendStatusBar(servant, owner);
        }

        return success;
    }

    /**
     * 尝试触发被动技能
     * @param servant 施法者
     * @param target 目标实体
     * @return 是否触发成功
     */
    public boolean tryTrigger(Servant servant, LivingEntity target) {
        if (!castType.equals("passive")) {
            return false;
        }

        // 检查触发概率
        if (Math.random() * 100 > chance) {
            return false;
        }

        DebugUtils.log("skill.passive_proc", name, chance);

        if (!canCast(servant)) {
            return false;
        }

        // 消耗魔法值
        double oldMana = servant.getCurrentMana();
        servant.setCurrentMana(oldMana - manaCost);
        DebugUtils.log("skill.mana_cost", name, manaCost, servant.getCurrentMana(), servant.getMaxMana());
        
        // 更新冷却时间
        lastCastTime = System.currentTimeMillis();

        // 使用MythicMobs执行技能
        boolean success = FateServants.getInstance().getSkillManager().castSkill(
            servant.getOwner(),
            name,
            1.0f
        );

        // 如果技能触发成功,发送提示给主人
        if (success && servant.getOwner() instanceof Player) {
            Player owner = (Player) servant.getOwner();
            owner.sendMessage(MessageManager.get("skills.passive_trigger", name));
        }

        return success;
    }

    /**
     * 检查技能是否在冷却中
     * @return 是否在冷却中
     */
    public boolean isOnCooldown() {
        if (cooldown <= 0) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastCastTime;
        
        return elapsedTime < (cooldown * 50);  // 转换为毫秒 (1 tick = 50ms)
    }

    /**
     * 获取剩余冷却时间(ticks)
     * @return 剩余冷却时间
     */
    public int getRemainingCooldown() {
        if (!isOnCooldown()) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastCastTime;
        int remainingMs = (int) ((cooldown * 50) - elapsedTime);
        
        return Math.max(0, remainingMs / 50);  // 转换为ticks
    }

    /**
     * 发送状态栏信息到玩家的ActionBar
     * @param servant 英灵
     * @param player 玩家
     */
    private void sendStatusBar(Servant servant, Player player) {
        try {
            // 构建状态栏消息
            String manaFormat = MessageManager.get("gui.status_bar.mana");
            String expFormat = MessageManager.get("gui.status_bar.exp");
            
            String manaInfo = String.format(manaFormat, 
                servant.getCurrentMana(), servant.getMaxMana());
            
            String expInfo = String.format(expFormat, 
                servant.getExperience(), servant.getRequiredExperience());
            
            String actionBarMessage = manaInfo + " " + expInfo;
            
            // 创建聊天数据包
            PacketContainer actionBarPacket = FateServants.getInstance().getProtocolManager().createPacket(PacketType.Play.Server.CHAT);
            
            // 创建JSON格式的ActionBar消息
            String jsonMessage = "{\"text\":\"" + actionBarMessage + "\"}";
            actionBarPacket.getChatComponents().write(0, WrappedChatComponent.fromJson(jsonMessage));
            actionBarPacket.getChatTypes().write(0, EnumWrappers.ChatType.GAME_INFO);
            
            // 发送数据包
            FateServants.getInstance().getProtocolManager().sendServerPacket(player, actionBarPacket);
        } catch (Exception e) {
            DebugUtils.log("gui.action_bar_error", e.getMessage());
        }
    }

    // Getters
    public String getName() { return name; }
    public String getCastType() { return castType; }
    public double getManaCost() { return manaCost; }
    public int getCooldown() { return cooldown; }
    public double getChance() { return chance; }
    public String getDescription() { return description; }
} 