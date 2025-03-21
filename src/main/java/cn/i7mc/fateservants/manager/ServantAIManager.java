package cn.i7mc.fateservants.manager;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class ServantAIManager {
    private final FateServants plugin;
    private int taskId = -1;

    public ServantAIManager(FateServants plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (taskId != -1) return;

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                Collection<Servant> servants = plugin.getServantManager().getAllServants();
                for (Servant servant : servants) {
                    servant.updateAI();
                }
            }
        }.runTaskTimer(plugin, 1L, 5L).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
