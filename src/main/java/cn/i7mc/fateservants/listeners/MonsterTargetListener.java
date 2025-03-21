package cn.i7mc.fateservants.listeners;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.manager.ServantManager;
import cn.i7mc.fateservants.model.Servant;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

public class MonsterTargetListener implements Listener {
    private final FateServants plugin;
    private final ServantManager servantManager;
    private static final double TARGET_RANGE = 16.0; // 怪物的索敌范围

    public MonsterTargetListener(FateServants plugin) {
        this.plugin = plugin;
        this.servantManager = plugin.getServantManager();
    }

    @EventHandler
    public void onMonsterTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        Monster monster = (Monster) event.getEntity();
        Location monsterLoc = monster.getLocation();

        // 检查附近的英灵
        for (Servant servant : servantManager.getServants()) {
            // 只考虑处于战斗状态的英灵
            if (!servant.isAttacking()) {
                continue;
            }

            Location servantLoc = servant.getLocation();
            if (servantLoc.getWorld() != monsterLoc.getWorld()) {
                continue;
            }

            double distance = servantLoc.distance(monsterLoc);
            if (distance <= TARGET_RANGE) {
                // 有一定概率将英灵作为目标
                if (Math.random() < 0.3) { // 30%的概率
                    // 取消原目标,将怪物的仇恨转向英灵的主人
                    event.setCancelled(true);
                    monster.setTarget(servant.getOwner());
                    break;
                }
            }
        }
    }
}
