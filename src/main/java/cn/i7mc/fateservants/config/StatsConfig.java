package cn.i7mc.fateservants.config;

import cn.i7mc.fateservants.FateServants;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StatsConfig {
    private final FateServants plugin;
    private FileConfiguration config;
    private final Map<String, AttributeInfo> attributes;
    private final Map<String, ClassInfo> classes;

    public StatsConfig(FateServants plugin) {
        this.plugin = plugin;
        this.attributes = new HashMap<>();
        this.classes = new HashMap<>();
        reload();
    }

    public void reload() {
        File configFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!configFile.exists()) {
            plugin.saveResource("stats.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadAttributes();
        loadClasses();
    }

    private void loadAttributes() {
        attributes.clear();
        ConfigurationSection attributesSection = config.getConfigurationSection("attributes");
        if (attributesSection == null) return;

        for (String key : attributesSection.getKeys(false)) {
            ConfigurationSection attrSection = attributesSection.getConfigurationSection(key);
            if (attrSection == null) continue;

            String displayName = attrSection.getString("display_name", key);
            String symbol = attrSection.getString("symbol", "");
            double minValue = attrSection.getDouble("min_value", 0.0);
            double maxValue = attrSection.getDouble("max_value", Double.MAX_VALUE);

            Map<String, String> mapping = new HashMap<>();
            ConfigurationSection mappingSection = attrSection.getConfigurationSection("mapping");
            if (mappingSection != null) {
                for (String provider : mappingSection.getKeys(false)) {
                    mapping.put(provider, mappingSection.getString(provider, key));
                }
            }

            attributes.put(key, new AttributeInfo(key, displayName, symbol, minValue, maxValue, mapping));
        }
    }

    private void loadClasses() {
        classes.clear();
        ConfigurationSection classesSection = config.getConfigurationSection("classes");
        if (classesSection == null) return;

        for (String key : classesSection.getKeys(false)) {
            ConfigurationSection classSection = classesSection.getConfigurationSection(key);
            if (classSection == null) continue;

            String displayName = classSection.getString("display_name", key);
            Map<String, Double> baseAttributes = new HashMap<>();

            ConfigurationSection attrSection = classSection.getConfigurationSection("base_attributes");
            if (attrSection != null) {
                for (String attr : attrSection.getKeys(false)) {
                    baseAttributes.put(attr, attrSection.getDouble(attr, 0.0));
                }
            }

            classes.put(key, new ClassInfo(key, displayName, baseAttributes));
        }
    }

    public AttributeInfo getAttribute(String key) {
        return attributes.get(key.toLowerCase());
    }

    public ClassInfo getClass(String key) {
        return classes.get(key.toUpperCase());
    }

    public Map<String, AttributeInfo> getAttributes() {
        return new HashMap<>(attributes);
    }

    public Map<String, ClassInfo> getClasses() {
        return new HashMap<>(classes);
    }

    public static class AttributeInfo {
        private final String key;
        private final String displayName;
        private final String symbol;
        private final double minValue;
        private final double maxValue;
        private final Map<String, String> mapping;

        public AttributeInfo(String key, String displayName, String symbol, double minValue, double maxValue, Map<String, String> mapping) {
            this.key = key;
            this.displayName = displayName;
            this.symbol = symbol;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.mapping = mapping;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSymbol() {
            return symbol;
        }

        public double getMinValue() {
            return minValue;
        }

        public double getMaxValue() {
            return maxValue;
        }

        public String getMappedName(String provider) {
            return mapping.getOrDefault(provider, key);
        }
    }

    public static class ClassInfo {
        private final String key;
        private final String displayName;
        private final Map<String, Double> baseAttributes;

        public ClassInfo(String key, String displayName, Map<String, Double> baseAttributes) {
            this.key = key;
            this.displayName = displayName;
            this.baseAttributes = baseAttributes;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Map<String, Double> getBaseAttributes() {
            return new HashMap<>(baseAttributes);
        }

        public double getBaseAttribute(String key) {
            return baseAttributes.getOrDefault(key.toLowerCase(), 0.0);
        }
    }
} 