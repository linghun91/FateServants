package cn.i7mc.fateservants.packets;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public class PacketHandler {
    private final FateServants plugin;
    private final AtomicInteger nextEntityId;

    public PacketHandler(FateServants plugin) {
        this.plugin = plugin;
        this.nextEntityId = new AtomicInteger(Integer.MAX_VALUE / 2);
        
        // 注册数据包监听器
        registerPacketListeners();
    }

    private void registerPacketListeners() {
        // 实体交互监听器
        plugin.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, PacketType.Play.Client.USE_ENTITY) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    Player player = event.getPlayer();
                    int targetId = packet.getIntegers().read(0);
                    
                    // 检查是否是英灵
                    for (Servant servant : FateServants.getInstance().getServantManager().getAllServants()) {
                        // 检查是否点击了任何一个全息图实体
                        if (servant.isHologramEntity(targetId)) {
                            // 处理攻击逻辑
                            handleServantAttack(player, servant);
                            event.setCancelled(true);
                            break;
                        }
                    }
                }
            }
        );
    }

    private void handleServantAttack(Player attacker, Servant servant) {
        // 检查是否可以攻击
        if (servant.getOwner().equals(attacker)) {
            return; // 不能攻击自己的英灵
        }
        
        // 计算伤害
        double damage = 1.0; // 基础伤害，可以根据攻击者的属性进行调整
        servant.setHealth(servant.getCurrentHealth() - damage);
        
        // 如果英灵死亡
        if (servant.getCurrentHealth() <= 0) {
            FateServants.getInstance().getServantManager().removeServant(servant.getOwner());
            servant.getOwner().sendMessage("你的英灵被击败了！");
            attacker.sendMessage("你击败了一个英灵！");
        }
    }

    /**
     * 获取下一个可用的实体ID
     */
    public int getNextEntityId() {
        return nextEntityId.decrementAndGet();
    }
} 