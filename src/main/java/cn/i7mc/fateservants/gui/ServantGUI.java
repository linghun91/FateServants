package cn.i7mc.fateservants.gui;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.attributes.AttributeInfo;
import cn.i7mc.fateservants.attributes.AttributeManager;
import cn.i7mc.fateservants.attributes.AttributeModifier;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.ai.ServantBehavior;
import cn.i7mc.fateservants.skills.Skill;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.MessageManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class ServantGUI implements Listener {
    private final Plugin plugin;
    private FileConfiguration guiConfig;
    private static final String INVENTORY_TITLE = "§6英灵管理";
    private static final int INVENTORY_SIZE = 27;
    private final Map<UUID, Integer> playerWindowIds = new HashMap<>();
    private final Map<UUID, Servant> openInventories = new HashMap<>();
    private final Map<String, String> attributeDisplayNames = new HashMap<>();
    private final Map<String, String> attributeSymbols = new HashMap<>();

    public ServantGUI(Plugin plugin) {
        this.plugin = plugin;
        loadGuiConfig();
        loadAttributeConfig();
        
        // 注册数据包监听器
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, PacketType.Play.Client.WINDOW_CLICK) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    PacketContainer packet = event.getPacket();
                    
                    // 检查是否是我们的虚拟物品栏
                    Integer windowId = playerWindowIds.get(player.getUniqueId());
                    if (windowId != null && packet.getIntegers().read(0) == windowId) {
                        event.setCancelled(true); // 取消事件防止实际物品交互
                        handleInventoryClick(player, packet.getIntegers().read(2)); // 1.12.2中使用第三个整数作为slot
                    }
                }
            }
        );

        // 添加关闭窗口的监听器
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, PacketType.Play.Client.CLOSE_WINDOW) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    PacketContainer packet = event.getPacket();
                    
                    Integer windowId = playerWindowIds.get(player.getUniqueId());
                    if (windowId != null && packet.getIntegers().read(0) == windowId) {
                        playerWindowIds.remove(player.getUniqueId());
                        openInventories.remove(player.getUniqueId());
                    }
                }
            }
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerWindowIds.remove(player.getUniqueId());
        openInventories.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // 检查是否是我们的GUI
        if (!openInventories.containsKey(player.getUniqueId())) return;
        
        event.setCancelled(true);
        handleInventoryClick(player, event.getSlot());
    }

    private void handleInventoryClick(Player player, int slot) {
        Servant servant = openInventories.get(player.getUniqueId());
        if (servant == null) {
            return;
        }
        
        switch (slot) {
            case 19: // 跟随
                servant.setFollowing(!servant.isFollowing());
                player.sendMessage(servant.isFollowing() 
                    ? MessageManager.get("gui.behavior.follow.enabled") 
                    : MessageManager.get("gui.behavior.follow.disabled"));
                break;
            case 20: // 攻击
                servant.setAttacking(!servant.isAttacking());
                player.sendMessage(servant.isAttacking() 
                    ? MessageManager.get("gui.behavior.attack.enabled") 
                    : MessageManager.get("gui.behavior.attack.disabled"));
                break;
            case 21: // 防御
                servant.setDefending(!servant.isDefending());
                player.sendMessage(servant.isDefending() 
                    ? MessageManager.get("gui.behavior.defend.enabled") 
                    : MessageManager.get("gui.behavior.defend.disabled"));
                break;
            case 23: // 巡逻
                servant.setPatrolPoint(player.getLocation());
                player.sendMessage(MessageManager.get("gui.behavior.patrol.set"));
                break;
            case 24: // 传送
                player.teleport(servant.getLocation());
                player.sendMessage(MessageManager.get("gui.behavior.teleport.success"));
                break;
            case 25: // 技能
                // TODO: 打开技能管理界面
                break;
        }
    }

    public void openMainMenu(Player player) {
        DebugUtils.log("gui.open_menu", player.getName());
        
        Servant servant = ((FateServants) plugin).getServantManager().getServant(player);
        if (servant == null) {
            MessageManager.send(player, "gui.errors.no_servant");
            return;
        }
        
        DebugUtils.log("gui.generate_window_id", player.getName());
        // 生成一个唯一的窗口ID (1-255)
        int windowId = new Random().nextInt(255) + 1;
        
        DebugUtils.log("gui.create_packet", player.getName());
        
        // 创建虚拟箱子
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);
        
        // 填充物品
        DebugUtils.log("gui.prepare_items", player.getName());
        
        // 基本信息 (头颅)
        inventory.setItem(4, createInfoItem(servant));
        
        // 从stats.yml动态读取并显示属性
        ConfigurationSection statsConfig = plugin.getConfig().getConfigurationSection("attributes");
        if (statsConfig != null) {
            int slot = 9;  // 从第二行开始
            for (String attrKey : statsConfig.getKeys(false)) {
                if (servant.getAttributes().getBaseValue(attrKey) > 0) {
                    String displayName = attributeDisplayNames.getOrDefault(attrKey, formatAttributeName(attrKey));
                    String symbol = getAttributeSymbol(attrKey);
                    Material icon = getMaterialForAttribute(attrKey);
                    
                    inventory.setItem(slot, createAttributeItem(
                        attrKey,
                        "§f" + symbol + " " + displayName,
                        icon,
                        servant
                    ));
                    
                    if (++slot > 17) break; // 最多显示9个属性
                }
            }
        }
        
        // 控制按钮
        inventory.setItem(19, createControlItem("§a跟随", Material.LEASH, servant, "§7点击切换跟随状态"));
        inventory.setItem(20, createControlItem("§c攻击", Material.DIAMOND_SWORD, servant, "§7点击切换攻击状态"));
        inventory.setItem(21, createControlItem("§e防御", Material.SHIELD, servant, "§7点击切换防御状态"));
        inventory.setItem(23, createControlItem("§b巡逻", Material.COMPASS, servant, "§7点击设置巡逻点"));
        inventory.setItem(24, createControlItem("§d传送", Material.ENDER_PEARL, servant, "§7点击传送到英灵位置"));
        inventory.setItem(25, createControlItem("§6技能", Material.BLAZE_POWDER, servant, "§7点击管理技能"));
        
        DebugUtils.log("gui.open_inventory", player.getName());
        player.openInventory(inventory);
        
        playerWindowIds.put(player.getUniqueId(), windowId);
        openInventories.put(player.getUniqueId(), servant);
        
        DebugUtils.log("gui.open_complete", player.getName());
    }

    public void showServantStatsMap(Player player, Servant servant) {
        // 创建一个新的物品栏
        int size = guiConfig.getInt("inventory.size", 27);
        String title = guiConfig.getString("titles.stats", "§6英灵属性");
        Inventory inventory = Bukkit.createInventory(null, size, title);

        // 基础信息
        inventory.setItem(guiConfig.getInt("inventory.slots.info", 4), createInfoItem(servant));
        
        // 核心属性信息 (独立管理的系统)
        inventory.setItem(guiConfig.getInt("inventory.slots.health", 10), 
            createCoreAttributeItem("health", "§a生命值", Material.GOLDEN_APPLE, 
                servant.getCurrentHealth(), servant.getMaxHealth()));
                
        inventory.setItem(guiConfig.getInt("inventory.slots.mana", 11), 
            createCoreAttributeItem("mana", "§b魔法值", Material.POTION, 
                servant.getCurrentMana(), servant.getMaxMana()));
                
        inventory.setItem(guiConfig.getInt("inventory.slots.speed", 12), 
            createCoreAttributeItem("speed", "§e移动速度", Material.FEATHER, 
                servant.getMovementSpeed(), -1));
                
        inventory.setItem(guiConfig.getInt("inventory.slots.exp", 13), 
            createCoreAttributeItem("exp", "§d经验值", Material.EXP_BOTTLE, 
                servant.getExperience(), servant.getRequiredExperience()));
        
        // 行为控制按钮
        inventory.setItem(21, createBehaviorItem(servant, ServantBehavior.FOLLOW));
        inventory.setItem(22, createBehaviorItem(servant, ServantBehavior.COMBAT));
        inventory.setItem(23, createBehaviorItem(servant, ServantBehavior.DEFEND));
        
        // 其他动态属性信息
        Map<String, Double> attributes = servant.getAttributes().getAllAttributes();
        int slot = guiConfig.getInt("inventory.slots.attributes-start", 14);
        int endSlot = guiConfig.getInt("inventory.slots.attributes-end", 16);
        
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            if (slot > endSlot) break; // 防止超出物品栏范围
            
            String attributeName = entry.getKey();
            double value = entry.getValue();
            
            // 跳过核心属性，因为已经单独显示了
            if (attributeName.equalsIgnoreCase("health") || 
                attributeName.equalsIgnoreCase("mana") || 
                attributeName.equalsIgnoreCase("speed") || 
                attributeName.equalsIgnoreCase("exp")) {
                continue;
            }
            
            String displayName = attributeDisplayNames.getOrDefault(attributeName, formatAttributeName(attributeName));
            Material material = getMaterialForAttribute(attributeName);
            inventory.setItem(slot++, createAttributeItem(attributeName, displayName, material, servant));
        }

        // 打开物品栏
        player.openInventory(inventory);
        
        // 记录打开的物品栏
        openInventories.put(player.getUniqueId(), servant);
    }
    
    private ItemStack createCoreAttributeItem(String attribute, String displayName, Material material, double currentValue, double maxValue) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        double percent = maxValue > 0 ? (currentValue / maxValue) * 100 : 0;
        meta.setLore(Arrays.asList(
            "§7当前值: §f" + String.format("%.1f", currentValue),
            "§7最大值: §f" + String.format("%.1f", maxValue),
            "§7百分比: §f" + String.format("%.1f%%", percent)
        ));
        item.setItemMeta(meta);
        return item;
    }
    
    private String formatAttributeName(String attributeName) {
        // 将下划线替换为空格，并将每个单词的首字母大写
        String[] words = attributeName.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }

    private String getAttributeDisplayName(String attributeName) {
        // 使用AttributeManager获取属性显示名称
        AttributeInfo info = ((FateServants)plugin).getAttributeManager().getAttributeInfo(attributeName);
        if (info != null) {
            return info.getDisplayName();
        }
        
        // 如果找不到，则格式化名称
        return formatAttributeName(attributeName);
    }

    private String getAttributeSymbol(String attributeName) {
        // 使用AttributeManager获取属性符号
        AttributeInfo info = ((FateServants)plugin).getAttributeManager().getAttributeInfo(attributeName);
        if (info != null) {
            return info.getSymbol();
        }
        
        // 如果找不到，则返回默认符号
        return "✦";
    }

    private Material getMaterialForAttribute(String attributeName) {
        // 使用AttributeManager获取属性的材质
        AttributeInfo info = ((FateServants)plugin).getAttributeManager().getAttributeInfo(attributeName);
        if (info != null) {
            try {
                return Material.valueOf(info.getMaterial());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的属性材质：" + info.getMaterial() + "，使用默认材质");
            }
        }
        
        // 如果找不到或无效，则返回默认材质
        return Material.PAPER;
    }

    private ItemStack createAttributeItem(String attribute, String displayName, Material material, Servant servant) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        double currentValue = servant.getAttributes().getValue(attribute);
        
        // 添加当前值
        lore.add("§7当前值: §f" + String.format("%.1f", currentValue));
        
        // 添加属性基础值和加成值
        double baseValue = servant.getAttributes().getBaseValue(attribute);
        double bonusValue = currentValue - baseValue;
        if (bonusValue != 0) {
            lore.add("§7基础值: §f" + String.format("%.1f", baseValue));
            lore.add("§7加成值: " + (bonusValue >= 0 ? "§a+" : "§c") + String.format("%.1f", bonusValue));
        }
        
        // 添加属性成长资质信息
        double growth = servant.getAttributeGrowths().getOrDefault(attribute, 0.0);
        String color = growth >= 0.8 ? "§a" : growth >= 0.5 ? "§e" : "§c";
        lore.add("");
        lore.add("§7成长资质: " + color + String.format("%.1f%%", growth * 100));
        
        // 添加属性修饰符信息
        Set<AttributeModifier> modifiers = servant.getAttributes().getModifiers(attribute);
        if (!modifiers.isEmpty()) {
            lore.add("");
            lore.add("§7属性加成:");
            for (AttributeModifier modifier : modifiers) {
                String prefix = modifier.getAmount() >= 0 ? "§a+" : "§c";
                lore.add("  " + prefix + String.format("%.1f", modifier.getAmount()));
            }
        }
        
        lore.add("");
        lore.add("§e点击查看详细信息");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBehaviorItem(Servant servant, ServantBehavior behavior) {
        Material material;
        switch (behavior) {
            case FOLLOW:
                material = Material.LEASH;
                break;
            case COMBAT:
                material = Material.IRON_SWORD;
                break;
            case DEFEND:
                material = Material.SHIELD;
                break;
            default:
                material = Material.BARRIER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // 设置显示名称
        meta.setDisplayName("§6" + behavior.getDisplayName());
        
        // 设置描述
        List<String> lore = new ArrayList<>();
        lore.add("§7" + behavior.getDescription());
        
        // 如果是当前行为，添加标记
        if (servant.getAIController().getCurrentBehavior() == behavior) {
            lore.add("");
            lore.add("§a✔ 当前模式");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createControlItem(String name, Material material, Servant servant, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>(Arrays.asList(lore));
        
        // 如果是技能按钮，添加技能列表
        if (name.equals("§6技能")) {
            loreList.add("");
            loreList.add("§e已学会的技能:");
            for (Skill skill : servant.getSkills()) {
                String skillType = skill.getCastType().equals("active") ? "§b[主动]" : "§a[被动]";
                loreList.add(String.format("§7- %s %s", skillType, skill.getName()));
                loreList.add(String.format("  §8魔法消耗: %.1f  冷却: %.1f秒", 
                    skill.getManaCost(), 
                    skill.getCooldown() / 20.0));  // 转换tick到秒
            }
        }
        
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(Servant servant) {
        ItemStack skull = createPlayerSkull(servant.getServantClass().getSkinName());
        ItemMeta meta = skull.getItemMeta();
        
        // 设置显示名称
        meta.setDisplayName(servant.getServantClass().getDisplayName());
        
        // 设置Lore
        List<String> lore = new ArrayList<>();
        
        // 获取等级格式并添加等级信息
        String levelFormat = guiConfig.getString("servant-info.format.level");
        if (levelFormat != null) {
            lore.add(String.format(levelFormat, (int)servant.getLevel()));
        } else {
            lore.add("§7等级: §f" + (int)servant.getLevel());
        }
        
        // 计算下一级所需经验
        int requiredExp;
        double expPercent = 0.0;
        
        try {
            // 如果是1级，使用默认值
            if (servant.getLevel() == 1) {
                requiredExp = plugin.getConfig().getInt("level_system.default_required_exp", 100);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info(String.format(
                        "[GUI Debug] 使用1级默认经验值: %d",
                        requiredExp
                    ));
                }
            } else {
                // 2级及以上使用公式计算
                String levelUpFormula = plugin.getConfig().getString("level_system.level_up_formula", "100*({level}^2)");
                String formula = levelUpFormula.replace("{level}", String.valueOf(servant.getLevel()));
                
                javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
                javax.script.ScriptEngine engine = manager.getEngineByName("js");
                
                // 替换变量并计算
                requiredExp = (int)Math.round(Double.parseDouble(engine.eval(formula).toString()));
                
                // 输出调试信息到后台
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info(String.format(
                        "[GUI Debug] 经验计算:\n" +
                        "1. 原始公式: %s\n" +
                        "2. 当前等级: %d\n" +
                        "3. 替换变量后的公式: %s\n" +
                        "4. 计算结果: %d",
                        levelUpFormula,
                        servant.getLevel(),
                        formula,
                        requiredExp
                    ));
                }
            }
            
            // 计算经验百分比
            expPercent = ((double)servant.getExperience() / requiredExp) * 100.0;
            
            // 输出最终结果
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                    "[GUI Debug] 最终结果:\n" +
                    "当前等级: %d\n" +
                    "当前经验: %d\n" +
                    "所需经验: %d\n" +
                    "经验百分比: %.2f%%",
                    servant.getLevel(),
                    servant.getExperience(),
                    requiredExp,
                    expPercent
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("计算升级经验时发生错误: " + e.getMessage());
            e.printStackTrace();
            requiredExp = 100; // 出错时使用默认值
        }
        
        // 添加经验值显示
        String expFormat = guiConfig.getString("servant-info.format.experience", "§7经验值: §e%d/%d");
        if (expFormat != null) {
            lore.add(String.format(expFormat, servant.getExperience(), requiredExp));
        } else {
            lore.add("§7经验值: §e" + servant.getExperience() + "/" + requiredExp);
        }
        
        // 添加品质和主人信息
        String qualityFormat = guiConfig.getString("servant-info.format.quality");
        if (qualityFormat != null) {
            lore.add(qualityFormat.replace("%s", servant.getQuality().getDisplayName()));
        } else {
            lore.add("§7品质: " + servant.getQuality().getDisplayName());
        }
        
        String ownerFormat = guiConfig.getString("servant-info.format.owner");
        if (ownerFormat != null) {
            lore.add(ownerFormat.replace("%s", servant.getOwner().getName()));
        } else {
            lore.add("§7主人: " + servant.getOwner().getName());
        }
        
        lore.add("");
        
        // 添加基础属性
        String baseAttrTitle = guiConfig.getString("servant-info.sections.base-attributes.title");
        if (baseAttrTitle != null) {
            lore.add(baseAttrTitle);
        } else {
            lore.add("§6基础属性:");
        }
        
        // 获取所有属性
        AttributeManager attributeManager = ((FateServants)plugin).getAttributeManager();
        
        // 核心属性是固定的，确保始终显示
        // 添加生命值显示
        AttributeInfo healthInfo = attributeManager.getAttributeInfo("health");
        if (healthInfo != null) {
            double healthPercent = (servant.getCurrentHealth() / servant.getMaxHealth()) * 100.0;
            String format = healthInfo.getFormat();
            
            // 替换变量
            format = format.replace("{display_name}", healthInfo.getDisplayName())
                          .replace("{value}", String.format("%.1f/%.1f", servant.getCurrentHealth(), servant.getMaxHealth()))
                          .replace("{symbol}", healthInfo.getSymbol())
                          .replace("{percent}", String.format("%.1f%%", healthPercent));
            
            lore.add(format);
        } else {
            lore.add("§7生命值: §a" + String.format("%.1f/%.1f (%.1f%%)", 
                servant.getCurrentHealth(), servant.getMaxHealth(), 
                (servant.getCurrentHealth() / servant.getMaxHealth() * 100)));
        }
        
        // 添加魔法值显示
        AttributeInfo manaInfo = attributeManager.getAttributeInfo("mana");
        if (manaInfo != null) {
            double manaPercent = (servant.getCurrentMana() / servant.getMaxMana()) * 100.0;
            String format = manaInfo.getFormat();
            
            // 替换变量
            format = format.replace("{display_name}", manaInfo.getDisplayName())
                          .replace("{value}", String.format("%.1f/%.1f", servant.getCurrentMana(), servant.getMaxMana()))
                          .replace("{symbol}", manaInfo.getSymbol())
                          .replace("{percent}", String.format("%.1f%%", manaPercent));
            
            lore.add(format);
        } else {
            lore.add("§7魔法值: §b" + String.format("%.1f/%.1f (%.1f%%)", 
                servant.getCurrentMana(), servant.getMaxMana(), 
                (servant.getCurrentMana() / servant.getMaxMana() * 100)));
        }
        
        // 添加经验值显示
        AttributeInfo expInfo = attributeManager.getAttributeInfo("exp");
        if (expInfo != null) {
            String format = expInfo.getFormat();
            
            // 替换变量
            format = format.replace("{display_name}", expInfo.getDisplayName())
                         .replace("{value}", String.format("%d/%d", servant.getExperience(), requiredExp))
                         .replace("{symbol}", expInfo.getSymbol())
                         .replace("{percent}", String.format("%.1f%%", expPercent));
            
            lore.add(format);
        }
        
        // 添加移动速度显示
        AttributeInfo speedInfo = attributeManager.getAttributeInfo("speed");
        if (speedInfo != null) {
            String format = speedInfo.getFormat();
            
            // 替换变量
            format = format.replace("{display_name}", speedInfo.getDisplayName())
                         .replace("{value}", String.format("%.2f", servant.getMovementSpeed()))
                         .replace("{symbol}", speedInfo.getSymbol());
            
            lore.add(format);
        } else {
            lore.add("§7移动速度: §f" + String.format("%.2f", servant.getMovementSpeed()));
        }
        
        // 添加攻击速度显示
        AttributeInfo attackSpeedInfo = attributeManager.getAttributeInfo("attack_speed");
        if (attackSpeedInfo != null) {
            double attackSpeedInSeconds = servant.getServantClass().getAttackSpeed() / 20.0; // 20 tick = 1秒
            String format = attackSpeedInfo.getFormat();
            
            // 替换变量
            format = format.replace("{display_name}", attackSpeedInfo.getDisplayName())
                         .replace("{value}", String.format("%.2f秒/次", attackSpeedInSeconds))
                         .replace("{symbol}", attackSpeedInfo.getSymbol());
            
            lore.add(format);
        } else {
            double attackSpeedInSeconds = servant.getServantClass().getAttackSpeed() / 20.0;
            lore.add("§7攻击速度: §f" + String.format("%.2f秒/次", attackSpeedInSeconds));
        }
        
        // 添加属性成长资质
        lore.add("");
        String growthTitle = guiConfig.getString("servant-info.sections.growth-attributes.title");
        if (growthTitle != null) {
            lore.add(growthTitle);
        } else {
            lore.add("§6成长资质:");
        }
        
        Map<String, Double> growths = servant.getAttributeGrowths();
        for (Map.Entry<String, Double> entry : growths.entrySet()) {
            String attributeName = entry.getKey();
            double value = entry.getValue();
            
            // 获取属性信息
            AttributeInfo info = attributeManager.getAttributeInfo(attributeName);
            if (info == null) continue;
            
            // 获取颜色配置
            String highColor = guiConfig.getString("servant-info.sections.growth-attributes.colors.high", "§a");
            String mediumColor = guiConfig.getString("servant-info.sections.growth-attributes.colors.medium", "§e");
            String lowColor = guiConfig.getString("servant-info.sections.growth-attributes.colors.low", "§c");
            
            String color;
            if (value >= 0.8) {
                color = highColor;
            } else if (value >= 0.5) {
                color = mediumColor;
            } else {
                color = lowColor;
            }
            
            String growthFormat = guiConfig.getString("servant-info.sections.growth-attributes.format");
            if (growthFormat != null) {
                lore.add(String.format(growthFormat, info.getDisplayName(), color, value * 100));
            } else {
                lore.add(String.format("§7%s: %s%.1f%%", info.getDisplayName(), color, value * 100));
            }
        }
        
        // 添加当前属性
        lore.add("");
        String currentAttrTitle = guiConfig.getString("servant-info.sections.current-attributes.title");
        if (currentAttrTitle != null) {
            lore.add(currentAttrTitle);
        } else {
            lore.add("§6当前属性:");
        }
        
        Map<String, Double> attributes = servant.getAttributes().getAllAttributes();
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            double value = entry.getValue();
            
            // 获取属性信息
            AttributeInfo info = attributeManager.getAttributeInfo(attributeName);
            if (info == null) continue;
            
            // 跳过核心属性，因为已经在基础属性部分显示了
            if (attributeName.equalsIgnoreCase("health") || 
                attributeName.equalsIgnoreCase("mana") || 
                attributeName.equalsIgnoreCase("speed") || 
                attributeName.equalsIgnoreCase("exp")) {
                continue;
            }
            
            String currentAttrFormat = guiConfig.getString("servant-info.sections.current-attributes.format");
            if (currentAttrFormat != null) {
                lore.add(String.format(currentAttrFormat, info.getDisplayName(), value));
            } else {
                lore.add(String.format("§7%s: §f%.1f", info.getDisplayName(), value));
            }
        }
        
        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack createPlayerSkull(String skinName) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (skinName != null && !skinName.isEmpty()) {
            try {
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                skullMeta.setOwner(skinName);
                skull.setItemMeta(skullMeta);
            } catch (Exception e) {
                plugin.getLogger().warning("设置头颅皮肤时发生错误: " + e.getMessage());
            }
        }
        return skull;
    }

    private void loadGuiConfig() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    private void loadAttributeConfig() {
        org.bukkit.configuration.file.FileConfiguration statsConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "stats.yml"));
        ConfigurationSection attributesSection = statsConfig.getConfigurationSection("attributes");
        if (attributesSection != null) {
            for (String key : attributesSection.getKeys(false)) {
                ConfigurationSection attrSection = attributesSection.getConfigurationSection(key);
                if (attrSection != null) {
                    // 加载显示名称
                    String displayName = attrSection.getString("display_name");
                    if (displayName != null) {
                        attributeDisplayNames.put(key, displayName);
                    }
                    
                    // 加载符号
                    String symbol = attrSection.getString("symbol", "✦");
                    attributeSymbols.put(key, symbol);
                }
            }
        }
    }

    public Servant getOpenInventoryServant(UUID playerUuid) {
        return openInventories.get(playerUuid);
    }

    public void removeOpenInventory(UUID playerUuid) {
        openInventories.remove(playerUuid);
        playerWindowIds.remove(playerUuid);
    }
}