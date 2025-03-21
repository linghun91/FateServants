package cn.i7mc.fateservants.model.handlers;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.MessageManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 英灵全息图处理器
 * 负责管理英灵的全息文本显示
 */
public class ServantHologramHandler {
    private final Servant servant;
    private final Map<String, Integer> hologramEntityIds;
    private static final int VIEW_DISTANCE = 32;

    /**
     * 构造函数
     * @param servant 所属的英灵
     */
    public ServantHologramHandler(Servant servant) {
        this.servant = servant;
        this.hologramEntityIds = new HashMap<>();
    }

    /**
     * 创建全息图
     * @param id 全息图ID
     * @param text 显示文本
     * @param heightOffset 高度偏移
     * @param horizontalOffset 水平偏移
     */
    public void createHologram(String id, String text, double heightOffset, double horizontalOffset) {
        if (servant.getLocation() == null) return;

        try {
            int entityId = FateServants.getInstance().getPacketHandler().getNextEntityId();
            hologramEntityIds.put(id, entityId);
            
            Location location = servant.getLocation().clone().add(horizontalOffset, heightOffset, 0);
            
            PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getIntegers().write(1, 1); // EntityType.ARMOR_STAND
            
            // 设置位置
            spawnPacket.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());
                
            // 设置朝向
            spawnPacket.getBytes()
                .write(0, (byte) 0)
                .write(1, (byte) 0);
                
            // 设置速度（全部为0）
            spawnPacket.getIntegers()
                .write(2, 0)
                .write(3, 0)
                .write(4, 0);
                
            // 创建元数据数据包
            PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            
            List<WrappedDataWatcher.WrappedDataWatcherObject> metadataObjects = new ArrayList<>();
            
            // 通用元数据设置
            metadataObjects.add(new WrappedDataWatcher.WrappedDataWatcherObject(
                0, WrappedDataWatcher.Registry.get(Byte.class)
            )); // 实体标志
            
            metadataObjects.add(new WrappedDataWatcher.WrappedDataWatcherObject(
                1, WrappedDataWatcher.Registry.get(Integer.class)
            )); // 空气值
            
            metadataObjects.add(new WrappedDataWatcher.WrappedDataWatcherObject(
                2, WrappedDataWatcher.Registry.get(String.class)
            )); // 自定义名称
            
            metadataObjects.add(new WrappedDataWatcher.WrappedDataWatcherObject(
                3, WrappedDataWatcher.Registry.get(Boolean.class)
            )); // 自定义名称可见
            
            metadataObjects.add(new WrappedDataWatcher.WrappedDataWatcherObject(
                4, WrappedDataWatcher.Registry.get(Boolean.class)
            )); // 静音
            
            // 盔甲架特定元数据
            metadataObjects.add(new WrappedDataWatcher.WrappedDataWatcherObject(
                10, WrappedDataWatcher.Registry.get(Byte.class)
            )); // 盔甲架标志
            
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            watcher.setObject(0, (byte) 0x20); // 不可见
            watcher.setObject(1, 300); // 空气值
            watcher.setObject(2, text); // 名称
            watcher.setObject(3, true); // 显示名称
            watcher.setObject(4, true); // 静音
            watcher.setObject(10, (byte) 0x01 | (byte) 0x08 | (byte) 0x10); // 小盔甲架，无重力，无底座
            
            metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            
            // 发送数据包给所有在视野范围内的玩家
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().equals(servant.getLocation().getWorld())) continue;
                if (player.getLocation().distanceSquared(servant.getLocation()) > VIEW_DISTANCE * VIEW_DISTANCE) continue;
                
                FateServants.getInstance().getProtocolManager().sendServerPacket(player, spawnPacket);
                FateServants.getInstance().getProtocolManager().sendServerPacket(player, metadataPacket);
            }
            
            DebugUtils.log("hologram.create", id, text, heightOffset);
        } catch (Exception e) {
            DebugUtils.logObject(e, "创建全息图异常");
        }
    }

    /**
     * 为指定玩家创建全息图
     * @param id 全息图ID
     * @param text 显示文本
     * @param heightOffset 高度偏移
     * @param horizontalOffset 水平偏移
     * @param player 目标玩家
     */
    public void createHologram(String id, String text, double heightOffset, double horizontalOffset, Player player) {
        if (servant.getLocation() == null || player == null || !player.isOnline()) return;
        
        try {
            // 检查视距
            if (!player.getWorld().equals(servant.getLocation().getWorld()) || 
                player.getLocation().distanceSquared(servant.getLocation()) > VIEW_DISTANCE * VIEW_DISTANCE) {
                return;
            }
            
            String fullId = id + "_" + player.getUniqueId().toString();
            int entityId = FateServants.getInstance().getPacketHandler().getNextEntityId();
            hologramEntityIds.put(fullId, entityId);
            
            Location location = servant.getLocation().clone().add(horizontalOffset, heightOffset, 0);
            
            PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getIntegers().write(1, 1); // EntityType.ARMOR_STAND
            
            // 设置位置
            spawnPacket.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());
                
            // 设置朝向
            spawnPacket.getBytes()
                .write(0, (byte) 0)
                .write(1, (byte) 0);
                
            // 设置速度（全部为0）
            spawnPacket.getIntegers()
                .write(2, 0)
                .write(3, 0)
                .write(4, 0);
                
            // 创建元数据数据包
            PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            watcher.setObject(0, (byte) 0x20); // 不可见
            watcher.setObject(1, 300); // 空气值
            watcher.setObject(2, text); // 名称
            watcher.setObject(3, true); // 显示名称
            watcher.setObject(4, true); // 静音
            watcher.setObject(10, (byte) 0x01 | (byte) 0x08 | (byte) 0x10); // 小盔甲架，无重力，无底座
            
            metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            
            // 发送数据包给指定玩家
            FateServants.getInstance().getProtocolManager().sendServerPacket(player, spawnPacket);
            FateServants.getInstance().getProtocolManager().sendServerPacket(player, metadataPacket);
            
            DebugUtils.log("hologram.create_for_player", id, player.getName(), text);
        } catch (Exception e) {
            DebugUtils.logObject(e, "为玩家" + player.getName() + "创建全息图异常");
        }
    }

    /**
     * 更新全息图文本
     * @param id 全息图ID
     * @param text 新的显示文本
     */
    public void updateHologramText(String id, String text) {
        Integer entityId = hologramEntityIds.get(id);
        if (entityId == null) return;
        
        try {
            PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            watcher.setObject(2, text);
            
            metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            
            // 向所有在视野范围内的玩家发送更新包
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().equals(servant.getLocation().getWorld())) continue;
                if (player.getLocation().distanceSquared(servant.getLocation()) > VIEW_DISTANCE * VIEW_DISTANCE) continue;
                
                FateServants.getInstance().getProtocolManager().sendServerPacket(player, metadataPacket);
            }
            
            DebugUtils.log("hologram.update", id, text);
        } catch (Exception e) {
            DebugUtils.logObject(e, "更新全息图文本异常");
        }
    }

    /**
     * 更新全息图位置
     * @param id 全息图ID
     * @param heightOffset 高度偏移
     * @param horizontalOffset 水平偏移
     */
    public void updateHologramPosition(String id, double heightOffset, double horizontalOffset) {
        Integer entityId = hologramEntityIds.get(id);
        if (entityId == null || servant.getLocation() == null) return;
        
        try {
            Location hologramLoc = servant.getLocation().clone().add(horizontalOffset, heightOffset, 0);
            
            PacketContainer teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles()
                .write(0, hologramLoc.getX())
                .write(1, hologramLoc.getY())
                .write(2, hologramLoc.getZ());
                
            // 向所有在视野范围内的玩家发送位置更新包
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().equals(servant.getLocation().getWorld())) continue;
                if (player.getLocation().distanceSquared(servant.getLocation()) > VIEW_DISTANCE * VIEW_DISTANCE) continue;
                
                FateServants.getInstance().getProtocolManager().sendServerPacket(player, teleportPacket);
            }
            
            DebugUtils.log("hologram.move", id, heightOffset, horizontalOffset);
        } catch (Exception e) {
            DebugUtils.logObject(e, "更新全息图位置异常");
        }
    }

    /**
     * 移除全息图
     * @param id 全息图ID
     */
    public void removeHologram(String id) {
        Integer entityId = hologramEntityIds.remove(id);
        if (entityId != null) {
            try {
                PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
                destroyPacket.getIntegerArrays().write(0, new int[]{entityId});
                
                // 向所有在线玩家发送销毁包
                for (Player player : Bukkit.getOnlinePlayers()) {
                    FateServants.getInstance().getProtocolManager().sendServerPacket(player, destroyPacket);
                }
                
                DebugUtils.log("hologram.remove", id);
            } catch (Exception e) {
                DebugUtils.logObject(e, "移除全息图异常");
            }
        }
    }

    /**
     * 更新所有全息图
     */
    public void updateAllHolograms() {
        // 从message.yml读取配置
        FileConfiguration config = FateServants.getInstance().getMessageConfig();
        
        // 获取血条设置
        String filledColor = config.getString("health_bar.filled_color", "§4");
        String emptyColor = config.getString("health_bar.empty_color", "§8");
        String heartSymbol = config.getString("health_bar.heart_symbol", "❤");
        int totalHearts = config.getInt("health_bar.total_hearts", 10);
        
        // 计算血条
        int filledHearts = (int) Math.ceil((servant.getCurrentHealth() / servant.getMaxHealth()) * totalHearts);
        filledHearts = Math.max(0, Math.min(totalHearts, filledHearts));
        
        StringBuilder healthBar = new StringBuilder();
        for (int i = 0; i < totalHearts; i++) {
            healthBar.append(i < filledHearts ? filledColor : emptyColor).append(heartSymbol);
        }
        
        // 获取高度设置
        double baseHeight = config.getDouble("messages.height.base", 2.0);
        double heightInterval = config.getDouble("messages.height.interval", 0.25);
        
        // 更新所有已启用的消息
        for (int i = 1; i <= 4; i++) {
            String msgKey = "msg" + i;
            boolean enabled = config.getBoolean("messages." + msgKey + ".enabled", false);
            
            if (enabled) {
                String format = config.getString("messages." + msgKey + ".format", "");
                String text = replaceVariables(format, healthBar.toString());
                
                double height = baseHeight + ((i - 1) * heightInterval);
                String holoId = "msg" + i;
                
                // 检查是否已存在该全息图
                if (hologramEntityIds.containsKey(holoId)) {
                    updateHologramText(holoId, text);
                    updateHologramPosition(holoId, height, 0);
                } else {
                    createHologram(holoId, text, height, 0);
                }
            } else {
                // 如果消息被禁用但存在全息图，则移除
                String holoId = "msg" + i;
                if (hologramEntityIds.containsKey(holoId)) {
                    removeHologram(holoId);
                }
            }
        }
    }

    /**
     * 移除所有全息图
     */
    public void removeAllHolograms() {
        // 创建一个副本以避免并发修改异常
        new HashMap<>(hologramEntityIds).keySet().forEach(this::removeHologram);
        hologramEntityIds.clear();
    }

    /**
     * 替换变量
     * @param text 带变量的文本
     * @param healthBar 预生成的血条字符串
     * @return 替换后的文本
     */
    private String replaceVariables(String text, String healthBar) {
        return text
            .replace("{class_name}", servant.getServantClass().getDisplayName())
            .replace("{level}", String.valueOf(servant.getLevel()))
            .replace("{quality}", servant.getQuality().getDisplayName())
            .replace("{owner}", servant.getOwner().getName())
            .replace("{health}", String.format("%.1f", servant.getCurrentHealth()))
            .replace("{max_health}", String.format("%.1f", servant.getMaxHealth()))
            .replace("{health_percentage}", String.format("%.1f", (servant.getCurrentHealth() / servant.getMaxHealth() * 100)))
            .replace("{health_bar}", healthBar);
    }
    
    /**
     * 检查是否为全息图实体
     * @param entityId 实体ID
     * @return 是否为全息图实体
     */
    public boolean isHologramEntity(int entityId) {
        return hologramEntityIds.containsValue(entityId);
    }
    
    /**
     * 获取所有全息图实体ID
     * @return 实体ID列表
     */
    public List<Integer> getAllHologramEntityIds() {
        return new ArrayList<>(hologramEntityIds.values());
    }
} 