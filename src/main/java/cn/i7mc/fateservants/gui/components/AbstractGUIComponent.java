package cn.i7mc.fateservants.gui.components;

import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 抽象GUI组件 - 实现基本组件功能的抽象类
 * 具体组件可以继承此类，专注于实现自己的特定功能
 */
public abstract class AbstractGUIComponent implements GUIComponent {
    protected final String id;
    protected final String type;
    protected String displayName;
    protected Material material;
    protected List<String> lore;
    protected boolean glow = false;
    protected int customModelData = -1;
    protected int slot = -1;  // 添加槽位属性，默认为-1表示未设置
    
    /**
     * 创建一个基础GUI组件
     * @param type 组件类型
     * @param material 物品材质
     * @param displayName 显示名称
     */
    public AbstractGUIComponent(String type, Material material, String displayName) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.material = material;
        this.displayName = displayName;
        this.lore = new ArrayList<>();
        
        DebugUtils.log("gui.component_add", this.type, this.id);
    }
    
    /**
     * 使用自定义ID创建组件
     * @param id 自定义ID
     * @param type 组件类型
     * @param material 物品材质
     * @param displayName 显示名称
     */
    public AbstractGUIComponent(String id, String type, Material material, String displayName) {
        this.id = id;
        this.type = type;
        this.material = material;
        this.displayName = displayName;
        this.lore = new ArrayList<>();
        
        DebugUtils.log("gui.component_add", this.type, this.id);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    /**
     * 获取组件所在槽位
     * @return 槽位索引，如果未设置则返回-1
     */
    public int getSlot() {
        return slot;
    }
    
    /**
     * 设置组件所在槽位
     * @param slot 槽位索引
     * @return 当前组件实例，用于链式调用
     */
    public AbstractGUIComponent setSlot(int slot) {
        this.slot = slot;
        return this;
    }
    
    /**
     * 设置物品描述
     * @param lore 描述文本列表
     * @return 当前组件实例，用于链式调用
     */
    public AbstractGUIComponent setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }
    
    /**
     * 添加物品描述行
     * @param line 描述文本行
     * @return 当前组件实例，用于链式调用
     */
    public AbstractGUIComponent addLoreLine(String line) {
        this.lore.add(line);
        return this;
    }
    
    /**
     * 设置物品是否发光
     * @param glow 是否发光
     * @return 当前组件实例，用于链式调用
     */
    public AbstractGUIComponent setGlow(boolean glow) {
        this.glow = glow;
        return this;
    }
    
    /**
     * 设置物品自定义模型数据
     * @param customModelData 自定义模型数据ID
     * @return 当前组件实例，用于链式调用
     */
    public AbstractGUIComponent setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        if (customModelData > 0) {
            DebugUtils.log("gui.custom_model_data", String.valueOf(customModelData), this.type);
        }
        return this;
    }
    
    /**
     * 设置物品材质
     * @param material 材质类型
     * @return 当前组件实例，用于链式调用
     */
    public AbstractGUIComponent setMaterial(Material material) {
        this.material = material;
        return this;
    }
    
    /**
     * 设置物品显示名称
     * @param displayName 显示名称
     * @return 当前组件实例，用于链式调用
     */
    public AbstractGUIComponent setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
    
    /**
     * 创建基础物品
     * @return 带有基本MetaData的物品
     */
    protected ItemStack createBaseItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            
            if (customModelData > 0) {
                // 对于不支持CustomModelData的版本，可以用其他方式标记自定义模型
                // 例如使用物品的damage值作为标识
                try {
                    // 1.14+版本使用CustomModelData
                    meta.getClass().getMethod("setCustomModelData", Integer.class).invoke(meta, customModelData);
                } catch (Exception e) {
                    // 旧版本使用Durability
                    if (customModelData <= Short.MAX_VALUE) {
                        item.setDurability((short) customModelData);
                    }
                }
            }
            
            // 如果需要发光效果，可以在这里添加
            
            item.setItemMeta(meta);
        }
        
        DebugUtils.log("gui.item_create", this.id, this.material.name());
        return item;
    }
    
    /**
     * 预处理物品描述，可替换变量
     * @param player 目标玩家
     * @return 处理后的描述文本
     */
    protected List<String> processLore(Player player) {
        List<String> processedLore = new ArrayList<>();
        for (String line : lore) {
            // 处理变量替换
            String processed = line
                .replace("%player%", player.getName());
            // 可以添加更多变量替换
            
            processedLore.add(processed);
        }
        return processedLore;
    }
    
    /**
     * 默认的更新方法实现，子类可根据需要重写
     */
    @Override
    public void update(Object data) {
        // 默认实现为空
    }
    
    /**
     * 默认的点击事件处理，子类应当重写此方法
     */
    @Override
    public boolean onClick(Player player, ClickType clickType, boolean isShiftClick) {
        return false; // 默认不处理点击
    }
    
    /**
     * 设置组件对应玩家的物品
     * @param player 玩家
     * @param item 物品
     */
    public void setItem(Player player, ItemStack item) {
        // 默认实现，不做任何操作
        DebugUtils.log("gui.set_item", player.getName(), getId(), item.getType().name());
    }
} 