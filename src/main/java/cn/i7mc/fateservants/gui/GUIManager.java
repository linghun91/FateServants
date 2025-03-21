package cn.i7mc.fateservants.gui;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.gui.components.GUIPage;
import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * GUI管理器 - 负责管理所有GUI页面和配置
 */
public class GUIManager implements Listener {
    private final FateServants plugin;
    private final Map<String, GUIPage> pages = new HashMap<>();
    private final Map<UUID, String> playerPages = new HashMap<>(); // 玩家正在查看的页面
    private FileConfiguration guiConfig;
    
    /**
     * 创建GUI管理器
     * @param plugin 插件实例
     */
    public GUIManager(FateServants plugin) {
        this.plugin = plugin;
        loadConfig();
        
        // 注册事件
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 加载GUI配置
     */
    public void loadConfig() {
        DebugUtils.log("gui.config_load");
        
        File configFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!configFile.exists()) {
            DebugUtils.log("gui.config_missing");
            plugin.saveResource("gui.yml", false);
        }
        
        try {
            guiConfig = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            DebugUtils.log("gui.config_error", e.getMessage());
            guiConfig = new YamlConfiguration();
        }
    }
    
    /**
     * 获取GUI配置
     * @return GUI配置
     */
    public FileConfiguration getConfig() {
        if (guiConfig == null) {
            loadConfig();
        }
        return guiConfig;
    }
    
    /**
     * 保存GUI配置
     */
    public void saveConfig() {
        if (guiConfig != null) {
            try {
                guiConfig.save(new File(plugin.getDataFolder(), "gui.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save gui.yml: " + e.getMessage());
            }
        }
    }
    
    /**
     * 注册GUI页面
     * @param page 页面实例
     */
    public void registerPage(GUIPage page) {
        pages.put(page.getId(), page);
        DebugUtils.log("gui.menu_register", page.getId(), page.getClass().getSimpleName());
    }
    
    /**
     * 注销GUI页面
     * @param pageId 页面ID
     */
    public void unregisterPage(String pageId) {
        GUIPage page = pages.remove(pageId);
        if (page != null) {
            // 关闭所有正在查看此页面的玩家
            for (Player viewer : page.getViewers()) {
                page.close(viewer);
                playerPages.remove(viewer.getUniqueId());
            }
            
            DebugUtils.log("gui.menu_unregister", pageId);
        }
    }
    
    /**
     * 获取GUI页面
     * @param pageId 页面ID
     * @return 页面实例，如果不存在则返回null
     */
    public GUIPage getPage(String pageId) {
        return pages.get(pageId);
    }
    
    /**
     * 打开GUI页面给玩家
     * @param player 目标玩家
     * @param pageId 页面ID
     * @return 是否成功打开
     */
    public boolean openPage(Player player, String pageId) {
        GUIPage page = pages.get(pageId);
        if (page != null) {
            // 关闭当前页面
            String currentPageId = playerPages.get(player.getUniqueId());
            if (currentPageId != null && !currentPageId.equals(pageId)) {
                GUIPage currentPage = pages.get(currentPageId);
                if (currentPage != null) {
                    currentPage.close(player);
                }
                
                DebugUtils.log("gui.page_change", player.getName(), currentPageId, pageId);
            }
            
            // 打开新页面
            page.open(player);
            playerPages.put(player.getUniqueId(), pageId);
            return true;
        }
        return false;
    }
    
    /**
     * 更新玩家当前查看的页面
     * @param player 目标玩家
     */
    public void refreshPlayerPage(Player player) {
        String pageId = playerPages.get(player.getUniqueId());
        if (pageId != null) {
            GUIPage page = pages.get(pageId);
            if (page != null) {
                page.refresh(player);
            }
        }
    }
    
    /**
     * 关闭玩家当前的GUI页面
     * @param player 目标玩家
     */
    public void closePlayerPage(Player player) {
        String pageId = playerPages.get(player.getUniqueId());
        if (pageId != null) {
            GUIPage page = pages.get(pageId);
            if (page != null) {
                page.close(player);
            }
            playerPages.remove(player.getUniqueId());
        }
    }
    
    /**
     * 处理物品栏点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String pageId = playerPages.get(player.getUniqueId());
        
        if (pageId != null) {
            GUIPage page = pages.get(pageId);
            if (page != null) {
                event.setCancelled(true); // 取消默认行为
                page.handleClick(event);
            }
        }
    }
    
    /**
     * 处理物品栏关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        String pageId = playerPages.get(player.getUniqueId());
        
        if (pageId != null) {
            DebugUtils.log("gui.close", player.getName());
            playerPages.remove(player.getUniqueId());
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerPages.remove(player.getUniqueId());
    }
    
    /**
     * 获取字符串配置
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getConfigString(String path, String defaultValue) {
        if (guiConfig != null && guiConfig.contains(path)) {
            return guiConfig.getString(path, defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * 获取整数配置
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getConfigInt(String path, int defaultValue) {
        if (guiConfig != null && guiConfig.contains(path)) {
            return guiConfig.getInt(path, defaultValue);
        }
        return defaultValue;
    }
    
    /**
     * 重新加载所有配置和页面
     */
    public void reload() {
        // 关闭所有页面
        for (Map.Entry<UUID, String> entry : new HashMap<>(playerPages).entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                closePlayerPage(player);
            }
        }
        
        // 清空页面注册
        pages.clear();
        playerPages.clear();
        
        // 重新加载配置
        loadConfig();
        
        // 此时需要重新注册所有页面
        // 子类应该在重写的reload方法中调用super.reload()并重新注册页面
    }
} 