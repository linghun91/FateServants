package cn.i7mc.fateservants.gui.pages;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.gui.components.AbstractGUIPage;
import cn.i7mc.fateservants.gui.components.ButtonComponent;
import cn.i7mc.fateservants.gui.components.LabelComponent;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * 主菜单页面 - 提供各种功能入口
 */
public class MainMenuPage extends AbstractGUIPage {
    private final FateServants plugin;
    
    /**
     * 创建主菜单页面
     * @param plugin 插件实例
     */
    public MainMenuPage(FateServants plugin) {
        super("main_menu", "§6英灵系统 - 主菜单", 27);
        this.plugin = plugin;
        
        // 初始化组件
        initComponents();
    }
    
    /**
     * 初始化页面组件
     */
    private void initComponents() {
        // 添加标题标签
        LabelComponent titleLabel = new LabelComponent("§6§l英灵系统", Material.BOOK);
        titleLabel.addLoreLine("§7欢迎使用英灵系统");
        titleLabel.addLoreLine("§7请选择你要使用的功能");
        
        addComponent(titleLabel, 4);
        
        // 添加英灵列表按钮
        ButtonComponent servantListButton = new ButtonComponent("§e我的英灵", Material.DIAMOND_SWORD);
        servantListButton.addLoreLine("§7查看你拥有的所有英灵");
        servantListButton.addLoreLine("§7点击前往英灵列表");
        servantListButton.setClickHandler((player, clickType) -> {
            DebugUtils.log("gui.button_click", player.getName(), "servant_list");
            openServantListPage(player, 1);
        });
        
        addComponent(servantListButton, 10);
        
        // 添加召唤英灵按钮
        ButtonComponent summonButton = new ButtonComponent("§a召唤英灵", Material.ENDER_PEARL);
        summonButton.addLoreLine("§7消耗圣晶石召唤新英灵");
        summonButton.addLoreLine("§7点击前往召唤界面");
        summonButton.setClickHandler((player, clickType) -> {
            DebugUtils.log("gui.button_click", player.getName(), "summon");
            plugin.getGUIManager().openPage(player, "summon_page");
        });
        
        addComponent(summonButton, 12);
        
        // 添加英灵强化按钮
        ButtonComponent enhanceButton = new ButtonComponent("§b强化英灵", Material.EXP_BOTTLE);
        enhanceButton.addLoreLine("§7提升英灵的等级和能力");
        enhanceButton.addLoreLine("§7点击前往强化界面");
        enhanceButton.setClickHandler((player, clickType) -> {
            DebugUtils.log("gui.button_click", player.getName(), "enhance");
            plugin.getGUIManager().openPage(player, "enhance_page");
        });
        
        addComponent(enhanceButton, 14);
        
        // 添加英灵资料按钮
        ButtonComponent infoButton = new ButtonComponent("§d英灵图鉴", Material.BOOK);
        infoButton.addLoreLine("§7查看所有英灵的详细资料");
        infoButton.addLoreLine("§7点击前往图鉴界面");
        infoButton.setClickHandler((player, clickType) -> {
            DebugUtils.log("gui.button_click", player.getName(), "info");
            plugin.getGUIManager().openPage(player, "info_page");
        });
        
        addComponent(infoButton, 16);
        
        // 添加帮助按钮
        ButtonComponent helpButton = new ButtonComponent("§f帮助", Material.PAPER);
        helpButton.addLoreLine("§7查看使用说明和帮助");
        helpButton.setClickHandler((player, clickType) -> {
            player.sendMessage("§a§l=== 英灵系统帮助 ===");
            player.sendMessage("§e/fs help §7- 显示帮助");
            player.sendMessage("§e/fs summon §7- 召唤英灵");
            player.sendMessage("§e/fs list §7- 列出所有英灵");
            player.sendMessage("§e/fs info <英灵ID> §7- 查看英灵详情");
            player.sendMessage("§e/fs equip <英灵ID> §7- 装备英灵");
            player.sendMessage("§e/fs unequip §7- 卸下当前英灵");
            player.closeInventory();
        });
        
        addComponent(helpButton, 22);
    }
    
    /**
     * 打开英灵列表页面
     * @param player 玩家
     * @param pageNumber 页码
     */
    private void openServantListPage(Player player, int pageNumber) {
        // 这是一个示例方法，实际应该先创建页面实例，再打开
        plugin.getGUIManager().openPage(player, "servant_list_page_" + pageNumber);
    }

    @Override
    public void onOpen(Player player) {
        DebugUtils.log("gui.open_main_menu", player.getName());
        // 主菜单打开时不需要特殊处理
    }
} 