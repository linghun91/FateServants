package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据包工具类，提供数据包批量发送功能
 */
public class PacketUtils {
    private static final int BATCH_SIZE = 10; // 每批发送的数据包数量
    
    /**
     * 向玩家发送数据包
     * @param player 玩家
     * @param packet 数据包
     */
    public static void sendPacket(Player player, PacketContainer packet) {
        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager.sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            DebugUtils.logException("packet.send_error", e, player.getName(), packet.getType().name());
        }
    }
    
    /**
     * 向多个玩家批量发送数据包
     * @param players 玩家列表
     * @param packet 数据包
     */
    public static void sendPacketBatch(Collection<Player> players, PacketContainer packet) {
        if (players.isEmpty()) {
            return;
        }
        
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        
        try {
            for (Player player : players) {
                protocolManager.sendServerPacket(player, packet);
            }
            
            OptimizationUtils.logPacketBatch(packet.getType().name(), 1, players.size());
        } catch (InvocationTargetException e) {
            DebugUtils.logException("packet.batch_send_error", e, packet.getType().name(), players.size());
        }
    }
    
    /**
     * 向多个玩家批量发送多个数据包
     * @param players 玩家列表
     * @param packets 数据包列表
     */
    public static void sendPacketsBatch(Collection<Player> players, List<PacketContainer> packets) {
        if (players.isEmpty() || packets.isEmpty()) {
            return;
        }
        
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        
        try {
            // 将数据包分批处理
            for (int i = 0; i < packets.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, packets.size());
                List<PacketContainer> batch = packets.subList(i, end);
                
                for (Player player : players) {
                    for (PacketContainer packet : batch) {
                        protocolManager.sendServerPacket(player, packet);
                    }
                }
            }
            
            OptimizationUtils.logPacketBatch("多类型", packets.size(), players.size());
        } catch (InvocationTargetException e) {
            DebugUtils.logException("packet.multi_batch_send_error", e, packets.size(), players.size());
        }
    }
    
    /**
     * 向视距内玩家发送数据包
     * @param location 位置
     * @param packet 数据包
     */
    public static void sendPacketToNearbyPlayers(Location location, PacketContainer packet) {
        Collection<Player> nearbyPlayers = OptimizationUtils.getNearbyPlayers(location, 
                FateServants.getInstance().getServer().getViewDistance() * 16.0);
        
        if (!nearbyPlayers.isEmpty()) {
            sendPacketBatch(nearbyPlayers, packet);
        }
    }
    
    /**
     * 向实体视距内玩家发送数据包
     * @param entity 实体
     * @param packet 数据包
     */
    public static void sendPacketToNearbyPlayers(Entity entity, PacketContainer packet) {
        Collection<Player> nearbyPlayers = entity.getWorld().getPlayers().stream()
                .filter(player -> OptimizationUtils.isEntityInPlayerViewDistance(entity, player))
                .collect(Collectors.toList());
        
        if (!nearbyPlayers.isEmpty()) {
            sendPacketBatch(nearbyPlayers, packet);
        }
    }
    
    /**
     * 创建实体相关数据包并发送给附近玩家
     * @param entity 实体
     * @param packetType 数据包类型
     * @param packetCreator 数据包创建函数
     */
    public static void createAndSendEntityPacket(Entity entity, PacketType packetType, 
            Function<Entity, PacketContainer> packetCreator) {
        Collection<Player> nearbyPlayers = entity.getWorld().getPlayers().stream()
                .filter(player -> OptimizationUtils.isEntityInPlayerViewDistance(entity, player))
                .collect(Collectors.toList());
        
        if (!nearbyPlayers.isEmpty()) {
            PacketContainer packet = packetCreator.apply(entity);
            sendPacketBatch(nearbyPlayers, packet);
        }
    }
    
    /**
     * 创建多个数据包并批量发送
     * @param players 玩家列表
     * @param count 数据包数量
     * @param packetCreator 数据包创建函数（接收索引参数）
     */
    public static void createAndSendPacketsBatch(Collection<Player> players, int count, 
            Function<Integer, PacketContainer> packetCreator) {
        if (players.isEmpty() || count <= 0) {
            return;
        }
        
        List<PacketContainer> packets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            packets.add(packetCreator.apply(i));
        }
        
        sendPacketsBatch(players, packets);
    }
} 