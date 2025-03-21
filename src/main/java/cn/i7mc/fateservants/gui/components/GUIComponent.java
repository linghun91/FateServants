package cn.i7mc.fateservants.gui.components;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * GUI组件接口 - 所有GUI组件的基础接口
 * 定义了一个GUI组件应该具备的基本功能
 */
public interface GUIComponent {
    
    /**
     * 获取组件ID
     * @return 组件唯一标识符
     */
    String getId();
    
    /**
     * 获取组件类型
     * @return 组件类型
     */
    String getType();
    
    /**
     * 渲染组件，生成用于显示的物品
     * @param player 目标玩家
     * @return 渲染后的物品
     */
    ItemStack render(Player player);
    
    /**
     * 处理玩家点击事件
     * @param player 点击的玩家
     * @param clickType 点击类型
     * @param isShiftClick 是否按住Shift键
     * @return 是否已处理此点击（如果返回false，将继续传递事件）
     */
    boolean onClick(Player player, ClickType clickType, boolean isShiftClick);
    
    /**
     * 更新组件状态
     * @param data 更新数据
     */
    void update(Object data);
    
    /**
     * 点击类型枚举
     */
    enum ClickType {
        LEFT,
        RIGHT,
        MIDDLE,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        DROP,
        CONTROL_DROP,
        DOUBLE_CLICK,
        NUMBER_KEY,
        UNKNOWN;
        
        /**
         * 从Bukkit的ClickType转换
         * @param bukkitClickType Bukkit点击类型
         * @return 对应的组件点击类型
         */
        public static ClickType fromBukkit(org.bukkit.event.inventory.ClickType bukkitClickType) {
            switch (bukkitClickType) {
                case LEFT:
                    return LEFT;
                case RIGHT:
                    return RIGHT;
                case MIDDLE:
                    return MIDDLE;
                case SHIFT_LEFT:
                    return SHIFT_LEFT;
                case SHIFT_RIGHT:
                    return SHIFT_RIGHT;
                case DROP:
                    return DROP;
                case CONTROL_DROP:
                    return CONTROL_DROP;
                case DOUBLE_CLICK:
                    return DOUBLE_CLICK;
                case NUMBER_KEY:
                    return NUMBER_KEY;
                default:
                    return UNKNOWN;
            }
        }
    }
} 