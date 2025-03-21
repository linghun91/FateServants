package cn.i7mc.fateservants.gui;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class ServantGUIListener implements Listener {
    private final FateServants plugin;

    public ServantGUIListener(FateServants plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Servant servant = plugin.getServantGUI().getOpenInventoryServant(player.getUniqueId());
        
        if (servant == null) {
            return;
        }

        event.setCancelled(true);
        
        // 处理点击事件
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) {
            return;
        }

        // 检查是否点击了头颅位置(slot 4)
        if (slot == 4) {
            plugin.getServantGUI().showServantStatsMap(player, servant);
            return;
        }

        switch (slot) {
            case 45: // 跟随
                servant.setFollowing(!servant.isFollowing());
                player.sendMessage(servant.isFollowing() 
                    ? MessageManager.get("gui.behavior.follow.enabled") 
                    : MessageManager.get("gui.behavior.follow.disabled"));
                break;
            case 46: // 攻击
                servant.setAttacking(!servant.isAttacking());
                player.sendMessage(servant.isAttacking() 
                    ? MessageManager.get("gui.behavior.attack.enabled") 
                    : MessageManager.get("gui.behavior.attack.disabled"));
                break;
            case 47: // 防御
                servant.setDefending(!servant.isDefending());
                player.sendMessage(servant.isDefending() 
                    ? MessageManager.get("gui.behavior.defend.enabled") 
                    : MessageManager.get("gui.behavior.defend.disabled"));
                break;
            case 48: // 巡逻
                servant.setPatrolPoint(player.getLocation());
                player.sendMessage(MessageManager.get("gui.behavior.patrol.set"));
                break;
            case 49: // 传送
                player.teleport(servant.getLocation());
                player.sendMessage(MessageManager.get("gui.behavior.teleport.success"));
                break;
            case 50: // 技能
                // TODO: 打开技能管理界面
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        plugin.getServantGUI().removeOpenInventory(player.getUniqueId());
    }
} 