package cn.i7mc.fateservants.gui.pages;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.attributes.AttributeInfo;
import cn.i7mc.fateservants.gui.components.AbstractGUIPage;
import cn.i7mc.fateservants.gui.components.ButtonComponent;
import cn.i7mc.fateservants.gui.components.LabelComponent;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.FormatUtils;
import cn.i7mc.fateservants.utils.MaterialAdapter;
import cn.i7mc.fateservants.utils.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 英灵属性页面
 */
public class ServantStatsPage extends AbstractGUIPage {
    
    private final FateServants plugin;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ServantStatsPage(FateServants plugin) {
        super("stats", MessageManager.get("gui.stats.title"), 
              plugin.getConfigManager().getInt("gui.yml", "pages.stats.size", 54));
        this.plugin = plugin;
        
        // 初始化页面组件
        initComponents();
    }
    
    /**
     * 初始化组件
     */
    private void initComponents() {
        // 信息展示组件
        LabelComponent infoLabel = new LabelComponent("info_display", 4);
        registerComponent(infoLabel);
        
        // 返回按钮 - 确保存在并放在底部中央位置
        ButtonComponent backButton = new ButtonComponent("back_button", 49, (player, button) -> {
            plugin.getGUIManager().openPage(player, "main");
            return true;
        });
        registerComponent(backButton);
        
        // 动态添加属性区域
        // 由于属性是动态的，我们只在这里预留位置，具体物品会在updateItems中设置
    }
    
    @Override
    public void onOpen(Player player) {
        DebugUtils.log("gui.open_stats", player.getName());
        updateItems(player);
    }
    
    @Override
    public void refresh(Player player) {
        DebugUtils.log("gui.refresh_stats", player.getName());
        updateItems(player);
    }
    
    /**
     * 更新所有物品
     * @param player 玩家
     */
    private void updateItems(Player player) {
        Servant servant = plugin.getServantManager().getServant(player);
        
        if (servant == null) {
            player.closeInventory();
            MessageManager.send(player, "servant.not_summoned");
            return;
        }
        
        // 更新信息显示
        updateInfoDisplay(player, servant);
        
        // 更新属性展示区域
        updateAttributesDisplay(player, servant);
        
        // 更新返回按钮
        updateBackButton(player);
    }
    
    /**
     * 更新信息显示
     * @param player 玩家
     * @param servant 英灵
     */
    private void updateInfoDisplay(Player player, Servant servant) {
        LabelComponent infoLabel = (LabelComponent) getComponent("info_display");
        
        if (infoLabel == null) return;
        
        String material = plugin.getConfigManager().getString("gui.yml", "pages.stats.components.info_display.material", "BOOK");
        ItemStack item = new ItemStack(MaterialAdapter.getMaterial(material));
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        String name = plugin.getConfigManager().getString("gui.yml", "pages.stats.components.info_display.display_name", "&b{servant_name} &7- 属性");
        name = FormatUtils.format(name, "servant_name", servant.getName());
        meta.setDisplayName(FormatUtils.colorize(name));
        
        // 设置lore
        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfigManager().getConfig("gui.yml").getStringList("pages.stats.components.info_display.lore");
        
        // 获取核心属性列表
        List<String> coreAttributes = plugin.getConfigManager().getConfig("gui.yml")
                .getStringList("pages.stats.core_attributes");
        
        // 如果配置中没有定义，则使用默认的核心属性列表
        if (coreAttributes.isEmpty()) {
            coreAttributes = Arrays.asList("health", "mana", "exp", "speed", "movement_speed");
        }
        
        if (configLore.isEmpty()) {
            // 如果配置中没有定义，使用默认lore
            lore.add(ChatColor.GRAY + "职阶: " + ChatColor.WHITE + servant.getServantClass().getDisplayName());
            lore.add(ChatColor.GRAY + "品质: " + FormatUtils.colorize(servant.getQuality().getDisplayName()));
            lore.add(ChatColor.GRAY + "等级: " + ChatColor.WHITE + servant.getLevel());
            lore.add("");
            
            // 添加核心属性
            lore.add(ChatColor.GOLD + "核心属性:");
            
            // 动态添加所有核心属性
            for (String attrName : coreAttributes) {
                AttributeInfo info = plugin.getAttributeManager().getAttributeInfo(attrName);
                if (info == null) continue;
                
                // 根据属性类型使用不同的格式化方法
                if ("health".equals(attrName)) {
                    lore.add(FormatUtils.formatHealth(servant));
                } else if ("mana".equals(attrName)) {
                    lore.add(FormatUtils.formatMana(servant));
                } else if ("movement_speed".equals(attrName) || "speed".equals(attrName)) {
                    lore.add(FormatUtils.formatMovementSpeed(servant.getMovementSpeed()));
                } else {
                    // 其他核心属性使用通用格式
                    double value = servant.getAttributes().getValue(attrName);
                    String format = info.getFormat();
                    
                    // 替换变量
                    format = format.replace("{display_name}", info.getDisplayName())
                                 .replace("{value}", String.format("%.1f", value))
                                 .replace("{symbol}", info.getSymbol());
                    
                    lore.add(ChatColor.translateAlternateColorCodes('&', format));
                }
            }
        } else {
            // 使用配置中的lore
            Map<String, Double> attributes = servant.getAttributes().getAllAttributes();
            
            for (String line : configLore) {
                // 替换基本变量
                line = FormatUtils.format(line, "servant_name", servant.getName());
                line = FormatUtils.format(line, "servant_class", servant.getServantClass().getDisplayName());
                line = FormatUtils.format(line, "servant_quality", servant.getQuality().getDisplayName());
                line = FormatUtils.format(line, "servant_level", String.valueOf(servant.getLevel()));
                
                // 替换核心属性格式化信息
                if (line.contains("{health}")) {
                    line = line.replace("{health}", FormatUtils.formatHealth(servant));
                }
                if (line.contains("{mana}")) {
                    line = line.replace("{mana}", FormatUtils.formatMana(servant));
                }
                if (line.contains("{movement_speed}") || line.contains("{speed}")) {
                    line = line.replace("{movement_speed}", FormatUtils.formatMovementSpeed(servant.getMovementSpeed()))
                               .replace("{speed}", FormatUtils.formatMovementSpeed(servant.getMovementSpeed()));
                }
                
                // 替换其他属性
                for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                    String attrName = entry.getKey();
                    if (line.contains("{" + attrName + "}")) {
                        AttributeInfo info = plugin.getAttributeManager().getAttributeInfo(attrName);
                        if (info != null) {
                            String format = info.getFormat();
                            format = format.replace("{display_name}", info.getDisplayName())
                                       .replace("{value}", String.format("%.1f", entry.getValue()))
                                       .replace("{symbol}", info.getSymbol());
                            
                            line = line.replace("{" + attrName + "}", format);
                        }
                    }
                }
                
                lore.add(FormatUtils.colorize(line));
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        infoLabel.setItem(player, item);
    }
    
    /**
     * 更新属性展示区域
     * @param player 玩家
     * @param servant 英灵
     */
    private void updateAttributesDisplay(Player player, Servant servant) {
        // 获取属性容器定义的槽位
        List<Integer> slots = plugin.getConfigManager().getConfig("gui.yml").getIntegerList("pages.stats.components.attributes_container.slots");
        
        if (slots.isEmpty()) {
            // 如果配置中没有定义，使用默认槽位
            slots = new ArrayList<>();
            for (int i = 19; i <= 25; i++) {
                slots.add(i);
            }
            for (int i = 28; i <= 34; i++) {
                slots.add(i);
            }
            for (int i = 37; i <= 43; i++) {
                slots.add(i);
            }
        }
        
        // 获取所有属性
        Map<String, Double> attributes = servant.getAttributes().getAllAttributes();
        
        // 清空之前的属性物品
        for (int slot : slots) {
            getInventory().setItem(slot, null);
        }
        
        // 填充属性物品
        int slotIndex = 0;
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            if (slotIndex >= slots.size()) break;
            
            String attrName = entry.getKey();
            double attrValue = entry.getValue();
            
            // 跳过核心属性，因为它们已经在info_display中显示
            if (isCoreAttribute(attrName)) continue;
            
            // 创建属性物品
            ItemStack attrItem = createAttributeItem(attrName, attrValue, servant);
            getInventory().setItem(slots.get(slotIndex), attrItem);
            slotIndex++;
        }
    }
    
    /**
     * 判断是否为核心属性
     * @param attributeName 属性名称
     * @return 是否为核心属性
     */
    private boolean isCoreAttribute(String attributeName) {
        // 从配置文件获取核心属性列表
        List<String> coreAttributes = plugin.getConfigManager().getConfig("gui.yml")
                .getStringList("pages.stats.core_attributes");
        
        // 如果配置中没有定义，则使用默认的核心属性列表
        if (coreAttributes.isEmpty()) {
            coreAttributes = Arrays.asList("health", "mana", "exp", "speed", "movement_speed");
        }
        
        return coreAttributes.contains(attributeName);
    }
    
    /**
     * 创建属性物品
     * @param attributeName 属性名称
     * @param value 属性值
     * @param servant 英灵
     * @return 物品
     */
    private ItemStack createAttributeItem(String attributeName, double value, Servant servant) {
        // 获取属性信息
        AttributeInfo info = plugin.getAttributeManager().getAttributeInfo(attributeName);
        
        // 如果没有属性信息，使用默认属性
        if (info == null) {
            DebugUtils.log("missing_attribute_info", attributeName);
            // 创建一个基础的物品
            ItemStack defaultItem = new ItemStack(Material.STONE);
            ItemMeta meta = defaultItem.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "未知属性: " + attributeName);
            defaultItem.setItemMeta(meta);
            return defaultItem;
        }
        
        // 获取属性显示配置
        String materialName = info.getMaterial();
        String displayName = info.getDisplayName();
        String symbol = info.getSymbol();
        
        // 获取物品材质
        Material material = MaterialAdapter.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
            DebugUtils.log("invalid_material", materialName, attributeName);
        }
        
        // 创建物品
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // 设置显示名称 - 从gui.yml获取标题格式
        String titleFormat = plugin.getConfigManager().getString("gui.yml", "format.attribute_title", 
                "&b{display_name}{symbol_part}");
        titleFormat = titleFormat.replace("{display_name}", displayName)
                               .replace("{symbol_part}", symbol.isEmpty() ? "" : " " + symbol)
                               .replace("{attribute_id}", attributeName);
        
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', titleFormat));
        
        // 设置lore
        List<String> lore = new ArrayList<>();
        
        // 从gui.yml获取属性显示模板
        List<String> template = plugin.getConfigManager().getConfig("gui.yml")
                .getStringList("format.attribute_lore");
        
        // 如果配置中没有模板，使用默认模板
        if (template.isEmpty()) {
            template = Arrays.asList(
                "&7{display_name}: &f{value}{symbol} {growth_color}({growth}%)",
                "",
                "&7基础值: &f{base_value}",
                "&7加成值: {bonus_color}{bonus_sign}{bonus_value}"
            );
        }
        
        // 获取属性相关数据
        double baseValue = servant.getAttributes().getBaseValue(attributeName);
        double bonus = value - baseValue;
        double growth = servant.getAttributeGrowths().getOrDefault(attributeName, 0.0);
        int growthPercent = (int)(growth * 100);
        
        // 生成颜色代码
        String bonusColor = bonus >= 0 ? "&a" : "&c";
        String bonusSign = bonus >= 0 ? "+" : "";
        String growthColor = getGrowthColor(growth);
        
        // 处理每一行
        for (String line : template) {
            // 替换所有变量
            line = line.replace("{attribute_id}", attributeName)
                     .replace("{display_name}", displayName)
                     .replace("{symbol}", symbol)
                     .replace("{value}", String.format("%.1f", value))
                     .replace("{base_value}", String.format("%.1f", baseValue))
                     .replace("{bonus_value}", String.format("%.1f", Math.abs(bonus)))
                     .replace("{bonus_sign}", bonusSign)
                     .replace("{bonus_color}", bonusColor)
                     .replace("{growth}", String.valueOf(growthPercent))
                     .replace("{growth_color}", growthColor);
            
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 更新返回按钮
     * @param player 玩家
     */
    private void updateBackButton(Player player) {
        ButtonComponent backButton = (ButtonComponent) getComponent("back_button");
        
        if (backButton == null) return;
        
        String configPath = "pages.stats.components.back_button";
        String material = plugin.getConfigManager().getString("gui.yml", configPath + ".material", "ARROW");
        ItemStack item = new ItemStack(MaterialAdapter.getMaterial(material));
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        String name = plugin.getConfigManager().getString("gui.yml", configPath + ".display_name", "&f返回主页");
        meta.setDisplayName(FormatUtils.colorize(name));
        
        // 设置lore
        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfigManager().getConfig("gui.yml").getStringList(configPath + ".lore");
        
        if (configLore.isEmpty()) {
            lore.add(FormatUtils.colorize("&7点击返回主页面"));
        } else {
            for (String line : configLore) {
                lore.add(FormatUtils.colorize(line));
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        backButton.setItem(player, item);
    }
    
    /**
     * 获取成长资质对应的颜色代码
     * @param growth 成长资质值(0.0-1.0)
     * @return 颜色代码
     */
    private String getGrowthColor(double growth) {
        // 从配置获取颜色区间
        double highThreshold = plugin.getConfigManager().getDouble("gui.yml", "growth.thresholds.high", 0.8);
        double mediumThreshold = plugin.getConfigManager().getDouble("gui.yml", "growth.thresholds.medium", 0.5);
        
        // 从配置获取对应颜色
        String highColor = plugin.getConfigManager().getString("gui.yml", "growth.colors.high", "&a");
        String mediumColor = plugin.getConfigManager().getString("gui.yml", "growth.colors.medium", "&e");
        String lowColor = plugin.getConfigManager().getString("gui.yml", "growth.colors.low", "&c");
        
        // 根据阈值返回对应颜色
        if (growth >= highThreshold) {
            return highColor;
        } else if (growth >= mediumThreshold) {
            return mediumColor;
        } else {
            return lowColor;
        }
    }
} 