package cn.i7mc.fateservants.model;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.attributes.AttributeContainer;
import cn.i7mc.fateservants.attributes.ServantQuality;
import cn.i7mc.fateservants.attributes.provider.AttributePlusProvider;
import cn.i7mc.fateservants.ai.ServantAIController;
import cn.i7mc.fateservants.ai.ServantBehavior;
import cn.i7mc.fateservants.manager.SkinManager;
import cn.i7mc.fateservants.model.handlers.*;

import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.data.AttributeData;
import org.serverct.ersha.attribute.data.AttributeSource;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;
import cn.i7mc.fateservants.skills.Skill;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import cn.i7mc.fateservants.utils.DebugUtils;

public class Servant {
    private UUID uuid;
    private final Player owner;
    private final ServantClass servantClass;
    private final AttributeContainer attributes;
    private Map<String, Double> attributeGrowths;  // 移除final关键字
    private ServantQuality quality;
    private double level = 1;  // 初始等级为1
    private double currentHealth;
    private double maxHealth;
    private double currentMana;
    private double maxMana;
    private Location location;
    private Location lastOwnerLocation;  // 记录主人上一次的位置
    private final Map<String, Integer> hologramEntityIds;
    private final ServantAIController aiController;  // 添加AI控制器
    private Monster target = null;  // 修改为 Monster 类型
    private static final double HEAD_TURN_SPEED = 2.0; // 头部转动速度
    // 获取当前经验值
    private int experience = 0;

    private Location patrolPoint = null;
    private int attackCooldown = 0;  // 技能冷却计时器
    private int normalAttackCooldown = 0;  // 普通攻击冷却计时器

    private final List<Skill> skills;
    
    // 处理器实例
    private ServantAttributeHandler attributeHandler;
    private ServantHologramHandler hologramHandler;
    private ServantCombatHandler combatHandler;
    private ServantMovementHandler movementHandler;
    private ServantAIHandler aiHandler;

    // 添加时间戳字段用于限制更新频率
    private long lastHeadUpdateTime = 0;

    private long summonTime;
    
    public Servant(Player owner, ServantClass servantClass) {
        this.uuid = UUID.randomUUID();
        this.owner = owner;
        this.servantClass = servantClass;
        this.attributes = new AttributeContainer(servantClass.getAttributes());
        this.attributeGrowths = new HashMap<>();
        this.hologramEntityIds = new HashMap<>();
        this.aiController = new ServantAIController(this);  // 初始化AI控制器
        
        // 根据职阶配置的品质范围随机生成品质
        String minQuality = servantClass.getQualityRangeMin();
        String maxQuality = servantClass.getQualityRangeMax();
        this.quality = ServantQuality.fromName(minQuality);  // 默认使用最低品质
        
        // 初始化属性成长资质
        for (String attributeName : servantClass.getAttributes().keySet()) {
            String growthRange = servantClass.getAttributeGrowthRange(attributeName);
            if (growthRange != null) {
                double growth = ServantClass.generateRandomGrowth(growthRange);
                attributeGrowths.put(attributeName, growth);
            }
        }
        
        // 计算初始最大生命值和魔法值
        this.maxHealth = calculateMaxHealth();
        this.maxMana = calculateMaxMana();
        this.currentHealth = this.maxHealth;
        this.currentMana = this.maxMana;
        
        // 记录主人初始位置
        this.lastOwnerLocation = owner.getLocation().clone();
        
        // 设置英灵初始位置为主人背后指定距离
        double followDistance = FateServants.getInstance().getConfig().getDouble("servants.follow_distance", 3.0);
        Location ownerLoc = owner.getLocation().clone();
        Vector direction = ownerLoc.getDirection().normalize();
        this.location = ownerLoc.subtract(direction.multiply(followDistance));
        
        // 初始化技能列表
        String classId = servantClass.getId();
        DebugUtils.debug("servant.init", 
            classId,
            servantClass.getDisplayName(),
            quality.getDisplayName());
            
        List<Skill> loadedSkills = FateServants.getInstance().getSkillManager().getClassSkills(classId);
        this.skills = loadedSkills != null ? loadedSkills : new ArrayList<>();
        
        DebugUtils.debug("servant.init_skills", 
            classId,
            (skills != null ? skills.size() : "null"),
            (skills != null ? skills.toString() : "null"),
            String.join(", ", FateServants.getInstance().getSkillManager().getLoadedClasses()));
        
        if (skills.isEmpty()) {
            DebugUtils.debug("servant.no_skills", classId);
        }
        
        // 初始化处理器
        initializeHandlers();
        
        spawn();
    }
    
    /**
     * 初始化处理器
     */
    private void initializeHandlers() {
        this.attributeHandler = new ServantAttributeHandler(this, attributes, attributeGrowths);
        this.hologramHandler = new ServantHologramHandler(this);
        this.combatHandler = new ServantCombatHandler(this);
        this.movementHandler = new ServantMovementHandler(this);
        this.aiHandler = new ServantAIHandler(this, combatHandler, movementHandler);
    }

    private void startFollowTask() {
        FateServants.getInstance().getServer().getScheduler().runTaskTimer(
            FateServants.getInstance(),
            () -> {
                if (owner == null || !owner.isOnline()) return;
                
                // 添加调试日志，确认跟随任务正在执行
                DebugUtils.debug("servant.follow_task", 
                    owner != null ? owner.getName() : "null",
                    servantClass != null ? servantClass.getId() : "null");
                
                Location currentOwnerLoc = owner.getLocation();
                double followDistance = FateServants.getInstance().getConfig().getDouble("servants.follow_distance", 3.0);
                
                // 检查主人位置是否发生显著变化（添加阈值）
                if (lastOwnerLocation != null && 
                    Math.abs(lastOwnerLocation.getX() - currentOwnerLoc.getX()) < 0.1 &&
                    Math.abs(lastOwnerLocation.getY() - currentOwnerLoc.getY()) < 0.1 &&
                    Math.abs(lastOwnerLocation.getZ() - currentOwnerLoc.getZ()) < 0.1) {
                    // 位置变化很小，跳过更新
                    DebugUtils.debug("servant.follow_skip", 
                        owner.getName(), 
                        String.format("%.2f,%.2f,%.2f", currentOwnerLoc.getX(), currentOwnerLoc.getY(), currentOwnerLoc.getZ()));
                    return;
                }
                
                // 添加调试日志，记录位置更新
                DebugUtils.debug("servant.follow_update", 
                    owner.getName(), 
                    String.format("%.2f,%.2f,%.2f -> %.2f,%.2f,%.2f", 
                        lastOwnerLocation != null ? lastOwnerLocation.getX() : 0, 
                        lastOwnerLocation != null ? lastOwnerLocation.getY() : 0, 
                        lastOwnerLocation != null ? lastOwnerLocation.getZ() : 0,
                        currentOwnerLoc.getX(), 
                        currentOwnerLoc.getY(), 
                        currentOwnerLoc.getZ()));
                
                // 更新上一次位置记录
                lastOwnerLocation = currentOwnerLoc.clone();
                
                // 检查是否超出最大跟随距离
                double maxFollowDistance = FateServants.getInstance().getConfig().getDouble("servants.max_follow_distance", 16);
                if (currentOwnerLoc.distance(location) > maxFollowDistance) {
                    // 记录当前行为模式
                    ServantBehavior previousBehavior = aiController.getCurrentBehavior();
                    
                    // 临时切换到跟随模式并传送
                    aiController.setBehavior(ServantBehavior.FOLLOW);
                    Location targetLoc = currentOwnerLoc.clone();
                    teleport(targetLoc);
                    
                    // 5tick后恢复之前的模式
                    FateServants.getInstance().getServer().getScheduler().runTaskLater(
                        FateServants.getInstance(),
                        () -> aiController.setBehavior(previousBehavior),
                        5L
                    );
                    return;
                }
                
                // 计算主人背后指定距离的目标位置
                Vector direction = currentOwnerLoc.getDirection().normalize();
                Location targetLoc = currentOwnerLoc.clone().subtract(direction.multiply(followDistance));
                
                // 获取目标位置的地面高度
                targetLoc.setY(targetLoc.getWorld().getHighestBlockYAt(targetLoc));
                
                // 计算英灵到目标位置的距离
                double distance = location.distance(targetLoc);
                
                if (distance > 0.5) {
                    // 如果距离大于0.5格，使用自然移动
                    Vector moveDirection = targetLoc.toVector().subtract(location.toVector()).normalize();
                    Location newLoc = location.clone().add(moveDirection.multiply(getMovementSpeed()));
                    
                    // 获取新位置的地面高度
                    newLoc.setY(newLoc.getWorld().getHighestBlockYAt(newLoc));
                    
                    teleport(newLoc);
                }
            },
            10L, // 延迟10tick开始执行
            5L   // 每5tick执行一次
        );
        
        // 添加日志记录跟随任务启动
        DebugUtils.debug("servant.follow_task_started", 
            owner != null ? owner.getName() : "null", 
            servantClass != null ? servantClass.getId() : "null");
    }

    private void updateHeadRotation(Location currentLoc) {
        // 如果有目标,朝向目标
        if (target != null) {
            return;
        }
        
        // 添加更新间隔检查
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHeadUpdateTime < 100) { // 限制更新间隔为100ms
            return;
        }
        lastHeadUpdateTime = currentTime;
        
        // 寻找最近的实体
        double closestDistance = Double.MAX_VALUE;
        Location targetLocation = null;
        
        // 获取附近16格范围内的所有实体
        for (org.bukkit.entity.Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 16, 16, 16)) {
            if (isHologramEntity(entity.getEntityId()) || entity.equals(owner)) {
                continue;
            }
            
            if (!(entity instanceof org.bukkit.entity.LivingEntity)) {
                continue;
            }
            
            double distance = entity.getLocation().distance(currentLoc);
            if (distance < closestDistance) {
                closestDistance = distance;
                targetLocation = entity.getLocation();
            }
        }
        
        if (targetLocation != null) {
            Vector direction = targetLocation.toVector().subtract(currentLoc.toVector());
            
            double dx = direction.getX();
            double dz = direction.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            double dy = direction.getY();
            float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDistance));
            
            float currentYaw = currentLoc.getYaw();
            float currentPitch = currentLoc.getPitch();
            
            float yawDiff = yaw - currentYaw;
            while (yawDiff > 180) yawDiff -= 360;
            while (yawDiff < -180) yawDiff += 360;
            
            float pitchDiff = pitch - currentPitch;
            
            // 只有当角度变化超过阈值时才更新
            if (Math.abs(yawDiff) > 5 || Math.abs(pitchDiff) > 5) {
                currentYaw += yawDiff * HEAD_TURN_SPEED * 0.1f;
                currentPitch += pitchDiff * HEAD_TURN_SPEED * 0.1f;
                
                currentPitch = Math.max(-30, Math.min(30, currentPitch));
                
                // 合并数据包发送
                List<PacketContainer> packets = new ArrayList<>();
                
                PacketContainer entityLookPacket = new PacketContainer(PacketType.Play.Server.ENTITY_LOOK);
                entityLookPacket.getIntegers().write(0, hologramEntityIds.get("player"));
                entityLookPacket.getBytes()
                    .write(0, (byte) (currentYaw * 256.0F / 360.0F))
                    .write(1, (byte) (currentPitch * 256.0F / 360.0F));
                entityLookPacket.getBooleans().write(0, true);
                packets.add(entityLookPacket);
                
                PacketContainer headRotationPacket = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                headRotationPacket.getIntegers().write(0, hologramEntityIds.get("player"));
                headRotationPacket.getBytes().write(0, (byte) (currentYaw * 256.0F / 360.0F));
                packets.add(headRotationPacket);
                
                // 批量发送数据包
                packets.forEach(this::broadcastPacketToNearbyPlayers);
                
                currentLoc.setYaw(currentYaw);
                currentLoc.setPitch(currentPitch);
            }
        }
    }

    private void broadcastPacketToNearbyPlayers(PacketContainer packet) {
        final int visibilityRange = FateServants.getInstance().getConfig().getInt("performance.servant_visibility_range", 32);
        final int squaredRange = visibilityRange * visibilityRange;
        
        // 获取范围内的所有玩家，包括主人
        location.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distanceSquared(location) <= squaredRange) {
                try {
                    // 添加延迟发送机制
                    FateServants.getInstance().getServer().getScheduler().runTaskLater(
                        FateServants.getInstance(),
                        () -> {
                            try {
                                FateServants.getInstance().getProtocolManager().sendServerPacket(player, packet);
                            } catch (Exception e) {
                                FateServants.getInstance().getLogger().severe("发送数据包时发生错误: " + e.getMessage());
                            }
                        },
                        1L
                    );
                } catch (Exception e) {
                    FateServants.getInstance().getLogger().severe("发送数据包时发生错误: " + e.getMessage());
                }
            }
        });
    }

    public void spawn() {
        FateServants.getInstance().getMessageConfig();
        
        // 创建玩家实体
        spawnPlayerEntity();
        
        // 初始化全息图显示
        updateHolograms();
        
        // 启动跟随任务和AI处理
        startFollowTask();
        
        // 确保AI控制器设置为跟随模式
        if (aiController != null) {
            aiController.setBehavior(ServantBehavior.FOLLOW);
        }
        
        // 启动英灵AI处理任务
        FateServants.getInstance().getServer().getScheduler().runTaskTimer(
            FateServants.getInstance(),
            this::updateAI,
            20L, // 1秒后开始
            5L   // 每5tick (0.25秒) 更新一次AI
        );
        
        DebugUtils.debug("servant.spawn_complete", 
            owner != null ? owner.getName() : "null", 
            servantClass != null ? servantClass.getId() : "null");
    }

    private void spawnPlayerEntity() {
        try {
            int entityId = FateServants.getInstance().getPacketHandler().getNextEntityId();
            hologramEntityIds.put("player", entityId);
            
            UUID playerUUID = UUID.randomUUID();
            String skinName = servantClass.getSkinName();
            
            // 从message.yml读取display配置
            FileConfiguration config = FateServants.getInstance().getMessageConfig();
            boolean displayEnabled = config.getBoolean("messages.display.enabled", true);
            String displayFormat = config.getString("messages.display.format", "{quality} {class_name}");
            
            // 替换变量获取显示名称
            String displayName = displayEnabled ? displayFormat
                .replace("{class_name}", servantClass.getDisplayName())
                .replace("{quality}", quality.getDisplayName())
                .replace("{level}", String.valueOf(level)) : "";
            
            // 创建GameProfile - 使用自定义displayName
            WrappedGameProfile profile = new WrappedGameProfile(playerUUID, displayName);
            
            // 从SkinManager获取皮肤数据
            SkinManager.SkinData skinData = FateServants.getInstance().getSkinManager().getSkin(skinName);
            WrappedSignedProperty textures = new WrappedSignedProperty(
                "textures",
                skinData.getValue(),
                skinData.getSignature()
            );
            profile.getProperties().put("textures", textures);

            // 1. 发送PLAYER_INFO数据包
            PacketContainer infoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            infoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            
            PlayerInfoData playerInfoData = new PlayerInfoData(
                profile,
                20,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText("")
            );
            
            infoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));
            
            // 2. 发送NAMED_ENTITY_SPAWN数据包
            PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, playerUUID);
            
            spawnPacket.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());
            
            spawnPacket.getBytes()
                .write(0, (byte) (location.getYaw() * 256.0F / 360.0F))
                .write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

            // 3. 设置元数据
            List<WrappedWatchableObject> dataValues = new ArrayList<>();
            
            // 基础实体数据 - 保持实体可见
            dataValues.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)),
                (byte) 0
            ));
            
            // 自定义名称 - 设置为空字符串
            dataValues.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.get(String.class)),
                ""
            ));
            
            // 名称可见性 - 设置为false以隐藏名称标签
            dataValues.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)),
                false
            ));

            // 创建并发送元数据包
            PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            metadataPacket.getWatchableCollectionModifier().write(0, dataValues);

            spawnPacket.getDataWatcherModifier().write(0, new WrappedDataWatcher(dataValues));

            // 4. 按顺序发送数据包
            broadcastPacketToNearbyPlayers(infoPacket);
            broadcastPacketToNearbyPlayers(spawnPacket);
            broadcastPacketToNearbyPlayers(metadataPacket);  // 额外发送一个元数据包确保设置生效
            
            // 5. 延迟移除玩家信息
            FateServants.getInstance().getServer().getScheduler().runTaskLater(
                FateServants.getInstance(),
                () -> {
                    try {
                        PacketContainer removeInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
                        removeInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                        removeInfo.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));
                        broadcastPacketToNearbyPlayers(removeInfo);
                    } catch (Exception e) {
                        FateServants.getInstance().getLogger().severe("移除玩家信息时发生错误: " + e.getMessage());
                    }
                },
                20L
            );
            
        } catch (Exception e) {
            FateServants.getInstance().getLogger().severe("生成玩家实体时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createHologram(String id, String text, double heightOffset, double horizontalOffset, Player targetPlayer) {
        try {
            int entityId = FateServants.getInstance().getPacketHandler().getNextEntityId();
            hologramEntityIds.put(id, entityId);
            
            // 创建盔甲架实体
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
            packet.getIntegers().write(0, entityId);
            
            // 计算位置，加入水平偏移
            Location loc = location.clone();
            if (horizontalOffset != 0) {
                Vector direction = loc.getDirection().normalize();
                Vector right = direction.crossProduct(new Vector(0, 1, 0)).multiply(horizontalOffset);
                loc.add(right);
            }
            loc.add(0, heightOffset, 0);
            
            packet.getDoubles()
                .write(0, loc.getX())
                .write(1, loc.getY())
                .write(2, loc.getZ());
                
            packet.getIntegers()
                .write(1, 0)
                .write(2, 0)
                .write(3, 0)
                .write(4, 0)
                .write(5, 0)
                .write(6, 78);
                
            packet.getUUIDs().write(0, UUID.randomUUID());
            
            // 设置盔甲架属性
            PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);
            
            List<WrappedWatchableObject> metadata = new ArrayList<>();
            
            metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)),
                (byte) (0x20 | 0x40)
            ));
            
            metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.get(String.class)),
                text
            ));
            
            metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)),
                true
            ));
            
            metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(11, WrappedDataWatcher.Registry.get(Byte.class)),
                (byte) (0x01 | 0x08 | 0x10)
            ));
            
            metadataPacket.getWatchableCollectionModifier().write(0, metadata);
            
            // 发送数据包
            if (targetPlayer != null) {
                // 发送给特定玩家
                FateServants.getInstance().getProtocolManager().sendServerPacket(targetPlayer, packet);
                FateServants.getInstance().getProtocolManager().sendServerPacket(targetPlayer, metadataPacket);
            } else {
                // 广播给所有范围内的玩家
                broadcastPacketToNearbyPlayers(packet);
                broadcastPacketToNearbyPlayers(metadataPacket);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 为了保持向后兼容,添加一个无targetPlayer参数的重载方法
    private void createHologram(String id, String text, double heightOffset, double horizontalOffset) {
        createHologram(id, text, heightOffset, horizontalOffset, null);
    }

    public void updateHologramText(String id, String text) {
        Integer entityId = hologramEntityIds.get(id);
        if (entityId == null) return;

        PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);
        
        List<WrappedWatchableObject> dataValues = new ArrayList<>();
        dataValues.add(new WrappedWatchableObject(
            new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.get(String.class)),
            text
        ));
        
        metadataPacket.getWatchableCollectionModifier().write(0, dataValues);
        broadcastPacketToNearbyPlayers(metadataPacket);
    }

    public void updateHealth(double newHealth) {
        this.currentHealth = Math.min(Math.max(0, newHealth), maxHealth);
        // 更新所有全息图，因为它们可能包含血量相关的变量
        updateHolograms();
    }

    public void updateHolograms() {
        // 重新获取最新的配置
        FileConfiguration config = FateServants.getInstance().getMessageConfig();
        
        // 更新玩家实体的显示名称
        boolean displayEnabled = config.getBoolean("messages.display.enabled", true);
        String displayFormat = config.getString("messages.display.format", "{quality} {class_name}");
        String displayName = displayEnabled ? displayFormat
            .replace("{class_name}", servantClass.getDisplayName())
            .replace("{quality}", quality.getDisplayName())
            .replace("{level}", String.valueOf(level)) : "";
            
        // 收集所有需要发送的数据包
        List<PacketContainer> packets = new ArrayList<>();
            
        // 更新玩家实体的显示名称
        Integer playerEntityId = hologramEntityIds.get("player");
        if (playerEntityId != null) {
            PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, playerEntityId);
            
            List<WrappedWatchableObject> metadata = new ArrayList<>();
            metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.get(String.class)),
                displayName
            ));
            metadata.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)),
                displayEnabled
            ));
            
            metadataPacket.getWatchableCollectionModifier().write(0, metadata);
            packets.add(metadataPacket);
        }
        
        // 获取所有启用的消息配置
        List<Map<String, Object>> enabledMessages = new ArrayList<>();
        
        // 获取高度设置
        double baseHeight = config.getDouble("messages.height.base", 2.0);
        double heightInterval = config.getDouble("messages.height.interval", 0.25);
        
        // 收集所有启用的消息
        for (int i = 1; i <= 4; i++) {
            String msgKey = "msg" + i;
            if (config.getBoolean("messages." + msgKey + ".enabled", false)) {
                Map<String, Object> msgConfig = new HashMap<>();
                msgConfig.put("id", msgKey);
                msgConfig.put("format", config.getString("messages." + msgKey + ".format", ""));
                enabledMessages.add(msgConfig);
            }
        }
        
        // 为每个启用的消息分配高度
        for (int i = 0; i < enabledMessages.size(); i++) {
            Map<String, Object> msgConfig = enabledMessages.get(i);
            msgConfig.put("height", baseHeight + (heightInterval * i));
        }
        
        // 更新或创建全息图
        for (Map<String, Object> msgConfig : enabledMessages) {
            String msgId = (String) msgConfig.get("id");
            String format = (String) msgConfig.get("format");
            double height = (Double) msgConfig.get("height");
            
            // 替换变量
            String text = replaceVariables(format);
            
            // 如果已存在此ID的全息图，更新它
            if (hologramEntityIds.containsKey(msgId)) {
                PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
                metadataPacket.getIntegers().write(0, hologramEntityIds.get(msgId));
                
                List<WrappedWatchableObject> dataValues = new ArrayList<>();
                dataValues.add(new WrappedWatchableObject(
                    new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.get(String.class)),
                    text
                ));
                
                metadataPacket.getWatchableCollectionModifier().write(0, dataValues);
                packets.add(metadataPacket);
                
                // 更新位置
                Location hologramLoc = location.clone().add(0, height, 0);
                PacketContainer teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
                teleportPacket.getIntegers().write(0, hologramEntityIds.get(msgId));
                teleportPacket.getDoubles()
                    .write(0, hologramLoc.getX())
                    .write(1, hologramLoc.getY())
                    .write(2, hologramLoc.getZ());
                packets.add(teleportPacket);
            } else {
                // 否则创建新的全息图
                createHologram(msgId, text, height, 0);
            }
        }
        
        // 移除已禁用的消息的全息图
        List<String> activeIds = enabledMessages.stream()
            .map(m -> (String)m.get("id"))
            .collect(java.util.stream.Collectors.toList());
            
        List<Integer> entitiesToRemove = new ArrayList<>();
        new ArrayList<>(hologramEntityIds.keySet()).forEach(id -> {
            if (!id.equals("player") && !activeIds.contains(id)) {
                entitiesToRemove.add(hologramEntityIds.get(id));
                hologramEntityIds.remove(id);
            }
        });
        
        if (!entitiesToRemove.isEmpty()) {
            PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntegerArrays().write(0, entitiesToRemove.stream().mapToInt(Integer::intValue).toArray());
            packets.add(destroyPacket);
        }
        
        // 批量发送所有数据包
        FateServants.getInstance().getServer().getScheduler().runTaskLater(
            FateServants.getInstance(),
            () -> packets.forEach(this::broadcastPacketToNearbyPlayers),
            1L
        );
    }
    
    private String replaceVariables(String format) {
        // 替换所有可用变量
        return format.replace("{class_name}", servantClass.getDisplayName())
                    .replace("{level}", String.valueOf(level))
                    .replace("{quality}", quality.getDisplayName())
                    .replace("{owner}", owner.getName())
                    .replace("{health}", String.format("%.1f", currentHealth))
                    .replace("{max_health}", String.format("%.1f", maxHealth))
                    .replace("{health_percentage}", String.format("%.1f", (currentHealth / maxHealth) * 100))
                    .replace("{health_bar}", generateHealthBar())
                    .replace("{mana}", String.format("%.1f", currentMana))
                    .replace("{max_mana}", String.format("%.1f", maxMana))
                    .replace("{mana_percentage}", String.format("%.1f", (currentMana / maxMana) * 100));
    }
    
    private String generateHealthBar() {
        FileConfiguration config = FateServants.getInstance().getMessageConfig();
        double healthPercentage = currentHealth / maxHealth;
        
        String filledColor = config.getString("health_bar.filled_color", "§4");
        String emptyColor = config.getString("health_bar.empty_color", "§8");
        String heartSymbol = config.getString("health_bar.heart_symbol", "❤");
        int totalHearts = config.getInt("health_bar.total_hearts", 10);
        
        StringBuilder bar = new StringBuilder();
        int filledHearts = (int) Math.ceil(healthPercentage * totalHearts);
        
        // 添加已有血量的心形
        bar.append(filledColor);
        for (int i = 0; i < filledHearts; i++) {
            bar.append(heartSymbol);
        }
        
        // 添加损失血量的心形
        if (filledHearts < totalHearts) {
            bar.append(emptyColor);
            for (int i = filledHearts; i < totalHearts; i++) {
                bar.append(heartSymbol);
            }
        }
        
        return bar.toString();
    }

    public void teleport(Location newLocation) {
        // 保存新位置
        this.location = newLocation.clone();
        
        // 收集所有需要发送的数据包
        List<PacketContainer> packets = new ArrayList<>();
        
        // 传送玩家实体
        Integer playerEntityId = hologramEntityIds.get("player");
        if (playerEntityId != null) {
            PacketContainer teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
            teleportPacket.getIntegers().write(0, playerEntityId);
            teleportPacket.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());
            teleportPacket.getBytes()
                .write(0, (byte) (location.getYaw() * 256.0F / 360.0F))
                .write(1, (byte) (location.getPitch() * 256.0F / 360.0F));
            
            packets.add(teleportPacket);
        }
        
        // 获取当前启用的消息配置并按顺序排列
        FileConfiguration config = FateServants.getInstance().getMessageConfig();
        
        // 获取高度设置
        double baseHeight = config.getDouble("messages.height.base", 2.0);
        double heightInterval = config.getDouble("messages.height.interval", 0.25);
        
        // 为每个全息图创建传送数据包
        int index = 0;
        for (Map.Entry<String, Integer> entry : hologramEntityIds.entrySet()) {
            if (!entry.getKey().equals("player")) {
                Location hologramLoc = location.clone().add(0, baseHeight + (heightInterval * index), 0);
                
                PacketContainer teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
                teleportPacket.getIntegers().write(0, entry.getValue());
                teleportPacket.getDoubles()
                    .write(0, hologramLoc.getX())
                    .write(1, hologramLoc.getY())
                    .write(2, hologramLoc.getZ());
                    
                packets.add(teleportPacket);
                index++;
            }
        }
        
        // 批量发送所有数据包
        FateServants.getInstance().getServer().getScheduler().runTaskLater(
            FateServants.getInstance(),
            () -> packets.forEach(this::broadcastPacketToNearbyPlayers),
            1L
        );
    }

    public void remove() {
        try {
            // 1. 停止所有任务
            FateServants.getInstance().getServer().getScheduler().cancelTasks(FateServants.getInstance());
            
            // 2. 移除所有实体
            PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntegerArrays().write(0, hologramEntityIds.values().stream().mapToInt(Integer::intValue).toArray());
            
            // 获取范围内的所有玩家
            final int visibilityRange = FateServants.getInstance().getConfig().getInt("performance.servant_visibility_range", 32);
            final int squaredRange = visibilityRange * visibilityRange;
            
            location.getWorld().getPlayers().forEach(player -> {
                if (player.getLocation().distanceSquared(location) <= squaredRange) {
                    try {
                        // 3. 移除玩家信息
                        PacketContainer removeInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
                        removeInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                        FateServants.getInstance().getProtocolManager().sendServerPacket(player, removeInfo);
                        
                        // 4. 发送销毁实体数据包
                        FateServants.getInstance().getProtocolManager().sendServerPacket(player, destroyPacket);
                    } catch (Exception e) {
                        FateServants.getInstance().getLogger().severe("移除英灵数据包发送失败: " + e.getMessage());
                    }
                }
            });
            
            // 5. 清除属性 - 安全处理AttributeAPI依赖
            if (owner != null && owner.isOnline()) {
                try {
                    // 检查AttributeAPI类是否存在
                    Class.forName("org.serverct.ersha.api.AttributeAPI");
                    
                    // AttributeAPI可用，执行属性清理
                    org.serverct.ersha.api.AttributeAPI.getAttrData(owner).clearApiAttribute();
                    org.serverct.ersha.api.AttributeAPI.getAttrData(owner).updateAttribute(true);
                    org.serverct.ersha.api.AttributeAPI.updateAttribute(owner);
                } catch (ClassNotFoundException e) {
                    // AttributeAPI不可用，记录日志
                    FateServants.getInstance().getLogger().warning("AttributeAPI未加载，跳过属性清理");
                } catch (Exception e) {
                    FateServants.getInstance().getLogger().warning("清除主人属性时发生错误: " + e.getMessage());
                }
            }
            
            // 6. 清除内存数据
            hologramEntityIds.clear();
            attributeGrowths.clear();
            skills.clear();
            
            // 7. 移除AI控制器
            if (aiController != null) {
                aiController.setBehavior(ServantBehavior.IDLE);
            }
            
            // 8. 清除目标
            target = null;
            
            FateServants.getInstance().getLogger().info("英灵 " + servantClass.getDisplayName() + " 已被完全移除");
            
        } catch (Exception e) {
            FateServants.getInstance().getLogger().severe("移除英灵时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setHealth(double health) {
        this.currentHealth = Math.min(Math.max(0, health), maxHealth);
        // 更新所有全息图，因为它们可能包含血量相关的变量
        updateHolograms();
    }

    /**
     * 设置当前生命值
     * @param newHealth 新的生命值
     */
    public void setCurrentHealth(double newHealth) {
        this.currentHealth = Math.max(0, Math.min(newHealth, maxHealth));
        updateHolograms();
    }

    public void setCurrentMana(double currentMana) {
        this.currentMana = Math.min(currentMana, maxMana);
        updateHolograms();
    }

    public void setMaxMana(double maxMana) {
        this.maxMana = maxMana;
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }
        updateHolograms();
    }

    public void setTarget(Monster target) {
        this.target = target;
        updateHologramText("target", target == null ? "" : "目标: " + (target.isDead() ? "已死亡" : target.getName()));
    }

    public ServantQuality getQuality() {
        return quality;
    }
    
    public void setQuality(ServantQuality quality) {
        // 修改为使用枚举类型
        this.quality = quality;
        // 更新显示
        updateHolograms();
    }
    
    // Getters
    public UUID getUuid() { return uuid; }
    public Player getOwner() { return owner; }
    public ServantClass getServantClass() { return servantClass; }
    public AttributeContainer getAttributes() { return attributes; }
    public double getCurrentHealth() { return currentHealth; }
    public double getCurrentMana() { return currentMana; }
    public Location getLocation() { return location; }
    public Monster getTarget() { return target; }
    public double getMaxHealth() { return maxHealth; }
    public double getMaxMana() { return maxMana; }
    public double getMovementSpeed() { 
        // 优先从属性容器中获取移动速度，如果不存在则从职阶配置中获取
        if (attributes.hasAttribute("movement_speed")) {
            return attributes.getValue("movement_speed");
        }
        // 回退到职阶配置中的值
        return servantClass.getBaseMovementSpeed();
    }
    
    /**
     * 检查给定的实体ID是否属于该英灵的全息图实体
     */
    public boolean isHologramEntity(int entityId) {
        return hologramEntityIds.containsValue(entityId);
    }

    /**
     * 获取属性的成长资质
     * @param attributeName 属性名称
     * @return 属性的成长资质值
     */
    public double getAttributeGrowth(String attributeName) {
        return attributeGrowths.getOrDefault(attributeName, 0.0);
    }

    /**
     * 获取所有属性的成长资质
     * @return 属性成长资质的Map
     */
    public Map<String, Double> getAttributeGrowths() {
        return new HashMap<>(attributeGrowths);
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    private void updateHologramPosition(String id, double height) {
        Integer entityId = hologramEntityIds.get(id);
        if (entityId == null) return;
        
        Location hologramLoc = location.clone().add(0, height, 0);
        
        PacketContainer teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        teleportPacket.getIntegers().write(0, entityId);
        teleportPacket.getDoubles()
            .write(0, hologramLoc.getX())
            .write(1, hologramLoc.getY())
            .write(2, hologramLoc.getZ());
            
        broadcastPacketToNearbyPlayers(teleportPacket);
    }
    
    private void removeHologram(String id) {
        Integer entityId = hologramEntityIds.remove(id);
        if (entityId != null) {
            PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntegerArrays().write(0, new int[]{entityId});
            broadcastPacketToNearbyPlayers(destroyPacket);
        }
    }

    public boolean isFollowing() {
        return aiController.getCurrentBehavior() == ServantBehavior.FOLLOW;
    }

    public void setFollowing(boolean following) {
        if (following) {
            aiController.setBehavior(ServantBehavior.FOLLOW);
        } else if (aiController.getCurrentBehavior() == ServantBehavior.FOLLOW) {
            aiController.setBehavior(ServantBehavior.IDLE);
        }
    }

    public boolean isAttacking() {
        return aiController.getCurrentBehavior() == ServantBehavior.COMBAT;
    }

    public void setAttacking(boolean attacking) {
        if (attacking) {
            aiController.setBehavior(ServantBehavior.COMBAT);
        } else if (aiController.getCurrentBehavior() == ServantBehavior.COMBAT) {
            aiController.setBehavior(ServantBehavior.IDLE);
        }
    }

    public boolean isDefending() {
        return aiController.getCurrentBehavior() == ServantBehavior.DEFEND;
    }

    public void setDefending(boolean defending) {
        if (defending) {
            aiController.setBehavior(ServantBehavior.DEFEND);
        } else if (aiController.getCurrentBehavior() == ServantBehavior.DEFEND) {
            aiController.setBehavior(ServantBehavior.IDLE);
        }
    }

    // 添加属性恢复方法
    private void restoreOwnerAttributes() {
        Player owner = getOwner();
        if (owner == null) return;

        AttributeData ownerData = AttributeAPI.getAttrData(owner);
        if (ownerData == null) return;

        // 完全清除主人当前的所有属性源
        ownerData.clearApiAttribute();

        // 准备英灵属性
        FateServants.getInstance().getLogger().info("Preparing servant attributes for addition:");
        Map<String, Number[]> servantAttributes = new HashMap<>();
        AttributePlusProvider provider = FateServants.getInstance().getAttributePlusProvider();
        
        // 映射英灵属性到AttributePlus属性
        attributes.getAll().forEach((key, value) -> {
            String mappedAttr = provider.getMappedAttributeName(key);
            if (mappedAttr != null) {
                double attrValue = value;
                servantAttributes.put(mappedAttr, new Number[]{attrValue, 0.0});
                FateServants.getInstance().getLogger().info(String.format(
                    "Setting attribute %s to %f", mappedAttr, attrValue
                ));
            }
        });

        // 创建属性源
        FateServants.getInstance().getLogger().info("Creating AttributeSource...");
        AttributeSource servantSource = AttributeAPI.createStaticAttributeSource(
            new HashMap<>(servantAttributes), 
            new HashMap<>()
        );

        // 添加英灵属性到主人
        FateServants.getInstance().getLogger().info("Adding attributes to owner...");
        ownerData.operationApiAttribute("servant_attributes", servantSource, 
            AttributeSource.OperationType.ADD, true);

        // 立即更新属性
        ownerData.updateAttribute(true);
        AttributeAPI.updateAttribute(owner);
    }

    public Location getPatrolPoint() {
        return patrolPoint;
    }

    public void setPatrolPoint(Location patrolPoint) {
        this.patrolPoint = patrolPoint;
        setFollowing(false);
        setAttacking(false);
        setDefending(false);
    }

    /**
     * 获取英灵等级
     * @return 英灵等级
     */
    public double getLevel() {
        return level;
    }
    
    public void setLevel(double level) {
        this.level = level;
        // 更新属性
        this.updateAttributes();
    }
    
    /**
     * 获取升级所需经验值
     * @return 所需经验值
     */
    public int getRequiredExperience() {
        // 计算下一级所需经验，这里使用一个简单的公式：基础经验100，每级增加50
        return 100 + ((int)level - 1) * 50;
    }

    // 添加AI控制器相关方法
    public ServantAIController getAIController() {
        return aiController;
    }

    public void updateAI() {
        // 添加调试日志，确认AI更新正在执行
        DebugUtils.debug("servant.update_ai", 
            owner != null ? owner.getName() : "null", 
            servantClass != null ? servantClass.getId() : "null");
            
        // 减少技能冷却
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        // 减少普通攻击冷却
        if (normalAttackCooldown > 0) {
            normalAttackCooldown--;
            if (normalAttackCooldown == 0) {
                DebugUtils.debug("servant.attack_cooldown_end", 
                    servantClass != null ? servantClass.getDisplayName() : "null");
            }
        }
        
        // 确保AI控制器存在
        if (aiController == null) {
            DebugUtils.debug("servant.ai_controller_null",
                owner != null ? owner.getName() : "null",
                servantClass != null ? servantClass.getId() : "null");
            return;
        }
        
        // 更新AI行为
        aiController.tick();
        
        // 记录当前行为状态
        DebugUtils.debug("servant.ai_behavior",
            owner != null ? owner.getName() : "null",
            aiController.getCurrentBehavior().toString());
    }

    // AI相关方法
    public void attack(Monster targetMonster) {
        // 如果不是在攻击或防御状态，就返回
        if ((!isAttacking() && !isDefending()) || targetMonster == null || targetMonster.isDead() || !targetMonster.isValid()) {
            return;
        }

        // 检查普通攻击冷却
        if (normalAttackCooldown > 0) {
            FateServants.getInstance().getLogger().info(String.format(
                "[Servant Debug] %s 普通攻击冷却中: 剩余 %d ticks",
                servantClass.getDisplayName(),
                normalAttackCooldown
            ));
            return;
        }

        // 设置普通攻击冷却
        normalAttackCooldown = servantClass.getAttackSpeed();

        if (owner == null || !owner.isOnline()) {
            return;
        }

        try {
            // 获取主人的AttributePlus数据
            AttributeData ownerData = AttributeAPI.getAttrData(owner);
            if (ownerData == null) {
                // 如果无法获取属性数据，直接使用基础伤害
                double attackDamage = attributes.getValue("attack");
                FateServants.getInstance().getLogger().info(String.format(
                    "[Servant Debug] %s 使用基础伤害攻击: %.2f (AttributePlus数据不可用)",
                    servantClass.getDisplayName(),
                    attackDamage
                ));
                
                // 获取目标初始血量用于计算实际伤害
                double initialHealth = targetMonster.getHealth();
                
                // 造成伤害
                targetMonster.damage(attackDamage, owner);
                
                // 计算实际伤害并添加经验
                double finalHealth = targetMonster.getHealth();
                double actualDamage = initialHealth - finalHealth;
                if (actualDamage > 0) {
                    addExperience(actualDamage);
                }
                
                // 尝试触发被动技能
                tryTriggerPassiveSkills(targetMonster);
                return;
            }
            
            // 检查是否禁用属性叠加
            boolean disableStacking = FateServants.getInstance().getConfig().getBoolean("servants.disable_attribute_stacking", true);
            
            // 保存主人原有的属性源（如果需要的话）
            final Map<String, AttributeSource> originalSources = !disableStacking ? 
                new HashMap<>(ownerData.getApiSourceAttribute()) : null;
            
            try {
                // 如果禁用属性叠加,则清除主人当前所有属性
                if (disableStacking) {
                    // 先更新属性，确保所有属性都被计算
                    ownerData.updateAttribute(true);
                    // 然后清除API属性，这样装备属性就不会被重新计算
                    ownerData.clearApiAttribute();
                }

                // 创建英灵属性源
                Map<String, Number[]> servantAttributes = new HashMap<>();
                AttributePlusProvider provider = FateServants.getInstance().getAttributePlusProvider();
                
                // 映射英灵属性到AttributePlus属性
                attributes.getAll().forEach((key, value) -> {
                    String mappedAttr = provider.getMappedAttributeName(key);
                    if (mappedAttr != null) {
                        // 只设置基础值，增益值设置为0，避免重复计算
                        servantAttributes.put(mappedAttr, new Number[]{value, 0.0});
                    }
                });

                AttributeSource servantSource = AttributeAPI.createStaticAttributeSource(
                    new HashMap<>(servantAttributes), 
                    new HashMap<>()
                );
                ownerData.operationApiAttribute("servant_attributes", servantSource, 
                    AttributeSource.OperationType.ADD, true);

                // 获取目标初始血量用于计算实际伤害
                double initialHealth = targetMonster.getHealth();
                
                // 造成伤害
                targetMonster.damage(1, owner);
                
                // 清除属性并更新
                ownerData.clearApiAttribute();
                ownerData.updateAttribute(true);
                
                // 计算实际伤害并添加经验
                double finalHealth = targetMonster.getHealth();
                double actualDamage = initialHealth - finalHealth;
                if (actualDamage > 0) {
                    addExperience(actualDamage);
                }
                
                // 尝试触发被动技能
                tryTriggerPassiveSkills(targetMonster);
                
            } finally {
                // 只有在不禁用属性叠加时才恢复主人原有属性
                if (!disableStacking && originalSources != null) {
                    ownerData.clearApiAttribute();
                    originalSources.forEach((source, attr) -> {
                        ownerData.operationApiAttribute(source, attr, 
                            AttributeSource.OperationType.ADD, true);
                    });
                    ownerData.updateAttribute(true);
                    AttributeAPI.updateAttribute(owner);
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            // AttributePlus 未加载或出错，使用基础伤害
            double attackDamage = attributes.getValue("attack");
            FateServants.getInstance().getLogger().info(String.format(
                "[Servant Debug] %s 使用基础伤害攻击: %.2f (AttributePlus异常: %s)",
                servantClass.getDisplayName(),
                attackDamage,
                e.getMessage()
            ));
            
            // 获取目标初始血量用于计算实际伤害
            double initialHealth = targetMonster.getHealth();
            
            // 造成伤害
            targetMonster.damage(attackDamage, owner);
            
            // 计算实际伤害并添加经验
            double finalHealth = targetMonster.getHealth();
            double actualDamage = initialHealth - finalHealth;
            if (actualDamage > 0) {
                addExperience(actualDamage);
            }
            
            // 尝试触发被动技能
            tryTriggerPassiveSkills(targetMonster);
        }
    }
    
    public void setRotation(float yaw, float pitch) {
        // 创建头部旋转数据包
        PacketContainer lookPacket = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        lookPacket.getIntegers().write(0, getEntityId());
        lookPacket.getBytes().write(0, (byte)(yaw * 256.0F / 360.0F));
        
        // 创建身体旋转数据包
        PacketContainer rotationPacket = new PacketContainer(PacketType.Play.Server.ENTITY_LOOK);
        rotationPacket.getIntegers().write(0, getEntityId());
        rotationPacket.getBytes()
            .write(0, (byte)(yaw * 256.0F / 360.0F))
            .write(1, (byte)(pitch * 256.0F / 360.0F));
        rotationPacket.getBooleans().write(0, true);
        
        // 使用统一的数据包发送方法
        broadcastPacketToNearbyPlayers(lookPacket);
        broadcastPacketToNearbyPlayers(rotationPacket);
    }

    public int getEntityId() {
        return hologramEntityIds.get("player");
    }

    /**
     * 主动施放技能
     * @param skillIndex 技能索引
     * @param target 目标实体
     * @return 是否施放成功
     */
    public boolean castSkill(int skillIndex, LivingEntity target) {
        if (skillIndex < 0 || skillIndex >= skills.size()) {
            return false;
        }
        
        Skill skill = skills.get(skillIndex);
        return skill.cast(this, target);
    }

    /**
     * 尝试触发被动技能
     * @param target 目标实体
     */
    public void tryTriggerPassiveSkills(LivingEntity target) {
        for (Skill skill : skills) {
            skill.tryTrigger(this, target);
        }
    }

    /**
     * 获取技能列表
     */
    public List<Skill> getSkills() {
        return new ArrayList<>(skills);
    }

    /**
     * 检查并触发主动技能
     * @param target 目标实体
     */
    public void checkAndTriggerActiveSkills(LivingEntity target) {
        if (skills.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            if (skill.getCastType().equals("active")) {
                FateServants.getInstance().getLogger().info("[Servant Debug] 检查主动技能: " + skill.getName());
                if (skill.canCast(this)) {
                    FateServants.getInstance().getLogger().info("[Servant Debug] 尝试释放主动技能: " + skill.getName());
                    boolean success = castSkill(i, target);
                    FateServants.getInstance().getLogger().info("[Servant Debug] 技能释放" + (success ? "成功" : "失败"));
                }
            }
        }
    }

    public void addExperience(double damage) {
        // 从配置中获取经验获取公式
        String expFormula = FateServants.getInstance().getConfig().getString("level_system.exp_gain_formula", "{damage}/10");
        
        // 替换变量并计算经验值
        expFormula = expFormula.replace("{damage}", String.valueOf(damage));
        try {
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName("js");
            double exp = Double.parseDouble(engine.eval(expFormula).toString());
            
            // 添加经验值（使用Math.round进行四舍五入）
            int gainedExp = (int)Math.round(exp);
            this.experience += gainedExp;
            
            // 输出调试信息
            FateServants.getInstance().getLogger().info(String.format(
                "[Experience Debug] 经验获取:\n" +
                "造成伤害: %.2f\n" +
                "获得经验: %d\n" +
                "当前等级: %d\n" +
                "当前经验: %d",
                damage,
                gainedExp,
                level,
                experience
            ));
            
            // 检查是否可以升级
            checkLevelUp();
            
            // 更新显示
            updateHolograms();
        } catch (Exception e) {
            FateServants.getInstance().getLogger().warning("计算经验值时发生错误: " + e.getMessage());
        }
    }
    
    public void checkLevelUp() {
        // 获取配置
        FileConfiguration config = FateServants.getInstance().getConfig();
        int globalMaxLevel = config.getInt("level_system.max_level", -1);
        int classMaxLevel = servantClass.getMaxLevel();
        
        // 确定实际的最大等级限制
        int effectiveMaxLevel;
        if (globalMaxLevel == -1) {
            // 如果全局限制是-1（无限制），则使用职阶限制
            effectiveMaxLevel = classMaxLevel;
        } else if (classMaxLevel == 0) {
            // 如果职阶限制是0（无限制），则使用全局限制
            effectiveMaxLevel = globalMaxLevel;
        } else {
            // 两者都有限制时，使用较小的值
            effectiveMaxLevel = Math.min(globalMaxLevel, classMaxLevel);
        }
        
        // 输出等级限制调试信息
        FateServants.getInstance().getLogger().info(String.format(
            "[LevelUp Debug] 等级限制检查:\n" +
            "全局最大等级: %d\n" +
            "职阶最大等级: %d\n" +
            "实际最大等级: %d\n" +
            "当前等级: %d",
            globalMaxLevel,
            classMaxLevel,
            effectiveMaxLevel,
            level
        ));
        
        // 如果已达到最大等级，则不再升级
        if (effectiveMaxLevel > 0 && level >= effectiveMaxLevel) {
            FateServants.getInstance().getLogger().info("[LevelUp Debug] 已达到最大等级，停止升级检查");
            return;
        }
        
        // 获取升级经验公式
        String levelUpFormula = config.getString("level_system.level_up_formula", "100+({level}*240)");
        javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
        javax.script.ScriptEngine engine = manager.getEngineByName("js");
        
        // 计算所需经验
        int requiredExp;
        if (level == 1) {
            // 1级使用默认值
            requiredExp = config.getInt("level_system.default_required_exp", 100);
            FateServants.getInstance().getLogger().info(String.format(
                "[LevelUp Debug] 使用1级默认经验值: %d",
                requiredExp
            ));
        } else {
            // 2级及以上使用公式
            String formula = levelUpFormula.replace("{level}", String.valueOf(level));
            
            try {
                // 替换变量并计算
                requiredExp = (int)Math.round(Double.parseDouble(engine.eval(formula).toString()));
                
                // 输出调试信息到后台
                FateServants.getInstance().getLogger().info(String.format(
                    "[LevelUp Debug] 经验计算详细过程:\n" +
                    "1. 原始公式: %s\n" +
                    "2. 当前等级: %d\n" +
                    "3. 替换变量后的公式: %s\n" +
                    "4. 计算结果: %d",
                    levelUpFormula,
                    level,
                    formula,
                    requiredExp
                ));
            } catch (Exception e) {
                FateServants.getInstance().getLogger().warning("计算升级经验时发生错误: " + e.getMessage());
                e.printStackTrace();
                requiredExp = config.getInt("level_system.default_required_exp", 100); // 出错时使用默认值
            }
        }
        
        // 检查是否可以升级
        while (experience >= requiredExp && (effectiveMaxLevel <= 0 || level < effectiveMaxLevel)) {
            // 输出升级前状态
            FateServants.getInstance().getLogger().info(String.format(
                "[LevelUp Debug] 准备升级:\n" +
                "当前等级: %d\n" +
                "当前经验: %d\n" +
                "消耗经验: %d\n" +
                "剩余经验: %d",
                level,
                experience,
                requiredExp,
                experience - requiredExp
            ));
            
            // 升级
            level++;
            experience -= requiredExp;
            
            // 应用等级奖励
            applyLevelUpRewards();
            
            // 发送升级消息
            if (config.getBoolean("level_system.level_up_message", true)) {
                String maxLevelInfo = effectiveMaxLevel > 0 ? String.format(" (最大等级: %d)", effectiveMaxLevel) : "";
                owner.sendMessage("§a你的英灵升级了！当前等级: §6" + level + maxLevelInfo);
            }
            
            // 播放升级音效
            if (config.getBoolean("level_system.level_up_sound", true)) {
                owner.playSound(owner.getLocation(), "entity.player.levelup", 1.0f, 1.0f);
            }
            
            // 更新显示
            updateHolograms();
            
            // 重新计算下一级所需经验
            try {
                String newFormula = levelUpFormula.replace("{level}", String.valueOf(level));
                requiredExp = (int)Math.round(Double.parseDouble(engine.eval(newFormula).toString()));
                
                // 输出升级后状态
                FateServants.getInstance().getLogger().info(String.format(
                    "[LevelUp Debug] 升级完成:\n" +
                    "新等级: %d\n" +
                    "剩余经验: %d\n" +
                    "下一级所需经验: %d",
                    level,
                    experience,
                    requiredExp
                ));
            } catch (Exception e) {
                FateServants.getInstance().getLogger().warning("计算下一级经验时发生错误: " + e.getMessage());
                break; // 出错时退出循环
            }
        }
    }
    
    /**
     * 计算属性值
     */
    private double calculateAttributeValue(String attributeName, double baseValue) {
        // 获取品质乘数
        double qualityMultiplier = quality.getAttributeMultiplier();
        
        // 获取成长资质
        double growthRate = attributeGrowths.getOrDefault(attributeName, 1.0);
        
        // 计算等级系数 (每级增加5%)
        double levelMultiplier = 1.0 + ((level - 1) * 0.05);
        
        // 使用计算公式：基础值 * 品质系数 * 等级系数 * 成长资质
        double result = baseValue * qualityMultiplier * levelMultiplier * growthRate;
        
        return result;
    }

    private void applyLevelUpRewards() {
        // 计算新的最大生命值和魔法值
        double newMaxHealth = calculateMaxHealth();
        double newMaxMana = calculateMaxMana();
        
        // 按比例恢复生命值和魔法值
        double healthPercent = currentHealth / maxHealth;
        double manaPercent = currentMana / maxMana;
        
        // 更新最大值
        maxHealth = newMaxHealth;
        maxMana = newMaxMana;
        
        // 按比例设置当前值
        currentHealth = maxHealth * healthPercent;
        currentMana = maxMana * manaPercent;
        
        // 更新所有属性
        Map<String, Double> baseAttributes = servantClass.getAttributes();
        for (Map.Entry<String, Double> entry : baseAttributes.entrySet()) {
            String attributeName = entry.getKey();
            double baseValue = entry.getValue();
            
            // 计算新的属性值
            double newValue = calculateAttributeValue(attributeName, baseValue);
            attributes.setBaseValue(attributeName, newValue);
        }
        
        // 更新属性
        restoreOwnerAttributes();
    }

    /**
     * 计算最大生命值
     */
    private double calculateMaxHealth() {
        return calculateAttributeValue("max_health", servantClass.getBaseHealth());
    }

    /**
     * 计算最大魔法值
     */
    private double calculateMaxMana() {
        return calculateAttributeValue("max_mana", servantClass.getBaseMana());
    }

    /**
     * 为指定玩家发送英灵的生成数据包
     * @param player 目标玩家
     */
    public void spawnForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        
        // 检查玩家是否在可见范围内
        final int visibilityRange = FateServants.getInstance().getConfig().getInt("performance.servant_visibility_range", 32);
        final int squaredRange = visibilityRange * visibilityRange;
        
        if (player.getLocation().distanceSquared(location) > squaredRange) {
            return;
        }

        try {
            // 获取皮肤数据
            String skinName = servantClass.getSkinName();
            SkinManager.SkinData skinData = FateServants.getInstance().getSkinManager().getSkin(skinName);
            
            // 从message.yml读取display配置
            FileConfiguration config = FateServants.getInstance().getMessageConfig();
            boolean displayEnabled = config.getBoolean("messages.display.enabled", true);
            String displayFormat = config.getString("messages.display.format", "{quality} {class_name}");
            
            // 替换变量获取显示名称
            String displayName = displayEnabled ? displayFormat
                .replace("{class_name}", servantClass.getDisplayName())
                .replace("{quality}", quality.getDisplayName())
                .replace("{level}", String.valueOf(level)) : "";

            // 创建GameProfile
            WrappedGameProfile profile = new WrappedGameProfile(UUID.randomUUID(), displayName);
            WrappedSignedProperty textures = new WrappedSignedProperty(
                "textures",
                skinData.getValue(),
                skinData.getSignature()
            );
            profile.getProperties().put("textures", textures);

            // 1. 发送PLAYER_INFO数据包
            PacketContainer infoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            infoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            
            PlayerInfoData playerInfoData = new PlayerInfoData(
                profile,
                20,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText("")
            );
            
            infoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));
            
            // 2. 发送NAMED_ENTITY_SPAWN数据包
            PacketContainer spawnPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            spawnPacket.getIntegers().write(0, getEntityId());
            spawnPacket.getUUIDs().write(0, profile.getUUID());
            
            spawnPacket.getDoubles()
                .write(0, location.getX())
                .write(1, location.getY())
                .write(2, location.getZ());
            
            spawnPacket.getBytes()
                .write(0, (byte) (location.getYaw() * 256.0F / 360.0F))
                .write(1, (byte) (location.getPitch() * 256.0F / 360.0F));

            // 3. 设置元数据
            List<WrappedWatchableObject> dataValues = new ArrayList<>();
            
            dataValues.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)),
                (byte) 0
            ));
            
            dataValues.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.get(String.class)),
                ""
            ));
            
            dataValues.add(new WrappedWatchableObject(
                new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)),
                false
            ));

            PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, getEntityId());
            metadataPacket.getWatchableCollectionModifier().write(0, dataValues);

            spawnPacket.getDataWatcherModifier().write(0, new WrappedDataWatcher(dataValues));

            // 4. 发送数据包
            FateServants.getInstance().getProtocolManager().sendServerPacket(player, infoPacket);
            FateServants.getInstance().getProtocolManager().sendServerPacket(player, spawnPacket);
            FateServants.getInstance().getProtocolManager().sendServerPacket(player, metadataPacket);

            // 5. 为玩家生成全息图
            FileConfiguration messageConfig = FateServants.getInstance().getMessageConfig();
            double baseHeight = messageConfig.getDouble("messages.height.base", 2.0);
            double heightInterval = messageConfig.getDouble("messages.height.interval", 0.25);
            
            // 收集所有启用的消息
            for (int i = 1; i <= 4; i++) {
                String msgKey = "msg" + i;
                if (messageConfig.getBoolean("messages." + msgKey + ".enabled", false)) {
                    String format = messageConfig.getString("messages." + msgKey + ".format", "");
                    double height = baseHeight + (heightInterval * (i - 1));
                    
                    // 使用修改后的createHologram方法,指定目标玩家
                    createHologram(msgKey, replaceVariables(format), height, 0, player);
                }
            }

            // 6. 延迟移除玩家信息
            FateServants.getInstance().getServer().getScheduler().runTaskLater(
                FateServants.getInstance(),
                () -> {
                    try {
                        PacketContainer removeInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
                        removeInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                        removeInfo.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfoData));
                        FateServants.getInstance().getProtocolManager().sendServerPacket(player, removeInfo);
                    } catch (Exception e) {
                        FateServants.getInstance().getLogger().severe("移除玩家信息时发生错误: " + e.getMessage());
                    }
                },
                20L
            );
            
        } catch (Exception e) {
            FateServants.getInstance().getLogger().severe("为玩家 " + player.getName() + " 生成英灵时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取属性处理器
     * @return 属性处理器
     */
    public ServantAttributeHandler getAttributeHandler() {
        return attributeHandler;
    }
    
    /**
     * 获取全息图处理器
     * @return 全息图处理器
     */
    public ServantHologramHandler getHologramHandler() {
        return hologramHandler;
    }
    
    /**
     * 获取战斗处理器
     * @return 战斗处理器
     */
    public ServantCombatHandler getCombatHandler() {
        return combatHandler;
    }
    
    /**
     * 获取移动处理器
     * @return 移动处理器
     */
    public ServantMovementHandler getMovementHandler() {
        return movementHandler;
    }
    
    /**
     * 获取AI处理器
     * @return AI处理器
     */
    public ServantAIHandler getAIHandler() {
        return aiHandler;
    }

    /**
     * 获取英灵名称
     * @return 英灵名称
     */
    public String getName() {
        return servantClass.getDisplayName();
    }
    
    /**
     * 获取英灵当前模式
     * @return 当前模式
     */
    public String getMode() {
        if (aiController.getCurrentBehavior() == ServantBehavior.COMBAT) {
            return "COMBAT";
        } else if (aiController.getCurrentBehavior() == ServantBehavior.FOLLOW) {
            return "FOLLOW";
        } else if (aiController.getCurrentBehavior() == ServantBehavior.DEFEND) {
            return "DEFEND";
        } else {
            return "IDLE";
        }
    }
    
    /**
     * 设置英灵模式
     * @param mode 模式名称
     */
    public void setMode(String mode) {
        switch (mode.toUpperCase()) {
            case "COMBAT":
                aiController.setBehavior(ServantBehavior.COMBAT);
                break;
            case "FOLLOW":
                aiController.setBehavior(ServantBehavior.FOLLOW);
                break;
            case "DEFEND":
                aiController.setBehavior(ServantBehavior.DEFEND);
                break;
            case "IDLE":
            default:
                aiController.setBehavior(ServantBehavior.IDLE);
                break;
        }
    }

    /**
     * 设置英灵UUID
     * @param uuid 英灵UUID
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * 设置英灵当前魔法值
     * @param mana 魔法值
     */
    public void setMana(double mana) {
        this.currentMana = Math.min(mana, this.maxMana);
    }

    /**
     * 获取召唤时间
     * @return 召唤时间戳
     */
    public long getSummonTime() {
        return summonTime;
    }
    
    /**
     * 设置召唤时间
     * @param summonTime 召唤时间戳
     */
    public void setSummonTime(long summonTime) {
        this.summonTime = summonTime;
    }

    /**
     * 设置属性成长资质
     * @param growths 属性成长资质映射
     */
    public void setAttributeGrowths(Map<String, Double> growths) {
        this.attributeGrowths = new HashMap<>(growths);
    }

    /**
     * 更新英灵属性
     * 当等级或其他相关因素变化时调用
     */
    public void updateAttributes() {
        // 根据等级和品质更新属性值
        Map<String, Double> baseAttrs = servantClass.getBaseAttributes();
        
        for (Map.Entry<String, Double> entry : baseAttrs.entrySet()) {
            String key = entry.getKey();
            double baseValue = entry.getValue();
            double growth = attributeGrowths.getOrDefault(key, 0.5);
            double qualityMultiplier = quality.getAttributeMultiplier();
            
            // 计算最终属性值: 基础值 * (1 + 成长资质 * (等级-1) * 0.1) * 品质倍率
            double finalValue = baseValue * (1 + growth * (level-1) * 0.1) * qualityMultiplier;
            
            // 更新属性基础值
            attributes.setBaseValue(key, finalValue);
        }
        
        // 更新生命值和魔法值上限
        this.maxHealth = attributes.getValue("health");
        this.maxMana = attributes.getValue("mana");
        
        // 确保当前值不超过最大值
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }
        if (this.currentMana > this.maxMana) {
            this.currentMana = this.maxMana;
        }
        
        // 更新全息图
        updateHolograms();
    }

    /**
     * 设置英灵的所有者（注意：这是个假方法，仅为了兼容性）
     * 实际上所有者在创建时就已经确定，这个方法不做任何实际修改
     * @param newOwner 新的所有者
     */
    public void setOwner(Player newOwner) {
        // 英灵的所有者在构造函数中已经设置，此方法仅为了兼容性
        // 不做任何实际修改
        DebugUtils.debug("servant.set_owner.ignored", 
            newOwner != null ? newOwner.getName() : "null", 
            owner != null ? owner.getName() : "null");
    }
    
    /**
     * 设置英灵的位置
     * @param newLocation 新的位置
     */
    public void setLocation(Location newLocation) {
        if (newLocation != null) {
            this.location = newLocation.clone();
            DebugUtils.debug("servant.set_location", 
                newLocation.getWorld().getName(),
                newLocation.getX(),
                newLocation.getY(),
                newLocation.getZ());
        }
    }
    
    /**
     * 设置特定属性的成长资质
     * @param attributeName 属性名称
     * @param growth 成长资质值
     */
    public void setAttributeGrowth(String attributeName, double growth) {
        if (attributeName != null && !attributeName.isEmpty()) {
            this.attributeGrowths.put(attributeName, growth);
            DebugUtils.debug("servant.set_attribute_growth", attributeName, growth);
        }
    }
}