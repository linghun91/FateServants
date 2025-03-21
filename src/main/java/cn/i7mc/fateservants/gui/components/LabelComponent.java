package cn.i7mc.fateservants.gui.components;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 标签组件 - 用于显示只读的文本信息
 */
public class LabelComponent extends AbstractGUIComponent {
    
    /**
     * 创建标签组件
     * @param displayName 显示名称
     * @param material 材质
     */
    public LabelComponent(String displayName, Material material) {
        super("label", material, displayName);
    }
    
    /**
     * 创建具有自定义ID的标签组件
     * @param id 组件ID
     * @param displayName 显示名称
     * @param material 材质
     */
    public LabelComponent(String id, String displayName, Material material) {
        super(id, "label", material, displayName);
    }
    
    /**
     * 创建具有自定义ID和指定槽位的标签组件
     * @param id 组件ID
     * @param slot 槽位
     */
    public LabelComponent(String id, int slot) {
        super(id, "label", Material.PAPER, "");
        setSlot(slot);
    }
    
    /**
     * 设置组件的动态变量
     * @param player 目标玩家
     * @param variables 变量键值对
     * @return 标签组件实例（链式调用）
     */
    public LabelComponent setVariables(Player player, String... variables) {
        // 清空旧的lore
        this.lore = processLore(player);
        
        // 添加新的变量文本
        for (int i = 0; i < variables.length - 1; i += 2) {
            if (i + 1 < variables.length) {
                String key = variables[i];
                String value = variables[i + 1];
                this.addLoreLine(key + ": " + value);
            }
        }
        
        return this;
    }
    
    @Override
    public ItemStack render(Player player) {
        return createBaseItem();
    }
    
    @Override
    public boolean onClick(Player player, ClickType clickType, boolean isShiftClick) {
        // 标签组件不响应点击
        return false;
    }
} 