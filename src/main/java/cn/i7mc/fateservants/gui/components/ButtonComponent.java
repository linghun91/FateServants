package cn.i7mc.fateservants.gui.components;

import cn.i7mc.fateservants.utils.DebugUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * 按钮组件 - 可点击触发回调的组件
 */
public class ButtonComponent extends AbstractGUIComponent {
    private BiConsumer<Player, ClickType> clickHandler;
    private BiFunction<Player, ButtonComponent, Boolean> actionHandler;
    private final List<ClickType> enabledClickTypes = new ArrayList<>();
    
    /**
     * 创建按钮组件
     * @param displayName 显示名称
     * @param material 材质
     */
    public ButtonComponent(String displayName, Material material) {
        super("button", material, displayName);
        
        // 默认启用所有点击类型
        for (ClickType clickType : ClickType.values()) {
            this.enabledClickTypes.add(clickType);
        }
    }
    
    /**
     * 创建具有自定义ID的按钮组件
     * @param id 组件ID
     * @param displayName 显示名称
     * @param material 材质
     */
    public ButtonComponent(String id, String displayName, Material material) {
        super(id, "button", material, displayName);
        
        // 默认启用所有点击类型
        for (ClickType clickType : ClickType.values()) {
            this.enabledClickTypes.add(clickType);
        }
    }
    
    /**
     * 创建具有自定义ID和指定槽位的按钮组件
     * @param id 组件ID
     * @param slot 槽位
     * @param material 材质
     */
    public ButtonComponent(String id, int slot, Material material) {
        super(id, "button", material, "");
        setSlot(slot);
        
        // 默认启用所有点击类型
        for (ClickType clickType : ClickType.values()) {
            this.enabledClickTypes.add(clickType);
        }
    }
    
    /**
     * 创建具有自定义ID、指定槽位和动作处理器的按钮组件
     * @param id 组件ID
     * @param slot 槽位
     * @param actionHandler 动作处理器
     */
    public ButtonComponent(String id, int slot, BiFunction<Player, ButtonComponent, Boolean> actionHandler) {
        super(id, "button", Material.STONE, "");
        setSlot(slot);
        this.actionHandler = actionHandler;
        
        // 默认启用所有点击类型
        for (ClickType clickType : ClickType.values()) {
            this.enabledClickTypes.add(clickType);
        }
    }
    
    /**
     * 设置点击处理器
     * @param clickHandler 点击处理器
     * @return 按钮组件实例（链式调用）
     */
    public ButtonComponent setClickHandler(BiConsumer<Player, ClickType> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }
    
    /**
     * 启用特定的点击类型
     * @param clickTypes 要启用的点击类型
     * @return 按钮组件实例（链式调用）
     */
    public ButtonComponent enableClickTypes(ClickType... clickTypes) {
        this.enabledClickTypes.clear();
        for (ClickType clickType : clickTypes) {
            this.enabledClickTypes.add(clickType);
        }
        return this;
    }
    
    /**
     * 禁用特定的点击类型
     * @param clickTypes 要禁用的点击类型
     * @return 按钮组件实例（链式调用）
     */
    public ButtonComponent disableClickTypes(ClickType... clickTypes) {
        for (ClickType clickType : clickTypes) {
            this.enabledClickTypes.remove(clickType);
        }
        return this;
    }
    
    /**
     * 检查点击类型是否已启用
     * @param clickType 要检查的点击类型
     * @return 是否已启用
     */
    public boolean isClickTypeEnabled(ClickType clickType) {
        return this.enabledClickTypes.contains(clickType);
    }
    
    @Override
    public ItemStack render(Player player) {
        return createBaseItem();
    }
    
    @Override
    public boolean onClick(Player player, ClickType clickType, boolean isShiftClick) {
        // 检查点击类型是否已启用
        if (!isClickTypeEnabled(clickType)) {
            return false;
        }
        
        DebugUtils.log("gui.click", player.getName(), getId(), clickType.name());
        
        // 先尝试调用新的动作处理器
        if (actionHandler != null) {
            return actionHandler.apply(player, this);
        }
        
        // 然后尝试原有的点击处理器
        if (clickHandler != null) {
            clickHandler.accept(player, clickType);
            return true;
        }
        
        return false;
    }
} 