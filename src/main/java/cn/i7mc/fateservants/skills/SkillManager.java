package cn.i7mc.fateservants.skills;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.MessageManager;
import io.lumine.xikage.mythicmobs.MythicMobs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.*;
import java.util.stream.Collectors;

public class SkillManager {
    private final FateServants plugin;
    private final MythicMobs mythicMobs;
    private final Map<String, Boolean> skillCache = new HashMap<>();
    private final Map<String, List<Skill>> classSkills = new HashMap<>();

    public SkillManager(FateServants plugin) {
        this.plugin = plugin;
        this.mythicMobs = MythicMobs.inst();
        loadSkills();
    }

    /**
     * 从配置文件加载所有职阶的技能
     */
    private void loadSkills() {
        DebugUtils.log("skill.loading");
        
        // 从classes.yml加载配置
        ConfigurationSection classesSection = plugin.getClassesConfig().getConfigurationSection("classes");
        if (classesSection == null) {
            DebugUtils.log("skill.no_classes_section");
            return;
        }

        Set<String> classNames = classesSection.getKeys(false);
        DebugUtils.log("skill.found_classes", String.join(", ", classNames));

        for (String className : classNames) {
            String upperClassName = className.toUpperCase();
            DebugUtils.log("skill.loading_class", upperClassName);
            
            ConfigurationSection classSection = classesSection.getConfigurationSection(className);
            if (classSection == null) {
                DebugUtils.log("skill.empty_class_section", upperClassName);
                continue;
            }

            ConfigurationSection skillsSection = classSection.getConfigurationSection("skills");
            if (skillsSection == null) {
                DebugUtils.log("skill.empty_skills_section", upperClassName);
                continue;
            }

            Set<String> skillKeys = skillsSection.getKeys(false);
            DebugUtils.log("skill.found_skills", upperClassName, String.join(", ", skillKeys));

            List<Skill> skills = new ArrayList<>();
            
            for (String key : skillKeys) {
                ConfigurationSection skillSection = skillsSection.getConfigurationSection(key);
                if (skillSection == null) {
                    DebugUtils.log("skill.empty_skill_section", upperClassName, key);
                    continue;
                }

                String name = skillSection.getString("name", key);
                String castType = skillSection.getString("cast_type", "passive");
                double manaCost = skillSection.getDouble("mana_cost", 0.0);
                int cooldown = skillSection.getInt("cooldown", 0);
                double chance = skillSection.getDouble("chance", 100.0);
                String description = skillSection.getString("description", "");

                DebugUtils.log("skill.loading_skill", name, upperClassName, castType, manaCost, cooldown, chance);

                skills.add(new Skill(name, castType, manaCost, cooldown, chance, description));
            }

            classSkills.put(upperClassName, skills);
            DebugUtils.log("skill.loaded_class", upperClassName, skills.size());
        }
        
        // 记录已加载的所有职阶和技能数量
        DebugUtils.log("skill.loading_complete");
    }

    /**
     * 获取指定职阶的所有技能
     */
    public List<Skill> getClassSkills(String className) {
        DebugUtils.log("skill.get_class_skills", className);
        
        // 尝试直接获取
        List<Skill> skills = classSkills.get(className);
        
        // 如果没找到，尝试忽略大小写匹配
        if (skills == null) {
            String upperClassName = className.toUpperCase();
            for (Map.Entry<String, List<Skill>> entry : classSkills.entrySet()) {
                if (entry.getKey().toUpperCase().equals(upperClassName)) {
                    skills = entry.getValue();
                    break;
                }
            }
        }
        
        if (skills == null) {
            DebugUtils.log("skill.class_not_found", className);
            return Collections.emptyList();
        }
        
        DebugUtils.log("skill.found_skills_count", skills.size());
        return new ArrayList<>(skills);
    }

    /**
     * 获取所有已加载的职阶名称
     */
    public Set<String> getLoadedClasses() {
        return new HashSet<>(classSkills.keySet());
    }

    /**
     * 检查技能是否存在
     */
    public boolean isSkillExists(String skillName) {
        return skillCache.computeIfAbsent(skillName, 
            name -> mythicMobs.getSkillManager().getSkill(name).isPresent());
    }

    /**
     * 执行技能
     * @param caster 技能施放者
     * @param skillName 技能名称
     * @param power 技能威力倍率
     * @return 是否成功执行
     */
    public boolean castSkill(LivingEntity caster, String skillName, float power) {
        if (!isSkillExists(skillName)) {
            DebugUtils.log("skill.not_exists", skillName);
            return false;
        }

        try {
            // 获取目标的初始生命值
            List<LivingEntity> targets = caster.getNearbyEntities(5, 5, 5).stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .collect(Collectors.toList());
            
            Map<LivingEntity, Double> initialHealthMap = new HashMap<>();
            for (LivingEntity target : targets) {
                initialHealthMap.put(target, target.getHealth());
            }
            
            // 执行技能
            boolean success = mythicMobs.getAPIHelper().castSkill(
                caster,                    // 施法者
                skillName,                 // 技能名称
                caster.getLocation(),      // 施法位置
                Collections.emptyList(),    // 目标实体列表
                Collections.emptyList(),    // 目标位置列表
                power                      // 技能威力
            );
            
            if (success) {
                DebugUtils.log("skill.cast_success", skillName, caster.getName());
                
                // 计算实际伤害并添加经验值
                for (LivingEntity target : targets) {
                    double initialHealth = initialHealthMap.get(target);
                    double finalHealth = target.getHealth();
                    double actualDamage = initialHealth - finalHealth;
                    
                    if (actualDamage > 0) {
                        DebugUtils.log("skill.damage_caused", skillName, target.getName(), actualDamage);
                        
                        // 找到对应的英灵
                        for (Servant servant : plugin.getServantManager().getAllServants()) {
                            if (servant.getOwner().equals(caster)) {
                                servant.addExperience(actualDamage);
                                DebugUtils.log("skill.exp_gained", servant.getClass().getSimpleName(), actualDamage);
                                break;
                            }
                        }
                    }
                }
            } else {
                DebugUtils.log("skill.cast_failed", skillName, "MythicMobs API返回失败");
            }
            
            return success;
        } catch (Exception e) {
            DebugUtils.log("skill.cast_error", skillName, e.getMessage());
            return false;
        }
    }

    /**
     * 使用默认威力执行技能
     */
    public boolean castSkill(LivingEntity caster, String skillName) {
        return castSkill(caster, skillName, 1.0f);
    }

    /**
     * 清除技能缓存
     */
    public void clearCache() {
        skillCache.clear();
        DebugUtils.log("skill.cache_cleared");
    }

    /**
     * 重新加载所有技能
     */
    public void reload() {
        clearCache();
        classSkills.clear();
        loadSkills();
        DebugUtils.log("skill.reloaded");
    }
} 