package cn.i7mc.fateservants.manager;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.ServantClass;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServantClassManager {
    private final FateServants plugin;
    private final Map<String, ServantClass> classes = new HashMap<>();

    public ServantClassManager(FateServants plugin) {
        this.plugin = plugin;
    }

    public void loadClasses() {
        classes.clear();
        
        // 确保配置文件存在
        File configFile = new File(plugin.getDataFolder(), "classes.yml");
        if (!configFile.exists()) {
            plugin.getLogger().info("classes.yml not found, creating default file");
            plugin.saveResource("classes.yml", false);
        }
        
        // 加载配置
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection classesSection = config.getConfigurationSection("classes");
        
        if (classesSection != null) {
            plugin.getLogger().info("Found classes section with keys: " + String.join(", ", classesSection.getKeys(false)));
            for (String id : classesSection.getKeys(false)) {
                ConfigurationSection classSection = classesSection.getConfigurationSection(id);
                plugin.getLogger().info("Loading class " + id + ", section exists: " + (classSection != null));
                if (classSection != null) {
                    plugin.getLogger().info("Class " + id + " config keys: " + String.join(", ", classSection.getKeys(false)));
                    ServantClass servantClass = ServantClass.fromConfig(id, classSection);
                    if (servantClass != null) {
                        classes.put(id.toUpperCase(), servantClass);
                        plugin.getLogger().info("Loaded class " + id + ": " + servantClass.getDisplayName() + 
                            " (quality range: " + servantClass.getQualityRangeMin() + " - " + servantClass.getQualityRangeMax() + ")");
                    } else {
                        plugin.getLogger().warning("Failed to load class " + id);
                    }
                }
            }
            plugin.getLogger().info("Loaded " + classes.size() + " classes: " + String.join(", ", classes.keySet()));
        } else {
            plugin.getLogger().warning("No classes section found in classes.yml");
        }
    }

    public ServantClass getClass(String id) {
        String upperCaseId = id.toUpperCase();
        ServantClass result = classes.get(upperCaseId);
        plugin.getLogger().info("Getting class " + upperCaseId + ": " + (result != null ? result.getDisplayName() : "null"));
        return result;
    }

    /**
     * 获取英灵职阶（getClass方法的别名，为了兼容性）
     * @param id 职阶ID
     * @return 职阶对象，如果不存在返回null
     */
    public ServantClass getServantClass(String id) {
        return getClass(id);
    }

    public Collection<ServantClass> getAllClasses() {
        return classes.values();
    }

    public boolean hasClass(String id) {
        boolean result = classes.containsKey(id.toUpperCase());
        plugin.getLogger().info("Checking if class " + id + " exists: " + result);
        return result;
    }
} 