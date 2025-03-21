package cn.i7mc.fateservants.manager;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantClass;
import cn.i7mc.fateservants.utils.DebugUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServantManager {
    private final FateServants plugin;
    private final Map<UUID, List<Servant>> playerServants = new ConcurrentHashMap<>();
    private final Map<UUID, Servant> activeServants = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownerToServant = new HashMap<>();

    public ServantManager(FateServants plugin) {
        this.plugin = plugin;
    }

    public boolean hasServant(Player player) {
        return activeServants.containsKey(player.getUniqueId()) || 
               (playerServants.containsKey(player.getUniqueId()) && !playerServants.get(player.getUniqueId()).isEmpty());
    }

    public Servant getServant(Player player) {
        UUID servantId = ownerToServant.get(player.getUniqueId());
        return servantId != null ? activeServants.get(player.getUniqueId()) : null;
    }

    public Servant createServant(Player player, ServantClass servantClass) {
        if (hasServant(player)) {
            return null;
        }

        Servant servant = new Servant(player, servantClass);
        activeServants.put(player.getUniqueId(), servant);
        ownerToServant.put(player.getUniqueId(), servant.getUuid());
        return servant;
    }

    /**
     * 添加一个已存在的英灵实例到服务管理器中
     * @param player 玩家
     * @param servant 英灵实例
     * @return 添加的英灵实例
     */
    public Servant addServant(Player player, Servant servant) {
        if (servant == null) {
            return null;
        }
        
        activeServants.put(player.getUniqueId(), servant);
        ownerToServant.put(player.getUniqueId(), servant.getUuid());
        
        // 如果还没有该玩家的英灵列表，创建一个
        if (!playerServants.containsKey(player.getUniqueId())) {
            playerServants.put(player.getUniqueId(), new CopyOnWriteArrayList<>());
        }
        
        return servant;
    }

    public boolean removeServant(Player player) {
        UUID servantId = ownerToServant.remove(player.getUniqueId());
        if (servantId != null) {
            Servant servant = activeServants.remove(player.getUniqueId());
            if (servant != null) {
                // 在这里不调用数据库保存，因为已经在PlayerQuitListener中处理了
                servant.remove();
                // 同时清除存储的英灵列表
                playerServants.remove(player.getUniqueId());
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有当前存在的英灵
     * @return 所有英灵的集合
     */
    public Collection<Servant> getAllServants() {
        return activeServants.values();
    }

    public Collection<Servant> getServants() {
        return activeServants.values();
    }

    /**
     * 保存玩家的英灵信息
     * @param player 要保存英灵信息的玩家
     */
    public void savePlayerServants(Player player) {
        // 移除此方法，因为我们不需要在玩家下线时保存英灵
        // 或者如果需要持久化存储，应该保存到配置文件或数据库中
    }

    /**
     * 加载玩家的英灵信息
     * @param player 要加载英灵信息的玩家
     */
    public void loadPlayerServants(Player player) {
        // 如果需要从持久化存储加载，应该从配置文件或数据库加载
        // 目前不需要此方法
    }

    /**
     * 保存所有英灵数据
     */
    public void saveAll() {
        if (plugin.getDatabaseManager() == null) {
            plugin.getLogger().warning("无法保存英灵数据：数据库管理器未初始化");
            return;
        }
        
        for (Map.Entry<UUID, Servant> entry : activeServants.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner != null && owner.isOnline()) {
                try {
                    plugin.getDatabaseManager().saveServant(owner, entry.getValue());
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("已保存 " + owner.getName() + " 的英灵数据");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("保存 " + owner.getName() + " 的英灵数据时出错: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 从数据库加载所有在线玩家的英灵数据
     */
    public void loadAll() {
        if (plugin.getDatabaseManager() == null) {
            plugin.getLogger().warning("无法加载英灵数据：数据库管理器未初始化");
            return;
        }
        
        // 为所有在线玩家加载英灵数据
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                Servant servant = plugin.getDatabaseManager().loadServant(player);
                if (servant != null) {
                    // 移除现有的英灵（如果有）
                    if (hasServant(player)) {
                        removeServant(player);
                    }
                    
                    // 直接使用从数据库加载的英灵
                    activeServants.put(player.getUniqueId(), servant);
                    ownerToServant.put(player.getUniqueId(), servant.getUuid());
                    
                    // 重新生成英灵的全息图和其他视觉元素
                    servant.spawn();
                    
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("已加载 " + player.getName() + " 的英灵数据");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载 " + player.getName() + " 的英灵数据时出错: " + e.getMessage());
                DebugUtils.logException("debug.servantdata.load.error", e, player.getName());
            }
        }
    }
} 