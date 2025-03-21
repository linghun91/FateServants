package cn.i7mc.fateservants.listeners;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import cn.i7mc.fateservants.utils.DebugUtils;

public class PlayerJoinListener implements Listener {
    
    private final FateServants plugin;
    
    public PlayerJoinListener(FateServants plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        // 延迟10ticks后完全加载玩家英灵
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // 从数据库加载玩家英灵
                    Servant servant = plugin.getDatabaseManager().loadServant(player);
                    if (servant != null) {
                        // 先移除如果已经有的英灵
                        if (plugin.getServantManager().hasServant(player)) {
                            plugin.getServantManager().removeServant(player);
                        }
                        
                        // 直接添加数据库加载的英灵到管理器中，而不是创建新实例
                        plugin.getServantManager().addServant(player, servant);
                        
                        // 确保英灵被正确生成
                        servant.spawn();
                        
                        // 如果必要，更新英灵位置
                        if (servant.getLocation() != null) {
                            servant.teleport(servant.getLocation());
                        }
                        
                        // 确保英灵AI控制器正确设置为跟随模式
                        if (servant.getAIController() != null) {
                            servant.getAIController().setBehavior(cn.i7mc.fateservants.ai.ServantBehavior.FOLLOW);
                        }
                        
                        // 添加调试日志，确认AI控制器已设置
                        DebugUtils.debug("servant.ai_controller_set", player.getName(), 
                            servant.getServantClass() != null ? servant.getServantClass().getId() : "null");
                        
                        DebugUtils.debug("debug.servantdata.load.success", player.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("加载 " + player.getName() + " 的英灵数据时发生错误: " + e.getMessage());
                    DebugUtils.logException("debug.servantdata.load.error", e, player.getName());
                }
            }
        }.runTaskLater(plugin, 10L);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getServantManager().hasServant(player)) {
            // 先获取玩家的英灵
            Servant servant = plugin.getServantManager().getServant(player);
            if (servant != null) {
                try {
                    // 先保存英灵数据到数据库
                    plugin.getDatabaseManager().saveServant(player, servant);
                    DebugUtils.debug("debug.servantdata.save.success", player.getName());
                } catch (Exception e) {
                    plugin.getLogger().severe("保存 " + player.getName() + " 的英灵数据时发生错误: " + e.getMessage());
                    DebugUtils.logException("debug.servantdata.save.error", e, player.getName());
                }
            }
            // 清理英灵实体
            plugin.getServantManager().removeServant(player);
        }
    }
} 