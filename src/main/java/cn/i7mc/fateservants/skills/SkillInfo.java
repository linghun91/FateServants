package cn.i7mc.fateservants.skills;

public class SkillInfo {
    private final String name;
    private final double chance;
    private final String description;

    public SkillInfo(String name, double chance, String description) {
        this.name = name;
        this.chance = chance;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public double getChance() {
        return chance;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查是否获得此技能（基于概率）
     */
    public boolean roll() {
        return Math.random() * 100 <= chance;
    }
} 