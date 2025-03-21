package cn.i7mc.fateservants.gui.pages;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.gui.components.AbstractGUIPage;
import cn.i7mc.fateservants.gui.components.ButtonComponent;
import cn.i7mc.fateservants.gui.components.LabelComponent;
import cn.i7mc.fateservants.gui.components.PaginationComponent;
import cn.i7mc.fateservants.manager.ServantManager;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 英灵列表页面 - 显示玩家所有英灵
 */
public class ServantListPage extends AbstractGUIPage {
    private final FateServants plugin;
    private final ServantManager servantManager;
    private final int pageNumber;
    private final int totalPages;
    private final int itemsPerPage = 45; // 9x5的空间，底部一排留给导航

    /**
     * 创建英灵列表页面
     * @param plugin 插件实例
     * @param pageNumber 页码
     */
    public ServantListPage(FateServants plugin, int pageNumber) {
        super("servant_list_page_" + pageNumber, "§6英灵列表 - 第" + pageNumber + "页", 54);
        this.plugin = plugin;
        this.servantManager = plugin.getServantManager();
        this.pageNumber = pageNumber;
        
        // 计算总页数
        int totalServants = countTotalServants();
        this.totalPages = (int) Math.ceil((double) totalServants / itemsPerPage);
        
        // 初始化页面组件
        initComponents();
    }
    
    /**
     * 计算总英灵数量
     * @return 总英灵数量
     */
    private int countTotalServants() {
        // 简单计算所有在线玩家的英灵总数，实际应该由ServantManager提供
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            List<Servant> servants = getPlayerServants(player.getUniqueId());
            count += servants.size();
        }
        return count;
    }
    
    /**
     * 获取玩家的英灵列表
     * @param playerUuid 玩家UUID
     * @return 英灵列表
     */
    private List<Servant> getPlayerServants(UUID playerUuid) {
        // 由于ServantManager没有getServants(UUID)方法，我们临时模拟一下返回所有英灵
        List<Servant> result = new ArrayList<>();
        Collection<Servant> allServants = servantManager.getServants();
        
        // 过滤出属于该玩家的英灵
        for (Servant servant : allServants) {
            if (servant.getOwner() != null && servant.getOwner().getUniqueId().equals(playerUuid)) {
                result.add(servant);
            }
        }
        
        return result;
    }
    
    /**
     * 初始化页面组件
     */
    private void initComponents() {
        // 添加页面导航组件
        PaginationComponent pagination = new PaginationComponent(plugin, "pagination", "§e页面导航", Material.COMPASS);
        pagination.setPageInfo(pageNumber, Math.max(1, totalPages));
        
        // 设置上一页和下一页
        if (pageNumber > 1) {
            pagination.setPrevPageId("servant_list_page_" + (pageNumber - 1));
        }
        
        if (pageNumber < totalPages) {
            pagination.setNextPageId("servant_list_page_" + (pageNumber + 1));
        }
        
        // 如果不是第一页，设置主页
        if (pageNumber > 1) {
            pagination.setHomePageId("servant_list_page_1");
        }
        
        // 添加到页面
        addComponent(pagination, 49); // 中间底部位置
        
        // 添加返回主菜单按钮
        ButtonComponent backButton = new ButtonComponent("§c返回主菜单", Material.BARRIER);
        backButton.setClickHandler((player, clickType) -> {
            plugin.getGUIManager().openPage(player, "main_menu");
        });
        
        addComponent(backButton, 45); // 左下角位置
        
        // 添加刷新按钮
        ButtonComponent refreshButton = new ButtonComponent("§a刷新列表", Material.WATCH);
        refreshButton.setClickHandler((player, clickType) -> {
            plugin.getGUIManager().openPage(player, "servant_list_page_" + pageNumber);
        });
        
        addComponent(refreshButton, 53); // 右下角位置
    }
    
    @Override
    public ItemStack[] render(Player player) {
        // 清空所有英灵位置的组件（保留导航栏）
        for (int i = 0; i < 45; i++) {
            removeComponent(i);
        }
        
        // 获取玩家的英灵列表
        List<Servant> playerServants = getPlayerServants(player.getUniqueId());
        
        // 计算当前页面应该显示哪些英灵
        int startIndex = (pageNumber - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, playerServants.size());
        
        DebugUtils.log("gui.servant_list", player.getName(), String.valueOf(startIndex), String.valueOf(endIndex));
        
        // 如果玩家没有英灵
        if (playerServants.isEmpty()) {
            LabelComponent noServantLabel = new LabelComponent("§c你还没有英灵", Material.BARRIER);
            noServantLabel.addLoreLine("§7使用 /fs summon 命令召唤一个英灵");
            addComponent(noServantLabel, 22); // 中央位置
        } else if (startIndex >= playerServants.size()) {
            // 如果当前页面没有英灵（可能是最后一页之后）
            LabelComponent noMoreLabel = new LabelComponent("§c这一页没有更多英灵", Material.BARRIER);
            noMoreLabel.addLoreLine("§7请返回上一页");
            addComponent(noMoreLabel, 22); // 中央位置
        } else {
            // 显示当前页面的英灵
            for (int i = startIndex; i < endIndex; i++) {
                if (i >= playerServants.size()) break;
                
                Servant servant = playerServants.get(i);
                int slot = i - startIndex;
                
                // 创建英灵按钮
                ButtonComponent servantButton = createServantButton(servant, player);
                addComponent(servantButton, slot);
            }
        }
        
        // 调用父类的render方法
        return super.render(player);
    }
    
    /**
     * 创建英灵按钮
     * @param servant 英灵对象
     * @param player 玩家
     * @return 按钮组件
     */
    private ButtonComponent createServantButton(Servant servant, Player player) {
        Material material;
        
        // 根据英灵职阶设置不同的显示材质
        String className = servant.getServantClass().toString().toLowerCase();
        switch (className) {
            case "saber":
                material = Material.DIAMOND_SWORD;
                break;
            case "archer":
                material = Material.BOW;
                break;
            case "lancer":
                material = Material.IRON_SWORD; // 替换不存在的TRIDENT
                break;
            case "rider":
                material = Material.SADDLE;
                break;
            case "caster":
                material = Material.ENCHANTED_BOOK;
                break;
            case "assassin":
                material = Material.IRON_SWORD;
                break;
            case "berserker":
                material = Material.DIAMOND_AXE; // 替换不存在的NETHERITE_AXE
                break;
            default:
                material = Material.PAPER;
                break;
        }
        
        ButtonComponent button = new ButtonComponent("servant_" + servant.getUuid().toString(), "§6" + servant.getServantClass().toString(), material);
        
        // 添加英灵信息
        button.addLoreLine("§7职阶: §f" + servant.getServantClass().toString());
        button.addLoreLine("§7品质: §f" + servant.getQuality().getDisplayName());
        button.addLoreLine("§7等级: §f" + servant.getLevel());
        button.addLoreLine("");
        button.addLoreLine("§7攻击力: §c" + servant.getAttributes().getValue("attack"));
        button.addLoreLine("§7生命值: §a" + servant.getMaxHealth());
        button.addLoreLine("");
        button.addLoreLine("§e左键点击查看详情");
        button.addLoreLine("§e右键点击装备/卸下");
        
        // 设置点击处理
        button.setClickHandler((p, clickType) -> {
            if (clickType == ButtonComponent.ClickType.LEFT) {
                // 左键打开详情页面
                plugin.getGUIManager().openPage(p, "servant_detail_" + servant.getUuid().toString());
            } else if (clickType == ButtonComponent.ClickType.RIGHT) {
                // 右键装备/卸下
                boolean isEquipped = isServantEquipped(p, servant.getUuid().toString());
                
                if (isEquipped) {
                    // 卸下英灵
                    servantManager.removeServant(p);
                    p.sendMessage("§a你已卸下英灵 §6" + servant.getServantClass().toString());
                } else {
                    // 装备英灵 - 由于没有equipServant方法，我们使用现有的createServant
                    servantManager.createServant(p, servant.getServantClass());
                    p.sendMessage("§a你已装备英灵 §6" + servant.getServantClass().toString());
                }
                
                // 刷新页面
                refresh(p);
            }
        });
        
        // 如果是已装备的英灵，添加标记
        if (isServantEquipped(player, servant.getUuid().toString())) {
            button.setGlow(true);
            button.addLoreLine("§a✓ 已装备");
        }
        
        return button;
    }
    
    /**
     * 检查英灵是否已装备
     * @param player 玩家
     * @param servantId 英灵ID
     * @return 是否已装备
     */
    private boolean isServantEquipped(Player player, String servantId) {
        // 使用ServantManager的getServant方法，而不是不存在的getEquippedServant
        Servant equipped = servantManager.getServant(player);
        return equipped != null && equipped.getUuid().toString().equals(servantId);
    }

    @Override
    public void onOpen(Player player) {
        // 在页面打开时更新物品列表
        render(player);
        DebugUtils.log("gui.open_list", player.getName());
    }
} 