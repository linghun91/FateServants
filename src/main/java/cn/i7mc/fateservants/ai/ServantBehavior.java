package cn.i7mc.fateservants.ai;

public enum ServantBehavior {
    IDLE("待机", "英灵处于待机状态"),
    FOLLOW("跟随", "让英灵跟随主人"),
    COMBAT("战斗", "让英灵主动攻击敌对生物"),
    DEFEND("防御", "让英灵保护主人，攻击靠近的敌对生物");

    private final String displayName;
    private final String description;

    ServantBehavior(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
