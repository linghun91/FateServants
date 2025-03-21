package cn.i7mc.fateservants.commands;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantClass;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.MessageManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.data.AttributeData;
import org.serverct.ersha.attribute.data.AttributeSource;

import java.util.List;
import java.util.Map;

public class FSCommand implements CommandExecutor {
    private final FateServants plugin;

    public FSCommand(FateServants plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageManager.get("commands.player_only"));
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length == 0) {
            // 没有参数时，显示所有命令帮助
            showCommandHelp(player);
            return true;
        }

        DebugUtils.log("command.execute", args[0], player.getName());
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "summon":
                if (args.length < 2) {
                    MessageManager.send(player, "commands.summon.usage");
                    return true;
                }
                handleSummon(player, args[1]);
                break;
            case "unsummon":
                handleUnsummon(player);
                break;
            case "list":
                handleList(player);
                break;
            case "reload":
                handleReload(player);
                break;
            case "info":
                handleInfo(player);
                break;
            case "manage":
                handleManage(sender);
                break;
            case "apstats":
                handleAPStats(player);
                break;
            case "help":
                showCommandHelp(player);
                break;
            default:
                MessageManager.send(player, "commands.unknown", subCommand);
                break;
        }
        
        return true;
    }

    private void handleSummon(Player player, String className) {
        DebugUtils.log("command.summon", className, player.getName());
        
        if (!player.hasPermission("fateservants.summon")) {
            MessageManager.send(player, "commands.no_permission");
            return;
        }

        // 检查是否已经有一个英灵了
        if (plugin.getServantManager().hasServant(player)) {
            MessageManager.send(player, "commands.summon.already_summoned");
            return;
        }

        // 获取职阶
        ServantClass servantClass = plugin.getServantClassManager().getClass(className);
        if (servantClass == null) {
            MessageManager.send(player, "commands.summon.invalid_class", className);
            return;
        }

        // 检查权限
        if (!player.hasPermission("fateservants.class." + servantClass.getId().toLowerCase())) {
            MessageManager.send(player, "commands.summon.no_class_permission", servantClass.getDisplayName());
            return;
        }

        // 召唤英灵
        Servant servant = plugin.getServantManager().createServant(player, servantClass);
        if (servant != null) {
            MessageManager.send(player, "commands.summon.success", servantClass.getDisplayName());
        } else {
            MessageManager.send(player, "commands.summon.failed");
        }
    }

    private void handleUnsummon(Player player) {
        DebugUtils.log("command.unsummon", player.getName());
        
        if (!player.hasPermission("fateservants.unsummon")) {
            MessageManager.send(player, "commands.no_permission");
            return;
        }

        // 检查是否有英灵
        if (!plugin.getServantManager().hasServant(player)) {
            MessageManager.send(player, "commands.unsummon.no_servant");
            return;
        }

        // 解除英灵
        if (plugin.getServantManager().removeServant(player)) {
            MessageManager.send(player, "commands.unsummon.success");
        }
    }

    private void handleList(Player player) {
        DebugUtils.log("command.list", player.getName());
        
        if (!player.hasPermission("fateservants.list")) {
            MessageManager.send(player, "commands.no_permission");
            return;
        }

        MessageManager.send(player, "commands.list.header");
        for (ServantClass servantClass : plugin.getServantClassManager().getAllClasses()) {
            boolean hasPermission = player.hasPermission("fateservants.class." + servantClass.getId().toLowerCase());
            String permissionText = hasPermission ? MessageManager.get("commands.list.has_permission") : MessageManager.get("commands.list.no_permission");
            
            MessageManager.send(player, "commands.list.entry", 
                servantClass.getId(), 
                servantClass.getDisplayName(),
                permissionText
            );
        }
    }

    private void handleReload(Player player) {
        DebugUtils.log("command.reload", player.getName());
        
        if (!player.hasPermission("fateservants.reload")) {
            MessageManager.send(player, "commands.no_permission");
            return;
        }

        // 重载插件
        plugin.reloadConfig();
        plugin.reloadMessageConfig();
        MessageManager.reload();
        
        // 更新所有在线英灵的显示
        plugin.getServantManager().getAllServants().forEach(servant -> {
            servant.updateHolograms();
        });
        
        MessageManager.send(player, "commands.reload.success");
    }

    private void handleInfo(Player player) {
        DebugUtils.log("command.info", player.getName());
        
        if (!player.hasPermission("fateservants.info")) {
            MessageManager.send(player, "commands.no_permission");
            return;
        }

        Servant servant = plugin.getServantManager().getServant(player);
        if (servant == null) {
            MessageManager.send(player, "commands.info.no_servant");
            return;
        }
        
        // 显示基本信息
        MessageManager.send(player, "commands.info.header");
        MessageManager.send(player, "commands.info.class", servant.getServantClass().getDisplayName());
        MessageManager.send(player, "commands.info.health", 
            String.format("%.1f", servant.getCurrentHealth()), 
            String.format("%.1f", servant.getAttributes().getValue("health"))
        );
        
        // 显示所有属性
        MessageManager.send(player, "commands.info.attributes_header");
        for (String attribute : servant.getServantClass().getAttributes().keySet()) {
            List<String> description = servant.getAttributes().getAttributeDescription(attribute);
            description.forEach(player::sendMessage);
        }
    }

    private void handleManage(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行!");
            return;
        }

        Player player = (Player) sender;
        player.sendMessage("§7[Debug] 正在检查权限...");
        
        if (!player.hasPermission("fateservants.manage")) {
            player.sendMessage("§c你没有权限执行此命令!");
            return;
        }
        
        player.sendMessage("§7[Debug] 正在获取英灵...");
        Servant servant = plugin.getServantManager().getServant(player);
        if (servant == null) {
            player.sendMessage("§c你没有召唤的英灵！");
            return;
        }
        
        player.sendMessage("§7[Debug] 正在打开GUI...");
        plugin.getServantGUI().openMainMenu(player);
    }

    private void handleAPStats(Player player) {
        if (!player.hasPermission("fateservants.command.apstats")) {
            MessageManager.send(player, "attributePlus.apstats.no_permission");
            return;
        }

        Servant servant = plugin.getServantManager().getServant(player);
        if (servant == null) {
            MessageManager.send(player, "attributePlus.apstats.no_servant");
            return;
        }

        player.sendMessage(MessageManager.get("attributePlus.apstats.header"));
        Map<String, Double> attributes = servant.getAttributes().getAll();
        
        // 获取配置中的百分比属性列表
        List<String> percentAttributes = plugin.getMessageConfig().getStringList("attributePlus.apstats.percent_attributes");
        
        // 遍历所有属性，动态显示
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            String attrKey = entry.getKey();
            double value = entry.getValue();
            
            // 获取属性显示名称，如果配置中没有则使用属性键
            String displayName = plugin.getMessageConfig().getString(
                "attributePlus.apstats.display_names." + attrKey, 
                attrKey
            );
            
            // 确定显示格式和单位
            String format;
            String unit;
            
            if (percentAttributes.contains(attrKey)) {
                // 百分比属性
                format = plugin.getMessageConfig().getString(
                    "attributePlus.apstats.format.percent", 
                    "§e{display_name}: §f{value}%"
                );
                unit = plugin.getMessageConfig().getString(
                    "attributePlus.apstats.units.percent", 
                    "%"
                );
            } else {
                // 普通属性
                format = plugin.getMessageConfig().getString(
                    "attributePlus.apstats.format.default", 
                    "§e{display_name}: §f{value}{unit}"
                );
                unit = plugin.getMessageConfig().getString(
                    "attributePlus.apstats.units.default", 
                    ""
                );
            }
            
            // 格式化值
            String formattedValue = String.format("%.2f", value);
            
            // 替换变量
            String message = format
                .replace("{display_name}", displayName)
                .replace("{value}", formattedValue)
                .replace("{unit}", unit);
            
            player.sendMessage(message);
        }
        
        player.sendMessage(MessageManager.get("attributePlus.apstats.footer"));
    }

    /**
     * 显示命令帮助信息
     * @param player 玩家
     */
    private void showCommandHelp(Player player) {
        player.sendMessage(MessageManager.get("commands.help.header"));
        
        // 基础命令
        if (player.hasPermission("fateservants.help")) {
            player.sendMessage(MessageManager.get("commands.help.help"));
        }
        
        if (player.hasPermission("fateservants.summon")) {
            player.sendMessage(MessageManager.get("commands.help.summon"));
        }
        
        if (player.hasPermission("fateservants.unsummon")) {
            player.sendMessage(MessageManager.get("commands.help.unsummon"));
        }
        
        if (player.hasPermission("fateservants.list")) {
            player.sendMessage(MessageManager.get("commands.help.list"));
        }
        
        if (player.hasPermission("fateservants.info")) {
            player.sendMessage(MessageManager.get("commands.help.info"));
        }
        
        if (player.hasPermission("fateservants.manage")) {
            player.sendMessage(MessageManager.get("commands.help.manage"));
        }
        
        // 管理命令
        if (player.hasPermission("fateservants.reload")) {
            player.sendMessage(MessageManager.get("commands.help.reload"));
        }
        
        if (player.hasPermission("fateservants.command.apstats")) {
            player.sendMessage(MessageManager.get("commands.help.apstats"));
        }
    }
} 