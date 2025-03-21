package cn.i7mc.fateservants.database;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantClass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class LocalStorageManager {
    private final FateServants plugin;
    private final File saveFile;
    private FileConfiguration saveConfig;

    public LocalStorageManager(FateServants plugin) {
        this.plugin = plugin;
        this.saveFile = new File(plugin.getDataFolder(), "save.yml");
        reload();
    }

    public void reload() {
        if (!saveFile.exists()) {
            plugin.saveResource("save.yml", false);
        }
        saveConfig = YamlConfiguration.loadConfiguration(saveFile);
    }

    public void saveServant(Player owner, Servant servant) {
        String ownerUuid = owner.getUniqueId().toString();
        ConfigurationSection servantSection = saveConfig.createSection("servants." + ownerUuid);

        // 保存基本信息
        servantSection.set("servant_uuid", servant.getUuid().toString());
        servantSection.set("class", servant.getServantClass().getId());
        servantSection.set("health", servant.getCurrentHealth());

        // 保存位置信息
        Location loc = servant.getLocation();
        ConfigurationSection locationSection = servantSection.createSection("location");
        locationSection.set("world", loc.getWorld().getName());
        locationSection.set("x", loc.getX());
        locationSection.set("y", loc.getY());
        locationSection.set("z", loc.getZ());
        locationSection.set("yaw", loc.getYaw());
        locationSection.set("pitch", loc.getPitch());

        // 保存属性信息
        ConfigurationSection attributesSection = servantSection.createSection("attributes");
        for (Map.Entry<String, Double> entry : servant.getAttributes().getAll().entrySet()) {
            attributesSection.set(entry.getKey(), entry.getValue());
        }

        // 保存到文件
        try {
            saveConfig.save(saveFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存英灵数据到本地文件时发生错误", e);
        }
    }

    public Servant loadServant(Player player) {
        // 重新加载配置
        reload();
        
        // 检查玩家是否有英灵数据
        String uuidStr = player.getUniqueId().toString();
        if (!saveConfig.contains("servants." + uuidStr)) {
            return null;
        }
        
        try {
            // 获取英灵基本信息
            String classId = saveConfig.getString("servants." + uuidStr + ".class");
            ServantClass servantClass = plugin.getServantClassManager().getServantClass(classId);
            if (servantClass == null) {
                plugin.getLogger().warning("无法为玩家 " + player.getName() + " 加载英灵，找不到职阶: " + classId);
                return null;
            }
            
            // 创建英灵实例
            Servant servant = new Servant(player, servantClass);
            servant.setLevel(saveConfig.getInt("servants." + uuidStr + ".level", 1));
            servant.setExperience(saveConfig.getInt("servants." + uuidStr + ".experience", 0));
            servant.setHealth(saveConfig.getDouble("servants." + uuidStr + ".health", servant.getMaxHealth()));
            servant.setMana(saveConfig.getDouble("servants." + uuidStr + ".mana", servant.getMaxMana()));
            
            // 确保明确设置所有者
            servant.setOwner(player);
            
            // 加载状态
            servant.setFollowing(saveConfig.getBoolean("servants." + uuidStr + ".following", true));
            servant.setAttacking(saveConfig.getBoolean("servants." + uuidStr + ".attacking", true));
            servant.setDefending(saveConfig.getBoolean("servants." + uuidStr + ".defending", true));
            
            // 加载位置
            if (saveConfig.contains("servants." + uuidStr + ".location")) {
                String worldName = saveConfig.getString("servants." + uuidStr + ".location.world");
                double x = saveConfig.getDouble("servants." + uuidStr + ".location.x");
                double y = saveConfig.getDouble("servants." + uuidStr + ".location.y");
                double z = saveConfig.getDouble("servants." + uuidStr + ".location.z");
                float yaw = (float) saveConfig.getDouble("servants." + uuidStr + ".location.yaw");
                float pitch = (float) saveConfig.getDouble("servants." + uuidStr + ".location.pitch");
                
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location location = new Location(world, x, y, z, yaw, pitch);
                    servant.setLocation(location);
                }
            }
            
            // 加载属性信息
            if (saveConfig.contains("servants." + uuidStr + ".attributes")) {
                ConfigurationSection attrSection = saveConfig.getConfigurationSection("servants." + uuidStr + ".attributes");
                if (attrSection != null) {
                    for (String key : attrSection.getKeys(false)) {
                        double baseValue = attrSection.getDouble(key + ".base", 0);
                        servant.getAttributes().setBaseValue(key, baseValue);
                        
                        if (attrSection.contains(key + ".growth")) {
                            double growth = attrSection.getDouble(key + ".growth", 1.0);
                            servant.setAttributeGrowth(key, growth);
                        }
                    }
                }
            }
            
            plugin.getLogger().info("从本地存储加载了 " + player.getName() + " 的英灵数据");
            return servant;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "从本地文件加载英灵数据时发生错误", e);
            return null;
        }
    }

    public void removeServant(Player owner) {
        String ownerUuid = owner.getUniqueId().toString();
        saveConfig.set("servants." + ownerUuid, null);
        try {
            saveConfig.save(saveFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "从本地文件删除英灵数据时发生错误", e);
        }
    }

    public void saveAll() {
        try {
            saveConfig.save(saveFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存所有英灵数据到本地文件时发生错误", e);
        }
    }
} 