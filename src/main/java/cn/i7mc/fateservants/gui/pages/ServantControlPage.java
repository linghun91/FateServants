package cn.i7mc.fateservants.gui.pages;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.gui.components.AbstractGUIPage;
import cn.i7mc.fateservants.gui.components.ButtonComponent;
import cn.i7mc.fateservants.gui.components.LabelComponent;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.FormatUtils;
import cn.i7mc.fateservants.utils.MaterialAdapter;
import cn.i7mc.fateservants.utils.MessageManager;
import cn.i7mc.fateservants.attributes.AttributeInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 英灵控制页面
 */
public class ServantControlPage extends AbstractGUIPage {
    
    private final FateServants plugin;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ServantControlPage(FateServants plugin) {
        super("control", MessageManager.get("gui.control.title"), 
              plugin.getConfigManager().getInt("gui.yml", "pages.control.size", 54));
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
        
        // 模式按钮 - 跟随模式
        ButtonComponent followButton = new ButtonComponent("follow_mode", 20, (player, button) -> {
            changeMode(player, "FOLLOW");
            return true;
        });
        registerComponent(followButton);
        
        // 模式按钮 - 战斗模式
        ButtonComponent combatButton = new ButtonComponent("combat_mode", 22, (player, button) -> {
            changeMode(player, "COMBAT");
            return true;
        });
        registerComponent(combatButton);
        
        // 模式按钮 - 防御模式
        ButtonComponent defendButton = new ButtonComponent("defend_mode", 24, (player, button) -> {
            changeMode(player, "DEFEND");
            return true;
        });
        registerComponent(defendButton);
        
        // 传送按钮
        ButtonComponent teleportButton = new ButtonComponent("teleport_button", 31, (player, button) -> {
            teleportServant(player);
            return true;
        });
        registerComponent(teleportButton);
        
        // 返回按钮
        ButtonComponent backButton = new ButtonComponent("back_button", 49, (player, button) -> {
            plugin.getGUIManager().openPage(player, "main");
            return true;
        });
        registerComponent(backButton);
    }
    
    /**
     * 更改英灵模式
     * @param player 玩家
     * @param mode 模式
     */
    private void changeMode(Player player, String mode) {
        Servant servant = plugin.getServantManager().getServant(player);
        
        if (servant == null) {
            MessageManager.send(player, "servant.not_summoned");
            return;
        }
        
        servant.setMode(mode);
        
        // 发送模式改变提示
        MessageManager.send(player, "servant.mode_changed." + mode.toLowerCase());
        
        // 刷新页面
        refresh(player);
    }
    
    /**
     * 传送英灵到玩家身边
     * @param player 玩家
     */
    private void teleportServant(Player player) {
        Servant servant = plugin.getServantManager().getServant(player);
        
        if (servant == null) {
            MessageManager.send(player, "servant.not_summoned");
            return;
        }
        
        servant.teleport(player.getLocation());
        MessageManager.send(player, "servant.teleported");
    }
    
    @Override
    public void onOpen(Player player) {
        DebugUtils.log("gui.open_control", player.getName());
        updateItems(player);
    }
    
    @Override
    public void refresh(Player player) {
        DebugUtils.log("gui.refresh_control", player.getName());
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
        
        // 更新模式按钮
        updateModeButtons(player, servant);
        
        // 更新传送按钮
        updateTeleportButton(player);
    }
    
    /**
     * 更新信息显示
     * @param player 玩家
     * @param servant 英灵
     */
    private void updateInfoDisplay(Player player, Servant servant) {
        LabelComponent infoLabel = (LabelComponent) getComponent("info_display");
        
        if (infoLabel == null) return;
        
        String material = plugin.getConfigManager().getString("gui.yml", "pages.control.components.info_display.material", "COMPASS");
        ItemStack item = new ItemStack(MaterialAdapter.getMaterial(material));
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        String name = plugin.getConfigManager().getString("gui.yml", "pages.control.components.info_display.display_name", "&b{servant_name} &7- 控制");
        name = FormatUtils.format(name, "servant_name", servant.getName());
        meta.setDisplayName(FormatUtils.colorize(name));
        
        // 设置lore
        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfigManager().getConfig("gui.yml").getStringList("pages.control.components.info_display.lore");
        
        // 获取核心属性列表
        List<String> coreAttributes = plugin.getConfigManager().getConfig("gui.yml")
                .getStringList("pages.control.core_attributes");
        
        // 如果配置中没有定义，则使用默认的核心属性列表
        if (coreAttributes.isEmpty()) {
            coreAttributes = Arrays.asList("health", "mana", "exp", "speed", "movement_speed");
        }
        
        if (configLore.isEmpty()) {
            // 如果配置中没有定义，使用默认lore
            // 从配置文件获取职阶格式模板
            String classFormat = plugin.getConfigManager().getString("gui.yml", "format.class", "&7职阶: &f{value}");
            lore.add(FormatUtils.colorize(classFormat.replace("{value}", servant.getServantClass().getDisplayName())));
            
            // 从配置文件获取品质格式模板
            String qualityFormat = plugin.getConfigManager().getString("gui.yml", "format.quality", "&7品质: {value}");
            lore.add(FormatUtils.colorize(qualityFormat.replace("{value}", servant.getQuality().getDisplayName())));
            
            // 从配置文件获取等级格式模板
            String levelFormat = plugin.getConfigManager().getString("gui.yml", "format.level", "&7等级: &f{value}");
            lore.add(FormatUtils.colorize(levelFormat.replace("{value}", String.valueOf(servant.getLevel()))));
            
            // 从配置文件获取模式格式模板
            String modeFormat = plugin.getConfigManager().getString("gui.yml", "format.mode", "&7当前模式: &e{value}");
            lore.add(FormatUtils.colorize(modeFormat.replace("{value}", MessageManager.get("servant.mode." + servant.getMode().toLowerCase()))));
            
            lore.add("");
            
            // 从配置文件获取核心属性标题
            String coreAttrTitle = plugin.getConfigManager().getString("gui.yml", "format.core_attributes_title", "&6核心属性:");
            lore.add(FormatUtils.colorize(coreAttrTitle));
            
            // 动态添加所有核心属性
            for (String attrName : coreAttributes) {
                AttributeInfo info = plugin.getAttributeManager().getAttributeInfo(attrName);
                if (info == null) continue;
                
                // 使用FormatUtils的方法格式化属性
                String formattedValue = "";
                if ("health".equals(attrName)) {
                    formattedValue = FormatUtils.formatHealth(servant);
                } else if ("mana".equals(attrName)) {
                    formattedValue = FormatUtils.formatMana(servant);
                } else if ("movement_speed".equals(attrName) || "speed".equals(attrName)) {
                    formattedValue = FormatUtils.formatMovementSpeed(servant.getMovementSpeed());
                } else {
                    // 其他核心属性使用通用格式
                    double value = servant.getAttributes().getValue(attrName);
                    String format = info.getFormat();
                    
                    // 替换变量
                    format = format.replace("{display_name}", info.getDisplayName())
                                 .replace("{value}", String.format("%.1f", value))
                                 .replace("{symbol}", info.getSymbol());
                    
                    formattedValue = ChatColor.translateAlternateColorCodes('&', format);
                }
                
                lore.add(formattedValue);
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
                line = FormatUtils.format(line, "mode", MessageManager.get("servant.mode." + servant.getMode().toLowerCase()));
                
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
     * 更新模式按钮
     * @param player 玩家
     * @param servant 英灵
     */
    private void updateModeButtons(Player player, Servant servant) {
        String currentMode = servant.getMode();
        
        // 获取各个模式按钮
        ButtonComponent followButton = (ButtonComponent) getComponent("follow_mode");
        ButtonComponent combatButton = (ButtonComponent) getComponent("combat_mode");
        ButtonComponent defendButton = (ButtonComponent) getComponent("defend_mode");
        
        // 更新按钮状态
        updateModeButton(player, followButton, "FOLLOW", currentMode);
        updateModeButton(player, combatButton, "COMBAT", currentMode);
        updateModeButton(player, defendButton, "DEFEND", currentMode);
    }
    
    /**
     * 更新单个模式按钮
     * @param player 玩家
     * @param button 按钮组件
     * @param mode 模式
     * @param currentMode 当前模式
     */
    private void updateModeButton(Player player, ButtonComponent button, String mode, String currentMode) {
        if (button == null) return;
        
        String configPath = "pages.control.components." + button.getId();
        String material = plugin.getConfigManager().getString("gui.yml", configPath + ".material", "STONE");
        ItemStack item = new ItemStack(MaterialAdapter.getMaterial(material));
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        String name = plugin.getConfigManager().getString("gui.yml", configPath + ".display_name", "&7" + mode);
        meta.setDisplayName(FormatUtils.colorize(name));
        
        // 设置lore
        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfigManager().getConfig("gui.yml").getStringList(configPath + ".lore");
        
        // 添加当前模式指示器
        for (String line : configLore) {
            lore.add(FormatUtils.colorize(line));
        }
        
        // 如果是当前模式，添加指示器
        if (mode.equals(currentMode)) {
            lore.add("");
            lore.add(FormatUtils.colorize("&a✓ &f当前已选择此模式"));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        button.setItem(player, item);
    }
    
    /**
     * 更新传送按钮
     * @param player 玩家
     */
    private void updateTeleportButton(Player player) {
        ButtonComponent teleportButton = (ButtonComponent) getComponent("teleport_button");
        
        if (teleportButton == null) return;
        
        String configPath = "pages.control.components.teleport_button";
        String material = plugin.getConfigManager().getString("gui.yml", configPath + ".material", "ENDER_PEARL");
        ItemStack item = new ItemStack(MaterialAdapter.getMaterial(material));
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        String name = plugin.getConfigManager().getString("gui.yml", configPath + ".display_name", "&b召回英灵");
        meta.setDisplayName(FormatUtils.colorize(name));
        
        // 设置lore
        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfigManager().getConfig("gui.yml").getStringList(configPath + ".lore");
        
        for (String line : configLore) {
            lore.add(FormatUtils.colorize(line));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        teleportButton.setItem(player, item);
    }
} 