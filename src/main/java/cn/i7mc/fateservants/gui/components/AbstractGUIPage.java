package cn.i7mc.fateservants.gui.components;

import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 抽象GUI页面 - 实现基本页面功能
 * 管理组件布局和事件分发
 */
public abstract class AbstractGUIPage implements GUIPage {
    protected final String id;
    protected final String title;
    protected final int size;
    protected final Map<Integer, GUIComponent> components = new HashMap<>();
    protected final Map<String, GUIComponent> componentsByName = new HashMap<>(); // 按名称索引组件
    protected final Set<UUID> viewers = new HashSet<>();
    protected Inventory inventory;
    
    /**
     * 创建一个GUI页面
     * @param id 页面ID
     * @param title 页面标题
     * @param size 页面大小（槽位数量）
     */
    public AbstractGUIPage(String id, String title, int size) {
        this.id = id;
        this.title = title;
        
        // 确保大小是9的倍数，且在有效范围内
        int adjustedSize = (size / 9) * 9;
        if (adjustedSize < 9) {
            adjustedSize = 9;
        } else if (adjustedSize > 54) {
            adjustedSize = 54;
        }
        this.size = adjustedSize;
        
        // 创建物品栏
        this.inventory = Bukkit.createInventory(null, this.size, title);
        
        DebugUtils.log("gui.page_create", id, String.valueOf(this.size), title);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public int getSize() {
        return size;
    }
    
    /**
     * 获取页面物品栏
     * @return 物品栏实例
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * 注册组件到页面
     * 如果组件已设置槽位，则使用该槽位；否则需要指定槽位
     * @param component 组件实例
     * @return 是否注册成功
     */
    public boolean registerComponent(GUIComponent component) {
        int slot = -1;
        
        // 如果组件是AbstractGUIComponent的实例，尝试获取其槽位
        if (component instanceof AbstractGUIComponent) {
            slot = ((AbstractGUIComponent) component).getSlot();
        }
        
        // 如果没有设置槽位，则注册失败
        if (slot < 0 || slot >= size) {
            DebugUtils.log("gui.register_component_error", component.getId(), "无效槽位: " + slot);
            return false;
        }
        
        // 添加组件
        components.put(slot, component);
        componentsByName.put(component.getId(), component);
        
        DebugUtils.log("gui.register_component", getId(), component.getId(), String.valueOf(slot));
        
        return true;
    }
    
    /**
     * 注册组件到页面，同时指定槽位
     * @param component 组件实例
     * @param slot 槽位
     * @return 是否注册成功
     */
    public boolean registerComponent(GUIComponent component, int slot) {
        // 设置槽位（如果组件是AbstractGUIComponent的实例）
        if (component instanceof AbstractGUIComponent) {
            ((AbstractGUIComponent) component).setSlot(slot);
        }
        
        // 注册组件
        return registerComponent(component);
    }
    
    @Override
    public boolean addComponent(GUIComponent component, int slot) {
        if (slot >= 0 && slot < size) {
            components.put(slot, component);
            componentsByName.put(component.getId(), component);
            DebugUtils.log("gui.item_set", this.id, String.valueOf(slot), component.getType());
            return true;
        }
        return false;
    }
    
    @Override
    public GUIComponent removeComponent(int slot) {
        GUIComponent removed = components.remove(slot);
        if (removed != null) {
            componentsByName.remove(removed.getId());
        }
        return removed;
    }
    
    @Override
    public GUIComponent getComponent(int slot) {
        return components.get(slot);
    }
    
    /**
     * 根据ID获取组件
     * @param id 组件ID
     * @return 组件实例，如果不存在则返回null
     */
    public GUIComponent getComponent(String id) {
        return componentsByName.get(id);
    }
    
    @Override
    public Map<Integer, GUIComponent> getComponents() {
        return Collections.unmodifiableMap(components);
    }
    
    @Override
    public ItemStack[] render(Player player) {
        ItemStack[] items = new ItemStack[size];
        
        for (Map.Entry<Integer, GUIComponent> entry : components.entrySet()) {
            int slot = entry.getKey();
            GUIComponent component = entry.getValue();
            
            if (slot >= 0 && slot < items.length) {
                items[slot] = component.render(player);
                DebugUtils.log("gui.render_component", component.getType(), component.getId());
            }
        }
        
        return items;
    }
    
    @Override
    public boolean handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return false;
        }
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        // 检查点击的是否为有效位置
        if (slot < 0 || slot >= size) {
            return false;
        }
        
        // 获取点击的组件
        GUIComponent component = components.get(slot);
        if (component == null) {
            return false;
        }
        
        // 解析点击类型
        GUIComponent.ClickType clickType = GUIComponent.ClickType.fromBukkit(event.getClick());
        boolean isShiftClick = event.isShiftClick();
        
        DebugUtils.log("gui.click_handle", player.getName(), String.valueOf(slot));
        
        // 转发点击事件到组件
        return component.onClick(player, clickType, isShiftClick);
    }
    
    @Override
    public boolean updateSlot(int slot, Object data) {
        GUIComponent component = components.get(slot);
        if (component != null) {
            component.update(data);
            return true;
        }
        return false;
    }
    
    @Override
    public void refresh(Player player) {
        if (viewers.contains(player.getUniqueId())) {
            // 获取玩家的物品栏
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory.getSize() == size) {
                // 重新渲染并更新物品
                ItemStack[] items = render(player);
                for (int i = 0; i < items.length; i++) {
                    if (items[i] != null) {
                        inventory.setItem(i, items[i]);
                    }
                }
                
                DebugUtils.log("gui.update", player.getName(), this.id);
            }
        }
    }
    
    @Override
    public void open(Player player) {
        // 创建物品栏并填充
        Inventory inventory = Bukkit.createInventory(null, size, title);
        ItemStack[] items = render(player);
        
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                inventory.setItem(i, items[i]);
            }
        }
        
        // 打开物品栏
        player.openInventory(inventory);
        viewers.add(player.getUniqueId());
        
        DebugUtils.log("gui.open", this.id, player.getName());
    }
    
    @Override
    public void close(Player player) {
        if (viewers.contains(player.getUniqueId())) {
            player.closeInventory();
            viewers.remove(player.getUniqueId());
            
            DebugUtils.log("gui.close", player.getName());
        }
    }
    
    @Override
    public Iterable<Player> getViewers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }
} 