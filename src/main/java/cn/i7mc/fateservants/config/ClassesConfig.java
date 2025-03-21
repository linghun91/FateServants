package cn.i7mc.fateservants.config;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.skills.SkillInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassesConfig {
    private final FateServants plugin;
    private FileConfiguration config;
    private final Map<String, ClassInfo> classes;

    public ClassesConfig(FateServants plugin) {
        this.plugin = plugin;
        this.classes = new HashMap<>();
        reload();
    }

    public void reload() {
        File configFile = new File(plugin.getDataFolder(), "classes.yml");
        if (!configFile.exists()) {
            plugin.saveResource("classes.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadClasses();
    }

    private void loadClasses() {
        classes.clear();
        ConfigurationSection classesSection = config.getConfigurationSection("classes");
        if (classesSection == null) return;

        for (String key : classesSection.getKeys(false)) {
            ConfigurationSection classSection = classesSection.getConfigurationSection(key);
            if (classSection == null) continue;

            String displayName = classSection.getString("display_name", key);
            String description = classSection.getString("description", "");
            Map<String, Double> baseAttributes = new HashMap<>();
            List<SkillInfo> skills = new ArrayList<>();

            ConfigurationSection attrSection = classSection.getConfigurationSection("base_attributes");
            if (attrSection != null) {
                for (String attr : attrSection.getKeys(false)) {
                    baseAttributes.put(attr, attrSection.getDouble(attr, 0.0));
                }
            }

            ConfigurationSection skillsSection = classSection.getConfigurationSection("skills");
            if (skillsSection != null) {
                for (String skillKey : skillsSection.getKeys(false)) {
                    ConfigurationSection skillSection = skillsSection.getConfigurationSection(skillKey);
                    if (skillSection != null) {
                        String skillName = skillSection.getString("name");
                        double chance = skillSection.getDouble("chance", 0.0);
                        String skillDescription = skillSection.getString("description", "");
                        if (skillName != null) {
                            skills.add(new SkillInfo(skillName, chance, skillDescription));
                        }
                    }
                }
            }

            classes.put(key, new ClassInfo(key, displayName, description, baseAttributes, skills));
        }
    }

    public ClassInfo getClass(String key) {
        return classes.get(key.toUpperCase());
    }

    public Map<String, ClassInfo> getClasses() {
        return new HashMap<>(classes);
    }

    public static class ClassInfo {
        private final String key;
        private final String displayName;
        private final String description;
        private final Map<String, Double> baseAttributes;
        private final List<SkillInfo> skills;

        public ClassInfo(String key, String displayName, String description, Map<String, Double> baseAttributes, List<SkillInfo> skills) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
            this.baseAttributes = baseAttributes;
            this.skills = skills;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Double> getBaseAttributes() {
            return new HashMap<>(baseAttributes);
        }

        public double getBaseAttribute(String key) {
            return baseAttributes.getOrDefault(key.toLowerCase(), 0.0);
        }

        public List<SkillInfo> getSkills() {
            return new ArrayList<>(skills);
        }

        public List<SkillInfo> rollSkills() {
            List<SkillInfo> result = new ArrayList<>();
            for (SkillInfo skill : skills) {
                if (skill.roll()) {
                    result.add(skill);
                }
            }
            return result;
        }
    }
} 