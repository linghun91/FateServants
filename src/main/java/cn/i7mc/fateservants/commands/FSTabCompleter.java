package cn.i7mc.fateservants.commands;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.ServantClass;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FSTabCompleter implements TabCompleter {
    private final FateServants plugin;
    private final List<String> baseCommands = Arrays.asList("summon", "unsummon", "list", "info", "reload", "manage");

    public FSTabCompleter(FateServants plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            // 根据权限过滤命令
            completions = baseCommands.stream()
                .filter(cmd -> {
                    if (cmd.equals("reload")) {
                        return player.hasPermission("fateservants.reload");
                    }
                    if (cmd.equals("manage")) {
                        return player.hasPermission("fateservants.manage");
                    }
                    return player.hasPermission("fateservants." + cmd);
                })
                .filter(cmd -> cmd.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            String input = args[1].toLowerCase();
            completions = plugin.getServantClassManager().getAllClasses().stream()
                .map(servantClass -> servantClass.getId())
                .filter(id -> id.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        }

        return completions;
    }
} 