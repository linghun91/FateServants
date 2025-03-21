package cn.i7mc.fateservants.gui;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.config.ConfigManager;
import cn.i7mc.fateservants.utils.MaterialAdapter;
import cn.i7mc.fateservants.utils.MessageManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI配置管理器，用于从gui.yml加载GUI配置
 */
public class GUIConfigManager {
    private final FateServants plugin;
    private final ConfigManager configManager;
    
    // GUI页面配置缓存
    private final Map<String, GUIPageConfig> pageConfigs = new HashMap<>();
    
    /**
     * 构造函数
     * @param plugin 插件实例
     * @param configManager 配置管理器实例
     */
    public GUIConfigManager(FateServants plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadAllPageConfigs();
    }
    
    /**
     * 加载所有GUI页面配置
     */
    public void loadAllPageConfigs() {
        pageConfigs.clear();
        
        ConfigurationSection pagesSection = configManager.getConfigSection("gui.yml", "pages");
        if (pagesSection == null) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("gui.no_pages_section");
            }
            return;
        }
        
        for (String pageId : pagesSection.getKeys(false)) {
            ConfigurationSection pageSection = pagesSection.getConfigurationSection(pageId);
            if (pageSection == null) continue;
            
            GUIPageConfig pageConfig = loadPageConfig(pageId, pageSection);
            pageConfigs.put(pageId, pageConfig);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                MessageManager.debug("gui.loaded_page", pageId);
            }
        }
    }
    
    /**
     * 加载页面配置
     * @param pageId 页面ID
     * @param pageSection 页面配置节
     * @return 页面配置
     */
    private GUIPageConfig loadPageConfig(String pageId, ConfigurationSection pageSection) {
        String title = pageSection.getString("title", "§8" + pageId);
        int size = pageSection.getInt("size", 54);
        
        // 加载组件配置
        Map<String, GUIComponentConfig> componentConfigs = new HashMap<>();
        ConfigurationSection componentsSection = pageSection.getConfigurationSection("components");
        
        if (componentsSection != null) {
            for (String componentId : componentsSection.getKeys(false)) {
                ConfigurationSection componentSection = componentsSection.getConfigurationSection(componentId);
                if (componentSection == null) continue;
                
                GUIComponentConfig componentConfig = loadComponentConfig(componentId, componentSection);
                componentConfigs.put(componentId, componentConfig);
            }
        }
        
        return new GUIPageConfig(pageId, title, size, componentConfigs);
    }
    
    /**
     * 加载组件配置
     * @param componentId 组件ID
     * @param componentSection 组件配置节
     * @return 组件配置
     */
    private GUIComponentConfig loadComponentConfig(String componentId, ConfigurationSection componentSection) {
        String type = componentSection.getString("type", "button");
        int slot = componentSection.getInt("slot", -1);
        String material = componentSection.getString("material", "STONE");
        int amount = componentSection.getInt("amount", 1);
        short data = (short) componentSection.getInt("data", 0);
        String displayName = componentSection.getString("display_name", "");
        List<String> lore = componentSection.getStringList("lore");
        String action = componentSection.getString("action", "");
        
        // 加载处理器配置
        Map<String, Object> handlerConfig = new HashMap<>();
        ConfigurationSection handlerSection = componentSection.getConfigurationSection("handler");
        
        if (handlerSection != null) {
            for (String key : handlerSection.getKeys(false)) {
                handlerConfig.put(key, handlerSection.get(key));
            }
        }
        
        return new GUIComponentConfig(componentId, type, slot, material, data, amount, displayName, lore, action, handlerConfig);
    }
    
    /**
     * 获取页面配置
     * @param pageId 页面ID
     * @return 页面配置，如果不存在则返回null
     */
    public GUIPageConfig getPageConfig(String pageId) {
        return pageConfigs.get(pageId);
    }
    
    /**
     * 重新加载所有配置
     */
    public void reload() {
        loadAllPageConfigs();
    }
    
    /**
     * 创建物品
     * @param config 组件配置
     * @return 物品对象
     */
    public static ItemStack createItem(GUIComponentConfig config) {
        Material material = MaterialAdapter.getMaterial(config.getMaterial());
        ItemStack item = new ItemStack(material, config.getAmount(), config.getData());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (config.getDisplayName() != null && !config.getDisplayName().isEmpty()) {
                meta.setDisplayName(config.getDisplayName());
            }
            
            List<String> lore = config.getLore();
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * GUI页面配置类
     */
    public static class GUIPageConfig {
        private final String id;
        private final String title;
        private final int size;
        private final Map<String, GUIComponentConfig> components;
        
        public GUIPageConfig(String id, String title, int size, Map<String, GUIComponentConfig> components) {
            this.id = id;
            this.title = title;
            this.size = size;
            this.components = components;
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public int getSize() {
            return size;
        }
        
        public Map<String, GUIComponentConfig> getComponents() {
            return components;
        }
        
        public GUIComponentConfig getComponent(String componentId) {
            return components.get(componentId);
        }
    }
    
    /**
     * GUI组件配置类
     */
    public static class GUIComponentConfig {
        private final String id;
        private final String type;
        private final int slot;
        private final String material;
        private final short data;
        private final int amount;
        private final String displayName;
        private final List<String> lore;
        private final String action;
        private final Map<String, Object> handlerConfig;
        
        public GUIComponentConfig(String id, String type, int slot, String material, short data, int amount, 
                String displayName, List<String> lore, String action, Map<String, Object> handlerConfig) {
            this.id = id;
            this.type = type;
            this.slot = slot;
            this.material = material;
            this.data = data;
            this.amount = amount;
            this.displayName = displayName;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.action = action;
            this.handlerConfig = handlerConfig != null ? handlerConfig : new HashMap<>();
        }
        
        public String getId() {
            return id;
        }
        
        public String getType() {
            return type;
        }
        
        public int getSlot() {
            return slot;
        }
        
        public String getMaterial() {
            return material;
        }
        
        public short getData() {
            return data;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public List<String> getLore() {
            return lore;
        }
        
        public String getAction() {
            return action;
        }
        
        public Map<String, Object> getHandlerConfig() {
            return handlerConfig;
        }
    }
} 