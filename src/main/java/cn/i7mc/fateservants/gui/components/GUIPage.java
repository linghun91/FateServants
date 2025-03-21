package cn.i7mc.fateservants.gui.components;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * GUI页面接口 - 定义一个GUI页面的基本功能
 * 每个页面包含多个组件，负责管理组件布局和事件分发
 */
public interface GUIPage {
    
    /**
     * 获取页面ID
     * @return 页面唯一标识符
     */
    String getId();
    
    /**
     * 获取页面标题
     * @return 页面标题
     */
    String getTitle();
    
    /**
     * 获取页面大小
     * @return 槽位数量
     */
    int getSize();
    
    /**
     * 添加组件到页面
     * @param component 组件实例
     * @param slot 槽位
     * @return 是否添加成功
     */
    boolean addComponent(GUIComponent component, int slot);
    
    /**
     * 移除槽位上的组件
     * @param slot 槽位
     * @return 被移除的组件，如果槽位为空则返回null
     */
    GUIComponent removeComponent(int slot);
    
    /**
     * 获取槽位上的组件
     * @param slot 槽位
     * @return 组件实例，如果槽位为空则返回null
     */
    GUIComponent getComponent(int slot);
    
    /**
     * 获取所有组件及其槽位
     * @return 组件映射表（槽位-组件）
     */
    Map<Integer, GUIComponent> getComponents();
    
    /**
     * 渲染整个页面为物品数组
     * @param player 目标玩家
     * @return 渲染后的物品数组，用于填充物品栏
     */
    ItemStack[] render(Player player);
    
    /**
     * 处理点击事件
     * @param event 原始物品栏点击事件
     * @return 是否已处理事件
     */
    boolean handleClick(InventoryClickEvent event);
    
    /**
     * 更新页面指定槽位的组件
     * @param slot 槽位
     * @param data 更新数据
     * @return 是否更新成功
     */
    boolean updateSlot(int slot, Object data);
    
    /**
     * 更新整个页面
     * @param player 目标玩家
     */
    void refresh(Player player);
    
    /**
     * 打开页面给玩家
     * @param player 目标玩家
     */
    void open(Player player);
    
    /**
     * 关闭页面
     * @param player 目标玩家
     */
    void close(Player player);
    
    /**
     * 获取查看此页面的玩家列表
     * @return 正在查看此页面的玩家
     */
    Iterable<Player> getViewers();
    
    /**
     * 页面打开时的回调方法
     * @param player 目标玩家
     */
    void onOpen(Player player);
} 