package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.attributes.ServantQuality;
import cn.i7mc.fateservants.config.ConfigManager;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 格式化工具类，用于处理各种显示格式化
 */
public class FormatUtils {
    private static FateServants plugin;
    private static ConfigManager configManager;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final Map<String, DecimalFormat> numberFormatCache = new HashMap<>();
    
    /**
     * 初始化格式化工具类
     * @param pluginInstance 插件实例
     * @param configManagerInstance 配置管理器实例
     */
    public static void init(FateServants pluginInstance, ConfigManager configManagerInstance) {
        plugin = pluginInstance;
        configManager = configManagerInstance;
    }
    
    /**
     * 格式化职阶显示
     * @param servantClass 职阶对象
     * @return 格式化后的职阶显示文本
     */
    public static String formatClass(ServantClass servantClass) {
        if (servantClass == null) {
            return MessageManager.get("format.unknown_class");
        }
        
        String template = configManager.getString("gui.yml", "format.class", "&7职阶: &f{name}");
        
        Map<String, Object> values = new HashMap<>();
        values.put("name", servantClass.getDisplayName());
        values.put("id", servantClass.getId());
        
        return format(template, values);
    }
    
    /**
     * 格式化品质显示
     * @param quality 品质名称
     * @return 格式化后的品质显示文本
     */
    public static String formatQuality(String quality) {
        if (quality == null || quality.isEmpty()) {
            return MessageManager.get("format.unknown_quality");
        }
        
        String displayName = configManager.getString("config.yml", "qualities." + quality + ".display_name", "&7" + quality);
        String template = configManager.getString("gui.yml", "format.quality", "&7品质: {display_name}");
        
        Map<String, Object> values = new HashMap<>();
        values.put("display_name", displayName);
        values.put("id", quality);
        
        return format(template, values);
    }
    
    /**
     * 格式化品质显示
     * @param quality 品质对象
     * @return 格式化后的品质显示文本
     */
    public static String formatQuality(ServantQuality quality) {
        if (quality == null) {
            return MessageManager.get("format.unknown_quality");
        }
        
        return formatQuality(quality.getId());
    }
    
    /**
     * 格式化生命值显示
     * @param current 当前生命值
     * @param max 最大生命值
     * @return 格式化后的生命值显示文本
     */
    public static String formatHealth(double current, double max) {
        String template = configManager.getString("gui.yml", "format.health", "&7生命值: &a{current}/{max} &7(&a{percent}%&7)");
        int percent = max > 0 ? (int)((current / max) * 100) : 0;
        
        Map<String, Object> values = new HashMap<>();
        values.put("current", formatNumber(current, "#,##0.#"));
        values.put("max", formatNumber(max, "#,##0.#"));
        values.put("percent", percent);
        
        return format(template, values);
    }
    
    /**
     * 格式化生命值显示
     * @param servant 英灵对象
     * @return 格式化后的生命值显示文本
     */
    public static String formatHealth(Servant servant) {
        if (servant == null) {
            return formatHealth(0, 0);
        }
        return formatHealth(servant.getCurrentHealth(), servant.getMaxHealth());
    }
    
    /**
     * 格式化魔法值显示
     * @param current 当前魔法值
     * @param max 最大魔法值
     * @return 格式化后的魔法值显示文本
     */
    public static String formatMana(double current, double max) {
        String template = configManager.getString("gui.yml", "format.mana", "&7魔法值: &b{current}/{max} &7(&b{percent}%&7)");
        int percent = max > 0 ? (int)((current / max) * 100) : 0;
        
        Map<String, Object> values = new HashMap<>();
        values.put("current", formatNumber(current, "#,##0.#"));
        values.put("max", formatNumber(max, "#,##0.#"));
        values.put("percent", percent);
        
        return format(template, values);
    }
    
    /**
     * 格式化魔法值显示
     * @param servant 英灵对象
     * @return 格式化后的魔法值显示文本
     */
    public static String formatMana(Servant servant) {
        if (servant == null) {
            return formatMana(0, 0);
        }
        return formatMana(servant.getCurrentMana(), servant.getMaxMana());
    }
    
    /**
     * 格式化经验值显示
     * @param current 当前经验值
     * @param required 升级所需经验值
     * @param level 当前等级
     * @return 格式化后的经验值显示文本
     */
    public static String formatExperience(double current, double required, double level) {
        String template = configManager.getString("gui.yml", "format.experience", 
                "&7等级: &e{level} &7经验: &e{current}/{required} &7(&e{percent}%&7)");
        int percent = required > 0 ? (int)((current / required) * 100) : 0;
        
        Map<String, Object> values = new HashMap<>();
        values.put("current", formatNumber(current, "#,##0.#"));
        values.put("required", formatNumber(required, "#,##0.#"));
        values.put("level", formatNumber(level, "#,##0.##"));
        values.put("percent", percent);
        
        return format(template, values);
    }
    
    /**
     * 格式化经验值显示
     * @param servant 英灵对象
     * @return 格式化后的经验值显示文本
     */
    public static String formatExperience(Servant servant) {
        if (servant == null) {
            return formatExperience(0, 0, 0);
        }
        return formatExperience(servant.getExperience(), servant.getRequiredExperience(), servant.getLevel());
    }
    
    /**
     * 替换字符串中的颜色代码
     * @param text 原始文本
     * @return 应用了颜色的文本
     */
    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * 简单格式化字符串，替换单个变量
     * @param template 模板字符串
     * @param key 变量名
     * @param value 变量值
     * @return 格式化后的字符串
     */
    public static String format(String template, String key, String value) {
        Map<String, Object> values = new HashMap<>();
        values.put(key, value);
        return format(template, values);
    }
    
    /**
     * 格式化移动速度显示
     * @param speed 移动速度
     * @return 格式化后的移动速度显示文本
     */
    public static String formatMovementSpeed(double speed) {
        String template = configManager.getString("gui.yml", "format.movement_speed", "&7移动速度: &f{value}");
        
        Map<String, Object> values = new HashMap<>();
        values.put("value", formatNumber(speed, "#,##0.##"));
        
        return format(template, values);
    }
    
    /**
     * 格式化攻击速度显示
     * @param attackSpeed 攻击速度值（游戏刻）
     * @return 格式化后的攻击速度显示文本
     */
    public static String formatAttackSpeed(int attackSpeed) {
        String template = configManager.getString("gui.yml", "format.attack_speed", "&7攻击速度: &f{attacks_per_second}次/秒");
        double attacksPerSecond = 20.0 / Math.max(1, attackSpeed); // 每秒攻击次数
        
        Map<String, Object> values = new HashMap<>();
        values.put("value", attackSpeed);
        values.put("attacks_per_second", formatNumber(attacksPerSecond, "0.##"));
        
        return format(template, values);
    }
    
    /**
     * 格式化英灵属性显示
     * @param servant 英灵对象
     * @param attributeName 属性名称
     * @param value 属性值
     * @param growth 成长资质值
     * @return 格式化后的属性显示文本
     */
    public static String formatAttribute(Servant servant, String attributeName, double value, double growth) {
        // 获取属性显示名称
        String displayName = configManager.getString("stats.yml", "attributes." + attributeName + ".display_name", attributeName);
        String symbol = configManager.getString("stats.yml", "attributes." + attributeName + ".symbol", "");
        
        // 获取格式模板
        String template = configManager.getString("gui.yml", "format.attribute", "&7{display_name}: &f{value}{symbol} {growth_color}({growth}%)");
        
        // 计算成长资质百分比和颜色
        int growthPercent = (int)(growth * 100);
        String growthColor = getGrowthColor(growth);
        
        Map<String, Object> values = new HashMap<>();
        values.put("display_name", displayName);
        values.put("value", formatNumber(value, "#,##0.##"));
        values.put("symbol", symbol);
        values.put("growth", growthPercent);
        values.put("growth_color", growthColor);
        
        return format(template, values);
    }
    
    /**
     * 格式化技能信息显示
     * @param skillName 技能名称
     * @param description 技能描述
     * @param manaCost 魔法值消耗
     * @param cooldown 冷却时间
     * @param castType 施法类型
     * @return 格式化后的技能信息显示文本
     */
    public static String formatSkill(String skillName, String description, double manaCost, int cooldown, String castType) {
        String template = configManager.getString("gui.yml", "format.skill", 
                "&e{name}\n&7{description}\n&b消耗: {mana_cost} &c冷却: {cooldown}秒 &7类型: {cast_type}");
        
        String castTypeDisplay = "active".equalsIgnoreCase(castType) 
                ? MessageManager.get("skill.cast_type.active") 
                : MessageManager.get("skill.cast_type.passive");
        
        Map<String, Object> values = new HashMap<>();
        values.put("name", skillName);
        values.put("description", description);
        values.put("mana_cost", formatNumber(manaCost, "#,##0.#"));
        values.put("cooldown", cooldown / 20); // 转换为秒
        values.put("cast_type", castTypeDisplay);
        
        return format(template, values);
    }
    
    /**
     * 获取成长资质颜色
     * @param growth 成长资质值
     * @return 代表资质等级的颜色代码
     */
    private static String getGrowthColor(double growth) {
        if (growth >= 0.9) return "&c"; // 红色 - 极佳
        if (growth >= 0.8) return "&6"; // 金色 - 优秀
        if (growth >= 0.7) return "&e"; // 黄色 - 良好
        if (growth >= 0.6) return "&a"; // 绿色 - 中等
        if (growth >= 0.5) return "&b"; // 浅蓝 - 普通
        return "&7";                   // 灰色 - 较差
    }
    
    /**
     * 格式化字符串，替换变量
     * @param template 模板字符串
     * @param values 变量值Map
     * @return 格式化后的字符串
     */
    public static String format(String template, Map<String, Object> values) {
        if (template == null) {
            return "";
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = values.get(key);
            String replacement = (value == null) ? "" : value.toString();
            // 替换特殊字符
            replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");
            matcher.appendReplacement(result, replacement);
        }
        
        matcher.appendTail(result);
        // 应用颜色代码
        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }
    
    /**
     * 格式化数字
     * @param number 数字
     * @param pattern 格式模式
     * @return 格式化后的数字字符串
     */
    public static String formatNumber(double number, String pattern) {
        // 从缓存获取格式化器
        DecimalFormat formatter = numberFormatCache.get(pattern);
        if (formatter == null) {
            formatter = new DecimalFormat(pattern);
            numberFormatCache.put(pattern, formatter);
        }
        
        return formatter.format(number);
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        numberFormatCache.clear();
    }
    
    /**
     * 格式化技能列表显示
     * @param skills 技能列表
     * @return 格式化后的技能列表文本
     */
    public static List<String> formatSkills(List<cn.i7mc.fateservants.skills.Skill> skills) {
        List<String> lines = new ArrayList<>();
        
        String header = configManager.getString("gui.yml", "format.skills_header", "&9技能列表:");
        lines.add(colorize(header));
        
        if (skills == null || skills.isEmpty()) {
            String emptyMsg = configManager.getString("gui.yml", "format.no_skills", "&7暂无技能");
            lines.add(colorize(emptyMsg));
            return lines;
        }
        
        for (cn.i7mc.fateservants.skills.Skill skill : skills) {
            String template = configManager.getString("gui.yml", 
                    "format.skill_entry", 
                    "&b{name} &7- 消耗: &3{cost}MP &7- CD: &3{cooldown}秒");
            
            Map<String, Object> values = new HashMap<>();
            values.put("name", skill.getName());
            values.put("cost", formatNumber(skill.getManaCost(), "#,##0.#"));
            values.put("cooldown", formatNumber(skill.getCooldown() / 20.0, "#,##0.#")); // 转换tick为秒
            values.put("description", skill.getDescription());
            values.put("type", skill.getCastType());
            
            // 检查技能类型并添加额外信息
            if ("passive".equalsIgnoreCase(skill.getCastType())) {
                template = configManager.getString("gui.yml", 
                        "format.passive_skill_entry", 
                        "&a[被动] &b{name} &7- 触发几率: &3{chance}%");
                values.put("chance", formatNumber(skill.getChance() * 100, "#,##0.#"));
            }
            
            lines.add(colorize(format(template, values)));
            
            // 如果有描述，添加描述行
            if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
                String descTemplate = configManager.getString("gui.yml", 
                        "format.skill_description", 
                        "&7  {description}");
                lines.add(colorize(format(descTemplate, "description", skill.getDescription())));
            }
        }
        
        return lines;
    }
    
    /**
     * 格式化英灵基本信息
     * @param servant 英灵对象
     * @return 格式化后的英灵基本信息文本列表
     */
    public static List<String> formatServant(Servant servant) {
        List<String> lines = new ArrayList<>();
        
        if (servant == null) {
            lines.add(colorize(MessageManager.get("format.invalid_servant")));
            return lines;
        }
        
        // 添加职阶和品质
        lines.add(colorize(formatClass(servant.getServantClass())));
        lines.add(colorize(formatQuality(servant.getQuality())));
        
        // 添加等级和经验
        lines.add(colorize(formatExperience(servant)));
        
        // 添加生命值和魔法值
        lines.add(colorize(formatHealth(servant)));
        lines.add(colorize(formatMana(servant)));
        
        // 添加移动速度
        lines.add(colorize(formatMovementSpeed(servant.getMovementSpeed())));
        
        // 添加其他属性
        Map<String, Double> attributes = servant.getAttributes().getAll();
        for (Map.Entry<String, Double> attribute : attributes.entrySet()) {
            String attrName = attribute.getKey();
            double attrValue = attribute.getValue();
            
            // 跳过已经单独显示的属性
            if (attrName.equals("health") || attrName.equals("mana") || attrName.equals("movement_speed")) {
                continue;
            }
            
            String template = configManager.getString("gui.yml", "format.attribute", "&7{name}: &f{value}");
            String displayName = configManager.getString("config.yml", "attributes." + attrName + ".display_name", attrName);
            
            Map<String, Object> values = new HashMap<>();
            values.put("name", displayName);
            values.put("value", formatNumber(attrValue, "#,##0.##"));
            values.put("id", attrName);
            
            lines.add(colorize(format(template, values)));
        }
        
        return lines;
    }
    
    /**
     * 格式化英灵成长资质
     * @param servant 英灵对象
     * @return 格式化后的英灵成长资质文本列表
     */
    public static List<String> formatGrowthRates(Servant servant) {
        List<String> lines = new ArrayList<>();
        
        if (servant == null) {
            lines.add(colorize(MessageManager.get("format.invalid_servant")));
            return lines;
        }
        
        String header = configManager.getString("gui.yml", "format.growth_header", "&9成长资质:");
        lines.add(colorize(header));
        
        Map<String, Double> growths = servant.getAttributeGrowths();
        if (growths.isEmpty()) {
            String emptyMsg = configManager.getString("gui.yml", "format.no_growth_data", "&7暂无成长资质数据");
            lines.add(colorize(emptyMsg));
            return lines;
        }
        
        for (Map.Entry<String, Double> growth : growths.entrySet()) {
            String attrName = growth.getKey();
            double growthValue = growth.getValue();
            
            String template = configManager.getString("gui.yml", "format.growth_rate", 
                    "&7{name}: {color}{value}");
            
            String displayName = configManager.getString("config.yml", 
                    "attributes." + attrName + ".display_name", attrName);
            
            // 根据成长值选择颜色
            String color = "&c"; // 默认红色（较差）
            if (growthValue > 1.5) color = "&a"; // 绿色（优秀）
            else if (growthValue > 1.0) color = "&e"; // 黄色（一般）
            else if (growthValue > 0.8) color = "&6"; // 金色（较低）
            
            Map<String, Object> values = new HashMap<>();
            values.put("name", displayName);
            values.put("value", formatNumber(growthValue, "0.00"));
            values.put("color", color);
            values.put("id", attrName);
            
            lines.add(colorize(format(template, values)));
        }
        
        return lines;
    }
    
    /**
     * 格式化冷却时间显示
     * @param cooldownTicks 冷却时间（以tick为单位）
     * @return 格式化后的冷却时间显示文本
     */
    public static String formatCooldown(int cooldownTicks) {
        // 将tick转为秒
        double seconds = cooldownTicks / 20.0;
        
        if (seconds < 60) {
            // 不足1分钟，显示秒
            return formatNumber(seconds, "#0.0") + "秒";
        } else if (seconds < 3600) {
            // 不足1小时，显示分:秒
            int minutes = (int)(seconds / 60);
            int remainingSeconds = (int)(seconds % 60);
            return minutes + "分" + (remainingSeconds > 0 ? remainingSeconds + "秒" : "");
        } else {
            // 超过1小时，显示时:分:秒
            int hours = (int)(seconds / 3600);
            int minutes = (int)((seconds % 3600) / 60);
            return hours + "小时" + (minutes > 0 ? minutes + "分" : "");
        }
    }
    
    /**
     * 格式化时间戳为可读时间
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的时间字符串
     */
    public static String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }
    
    /**
     * 格式化时间差（从指定时间到现在）
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的时间差
     */
    public static String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // 不到1分钟
        if (diff < 60 * 1000) {
            return "刚刚";
        }
        
        // 不到1小时
        if (diff < 60 * 60 * 1000) {
            return (diff / (60 * 1000)) + "分钟前";
        }
        
        // 不到1天
        if (diff < 24 * 60 * 60 * 1000) {
            return (diff / (60 * 60 * 1000)) + "小时前";
        }
        
        // 不到1周
        if (diff < 7 * 24 * 60 * 60 * 1000) {
            return (diff / (24 * 60 * 60 * 1000)) + "天前";
        }
        
        // 不到1个月
        if (diff < 30 * 24 * 60 * 60 * 1000) {
            return (diff / (7 * 24 * 60 * 60 * 1000)) + "周前";
        }
        
        // 不到1年
        if (diff < 365 * 24 * 60 * 60 * 1000) {
            return (diff / (30 * 24 * 60 * 60 * 1000)) + "个月前";
        }
        
        // 超过1年
        return (diff / (365 * 24 * 60 * 60 * 1000)) + "年前";
    }
    
    /**
     * 格式化进度条
     * @param current 当前值
     * @param max 最大值
     * @param length 进度条长度
     * @param completeChar 完成字符
     * @param incompleteChar 未完成字符
     * @param completeColor 完成部分颜色
     * @param incompleteColor 未完成部分颜色
     * @return 格式化后的进度条
     */
    public static String formatProgressBar(double current, double max, int length, 
            char completeChar, char incompleteChar, 
            String completeColor, String incompleteColor) {
        int percent = max > 0 ? (int)((current / max) * 100) : 0;
        int completeLength = max > 0 ? (int)((current / max) * length) : 0;
        
        StringBuilder progressBar = new StringBuilder();
        
        // 已完成部分
        progressBar.append(completeColor);
        for (int i = 0; i < completeLength; i++) {
            progressBar.append(completeChar);
        }
        
        // 未完成部分
        progressBar.append(incompleteColor);
        for (int i = completeLength; i < length; i++) {
            progressBar.append(incompleteChar);
        }
        
        return progressBar.toString();
    }
    
    /**
     * 格式化进度条（使用默认样式）
     * @param current 当前值
     * @param max 最大值
     * @param length 进度条长度
     * @return 格式化后的进度条
     */
    public static String formatProgressBar(double current, double max, int length) {
        String template = configManager.getString("gui.yml", "format.progress_bar_template", 
                "{bar} &7{percent}%");
        
        String completeColor = configManager.getString("gui.yml", "format.progress_bar_complete_color", "&a");
        String incompleteColor = configManager.getString("gui.yml", "format.progress_bar_incomplete_color", "&8");
        
        char completeChar = configManager.getString("gui.yml", "format.progress_bar_complete_char", "■").charAt(0);
        char incompleteChar = configManager.getString("gui.yml", "format.progress_bar_incomplete_char", "■").charAt(0);
        
        String bar = formatProgressBar(current, max, length, completeChar, incompleteChar, 
                completeColor, incompleteColor);
        int percent = max > 0 ? (int)((current / max) * 100) : 0;
        
        // 替换变量
        Map<String, Object> values = new HashMap<>();
        values.put("bar", bar);
        values.put("percent", percent);
        values.put("current", formatNumber(current, "#,##0.#"));
        values.put("max", formatNumber(max, "#,##0.#"));
        
        return format(template, values);
    }
    
    /**
     * 格式化健康度进度条
     * @param current 当前值
     * @param max 最大值
     * @return 格式化后的进度条
     */
    public static String formatHealthBar(double current, double max) {
        int length = configManager.getInt("gui.yml", "format.health_bar_length", 10);
        String template = configManager.getString("gui.yml", "format.health_bar_template", 
                "{bar} &7{current}/{max} &7(&a{percent}%&7)");
        
        // 根据健康度百分比选择颜色
        String completeColor;
        int percent = max > 0 ? (int)((current / max) * 100) : 0;
        
        if (percent > 70) {
            completeColor = "&a"; // 绿色 (>70%)
        } else if (percent > 40) {
            completeColor = "&e"; // 黄色 (40-70%)
        } else if (percent > 20) {
            completeColor = "&6"; // 金色 (20-40%)
        } else {
            completeColor = "&c"; // 红色 (<20%)
        }
        
        String incompleteColor = "&8"; // 深灰色
        
        char completeChar = configManager.getString("gui.yml", "format.health_bar_complete_char", "■").charAt(0);
        char incompleteChar = configManager.getString("gui.yml", "format.health_bar_incomplete_char", "■").charAt(0);
        
        String bar = formatProgressBar(current, max, length, completeChar, incompleteChar, 
                completeColor, incompleteColor);
        
        // 替换变量
        Map<String, Object> values = new HashMap<>();
        values.put("bar", bar);
        values.put("percent", percent);
        values.put("current", formatNumber(current, "#,##0.#"));
        values.put("max", formatNumber(max, "#,##0.#"));
        
        return format(template, values);
    }
    
    /**
     * 格式化为ItemStack的Lore
     * @param text 要格式化的文本
     * @return 格式化后的Lore列表
     */
    public static List<String> formatToLore(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] lines = text.split("\n");
        List<String> lore = new ArrayList<>();
        
        for (String line : lines) {
            lore.add(colorize(line));
        }
        
        return lore;
    }
    
    /**
     * 格式化物品属性为Lore
     * @param attributes 属性Map
     * @return 格式化后的Lore列表
     */
    public static List<String> formatAttributes(Map<String, Double> attributes) {
        List<String> lore = new ArrayList<>();
        
        if (attributes == null || attributes.isEmpty()) {
            return lore;
        }
        
        String header = configManager.getString("gui.yml", "format.attributes_header", "&9属性:");
        lore.add(colorize(header));
        
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            double value = entry.getValue();
            
            String template = configManager.getString("gui.yml", "format.attribute_entry", 
                    "&7{name}: &f{value}");
            
            String displayName = configManager.getString("config.yml", 
                    "attributes." + attrName + ".display_name", attrName);
            
            // 获取属性单位
            String unit = configManager.getString("config.yml", 
                    "attributes." + attrName + ".unit", "");
            
            Map<String, Object> values = new HashMap<>();
            values.put("name", displayName);
            values.put("value", formatNumber(value, "#,##0.##") + unit);
            values.put("id", attrName);
            
            lore.add(colorize(format(template, values)));
        }
        
        return lore;
    }
} 