package cn.i7mc.fateservants.gui.components;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.gui.GUIManager;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 分页导航组件 - 用于在多页面之间导航
 */
public class PaginationComponent extends AbstractGUIComponent {
    private final FateServants plugin;
    private String nextPageId;
    private String prevPageId;
    private String homePageId;
    private int currentPage;
    private int totalPages;
    
    /**
     * 创建分页导航组件
     * @param plugin 插件实例
     * @param displayName 显示名称
     * @param material 材质
     */
    public PaginationComponent(FateServants plugin, String displayName, Material material) {
        super("pagination", material, displayName);
        this.plugin = plugin;
        this.currentPage = 1;
        this.totalPages = 1;
    }
    
    /**
     * 创建具有自定义ID的分页导航组件
     * @param plugin 插件实例
     * @param id 组件ID
     * @param displayName 显示名称
     * @param material 材质
     */
    public PaginationComponent(FateServants plugin, String id, String displayName, Material material) {
        super(id, "pagination", material, displayName);
        this.plugin = plugin;
        this.currentPage = 1;
        this.totalPages = 1;
    }
    
    /**
     * 设置下一页的ID
     * @param nextPageId 下一页ID
     * @return 分页导航组件（链式调用）
     */
    public PaginationComponent setNextPageId(String nextPageId) {
        this.nextPageId = nextPageId;
        return this;
    }
    
    /**
     * 设置上一页的ID
     * @param prevPageId 上一页ID
     * @return 分页导航组件（链式调用）
     */
    public PaginationComponent setPrevPageId(String prevPageId) {
        this.prevPageId = prevPageId;
        return this;
    }
    
    /**
     * 设置主页的ID
     * @param homePageId 主页ID
     * @return 分页导航组件（链式调用）
     */
    public PaginationComponent setHomePageId(String homePageId) {
        this.homePageId = homePageId;
        return this;
    }
    
    /**
     * 设置当前页码和总页数
     * @param currentPage 当前页码
     * @param totalPages 总页数
     * @return 分页导航组件（链式调用）
     */
    public PaginationComponent setPageInfo(int currentPage, int totalPages) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        return this;
    }
    
    @Override
    public ItemStack render(Player player) {
        // 清空旧的描述，添加新的页码信息
        this.lore.clear();
        this.addLoreLine("§7当前页码: §e" + currentPage + " / " + totalPages);
        
        if (prevPageId != null && currentPage > 1) {
            this.addLoreLine("§a← 上一页");
        }
        
        if (nextPageId != null && currentPage < totalPages) {
            this.addLoreLine("§a下一页 →");
        }
        
        if (homePageId != null && !homePageId.equals(prevPageId) && currentPage != 1) {
            this.addLoreLine("§e回到首页");
        }
        
        return createBaseItem();
    }
    
    @Override
    public boolean onClick(Player player, ClickType clickType, boolean isShiftClick) {
        GUIManager guiManager = plugin.getGUIManager();
        if (guiManager == null) {
            return false;
        }
        
        switch (clickType) {
            case LEFT:
                // 下一页
                if (nextPageId != null && currentPage < totalPages) {
                    DebugUtils.log("gui.pagination_next", player.getName(), String.valueOf(currentPage), String.valueOf(totalPages));
                    guiManager.openPage(player, nextPageId);
                    return true;
                }
                break;
                
            case RIGHT:
                // 上一页
                if (prevPageId != null && currentPage > 1) {
                    DebugUtils.log("gui.pagination_prev", player.getName(), String.valueOf(currentPage), String.valueOf(totalPages));
                    guiManager.openPage(player, prevPageId);
                    return true;
                }
                break;
                
            case MIDDLE:
                // 回到首页
                if (homePageId != null) {
                    DebugUtils.log("gui.pagination_home", player.getName());
                    guiManager.openPage(player, homePageId);
                    return true;
                }
                break;
                
            default:
                break;
        }
        
        return false;
    }
} 