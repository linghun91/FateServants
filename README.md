# FateServants 项目结构索引

## 项目概述
FateServants是一个基于Java开发的Minecraft Bukkit/Spigot插件，主要实现了命运(Fate)系列英灵战斗宠物系统。该插件允许玩家在游戏中召唤、培养和使用来自命运系列的英灵作为战斗伙伴。

### 插件信息
- **名称**: FateServants
- **开发语言**: Java
- **适用平台**: Minecraft Bukkit/Spigot
- **版本**: 正在积极开发中 (最近更新：2024年2月)
- **作者**: Saga
- **必需依赖**: ProtocolLib
- **软依赖**: AttributePlus, ItemLoreOrigin, LibsDisguises, MythicMobs, PlaceholderAPI, Vault

### 主要功能
- 召唤不同职阶和品质的英灵
- 英灵具有独特属性、技能和外观
- 完整的属性系统和升级机制
- AI控制英灵跟随玩家、战斗等行为
- 实时显示英灵状态和信息

### 命令系统
- `/fs summon [职阶]` - 召唤指定职阶的英灵
- `/fs unsummon` - 解除当前英灵
- `/fs list` - 查看可用英灵列表
- `/fs reload` - 重载配置文件
- `/fs manage` - 打开英灵管理界面

## 属性系统说明

### 核心属性系统
项目中有5个独立管理的核心属性系统，它们与其他属性系统分开处理：

1. **生命值系统 (Health System)**
   - 独立的生命值计算和管理
   - 包含当前生命值和最大生命值
   - 配置在gui.yml中定义显示格式
   - 使用百分比显示生命值状态

2. **魔法值系统 (Mana System)**
   - 独立的魔法值计算和管理
   - 包含当前魔法值和最大魔法值
   - 配置在gui.yml中定义显示格式
   - 使用百分比显示魔法值状态

3. **经验值系统 (Experience System)**
   - 独立的经验值计算和升级机制
   - 包含当前经验值和所需经验值
   - 配置在gui.yml中定义显示格式
   - 使用整数显示经验值

4. **移动速度系统 (Movement Speed System)**
   - 独立的移动速度计算
   - 直接显示速度值
   - 配置在gui.yml中定义显示格式

5. **攻击速度系统 (Attack Speed System)**
   - 独立的攻击频率控制
   - 在classes.yml中为每个职阶单独配置
   - 使用tick作为时间单位（20tick = 1秒）
   - 通过attack_speed参数控制攻击间隔
   - 影响英灵对怪物的攻击频率

### 重要说明
**AttributePlus插件的源代码位置位于 'D:/aicore/FateServants/libs/ersha' 目录，所有关于AttributePlus的方法、接口和类的调用必须直接从源代码中查找，严禁使用反射或硬编码方式实现。所有属性映射必须通过配置文件实现，保证系统的完全动态化。**

### 英灵AttributePlus属性方法
由于英灵是通过ProtocolLib发包模拟的虚拟实体，无法直接使用AttributePlus的属性系统，因此采用了以下方案：

1. **属性传递机制**
   - 英灵攻击时将属性临时赋予主人
   - 通过主人执行实际的伤害计算
   - 攻击完成后恢复主人原有属性

2. **工作流程**
   - 获取主人的AttributePlus属性数据
   - 保存主人原有的属性源
   - 清空主人当前的属性源
   - 将英灵的属性临时添加给主人
   - 执行攻击动作
   - 恢复主人原有属性

3. **属性映射**
   - 在stats.yml中定义属性映射关系
   - 通过AttributePlusProvider进行属性转换
   - 支持自定义属性名称映射

4. **优点**
   - 完全利用AttributePlus的伤害计算系统
   - 支持暴击、闪避等高级属性效果
   - 无需实现复杂的伤害计算逻辑

5. **注意事项**
   - 属性传递过程是临时的
   - 设有异常处理机制
   - 如果出现异常会回退到基础伤害逻辑

### 其他属性系统
除了核心属性外的其他属性（如攻击力、防御力等）：
- 配置在stats.yml中统一管理
- 包含属性的显示名称、符号等信息
- 支持AttributePlus等外部插件的属性映射
- 可以通过配置文件扩展新的属性

### 属性显示规则
1. **核心属性**
   - 显示名称和格式在gui.yml中定义
   - 固定位置显示在GUI的基础属性部分
   - 使用独特的显示格式（如百分比、整数等）

2. **其他属性**
   - 显示名称在stats.yml中定义
   - 动态显示在GUI的属性列表中
   - 使用统一的显示格式
   - 支持属性成长资质显示（使用颜色区分等级）

### 配置文件
1. **gui.yml**
   - 定义GUI的整体布局
   - 包含核心属性的显示格式
   - 定义属性显示的样式和颜色

2. **stats.yml**
   - 定义其他属性的基本信息
   - 包含属性的显示名称和符号
   - 设置属性的最大/最小值
   - 配置与其他插件的属性映射

### 注意事项
1. 修改核心属性相关功能时，确保只在gui.yml中修改其显示相关配置
2. 添加或修改其他属性时，在stats.yml中进行配置
3. 核心属性的处理逻辑应该保持独立，不要与其他属性系统混合
4. 在代码中处理属性时，需要判断是否为核心属性，并使用相应的处理逻辑

## 职阶与品质系统

### 职阶系统
- 英灵分为多个职阶(SABER、ARCHER、LANCER等)
- 每个职阶拥有独特的基础属性、技能和皮肤
- 所有职阶在classes.yml中配置
- 职阶影响英灵的战斗风格和能力

### 品质系统
- 英灵拥有从E到SSR的品质等级
- 品质决定属性倍率，高品质英灵属性更强
- 各职阶有不同的品质范围限制(如SABER只能出现SS至SSR品质)
- 品质在config.yml中配置
- 随机生成英灵时将根据职阶限制生成对应范围内的品质

## 英灵移动系统

### 移动速度
- 所有移动速度统一从 `classes.yml` 中对应职阶的 `movement_speed` 读取
- 在所有行为模式下保持一致的速度
- 通过配置文件可以为不同职阶设置不同的基础移动速度

### 移动逻辑
英灵使用统一的移动处理逻辑，确保在所有状态下表现一致：
1. **跟随模式**
   - 始终跟随主人
   - 保持配置的理想跟随距离
   - 超出最大跟随距离时传送

2. **战斗模式**
   - 有目标时：追击目标直到进入攻击范围
   - 无目标时：保持在主人附近，与跟随模式行为一致
   - 搜索范围内没有目标时不会静止不动

3. **防御模式**
   - 有威胁目标时：追击威胁目标
   - 无威胁时：返回主人身边，保持理想跟随距离
   - 始终以保护主人为优先

### 距离管理
所有距离参数都在 `config.yml` 中配置：

1. **最大跟随距离** (`max_follow_distance`)
   - 默认值：16格
   - 超出此距离触发传送
   - 传送时会清空目标并进入短暂冷却
   - 适用于所有行为模式

2. **理想跟随距离** (`follow_distance`)
   - 默认值：2格
   - 日常跟随保持的距离
   - 无目标时的默认站位距离
   - 影响跟随和防御模式的站位

3. **战斗搜索范围** (`combat_search_range`)
   - 默认值：10格
   - 主动搜索敌对目标的范围
   - 影响战斗模式的搜索范围

4. **防御搜索范围** (`defend_search_range`)
   - 默认值：5格
   - 以主人为中心的威胁检测范围
   - 影响防御模式的警戒范围

5. **攻击距离** (`attack_range`)
   - 默认值：2格
   - 可以发动攻击的最小距离
   - 影响战斗和防御模式

### 行为模式切换
1. **传送机制**
   - 保持当前行为模式不变
   - 清空当前战斗目标
   - 传送到主人位置
   - 进入短暂冷却时间（10 tick）
   - 冷却结束后重新搜索目标

2. **目标切换**
   - 战斗模式：优先选择最近的敌对生物
   - 防御模式：优先选择接近主人的威胁
   - 目标生命值过低时主动切换新目标
   - 切换行为模式时重置目标

### 注意事项
1. 修改移动速度应在 `classes.yml` 中对应职阶下配置
2. 距离相关参数统一在 `config.yml` 中的 `servants` 部分配置
3. 英灵在所有状态下都会保持移动的流畅性和一致性
4. 传送和目标切换机制确保英灵不会脱离主人太远

## 英灵攻击机制实现

英灵攻击采用模拟主人普攻的方式实现,以确保 AttributePlus 的属性计算能够正确生效。具体实现步骤如下:

1. 获取主人的 AttributePlus 数据
```java
AttributeData ownerData = AttributeAPI.getAttrData(owner);
```

2. 保存主人原有属性
```java
Map<String, AttributeSource> originalSources = new HashMap<>(ownerData.getApiSourceAttribute());
```

3. 清除主人当前所有属性
```java
ownerData.clearApiAttribute();
```

4. 创建英灵属性源
```java
Map<String, Number[]> servantAttributes = new HashMap<>();
// 映射英灵属性到 AttributePlus 属性
attributes.getAll().forEach((key, value) -> {
    String mappedAttr = provider.getMappedAttributeName(key);
    if (mappedAttr != null) {
        double attrValue = value;
        servantAttributes.put(mappedAttr, new Number[]{attrValue, attrValue});
    }
});
```

5. 将英灵属性添加到主人身上
```java
AttributeSource servantSource = AttributeAPI.createStaticAttributeSource(
    new HashMap<>(servantAttributes), 
    new HashMap<>()
);
ownerData.operationApiAttribute("servant_attributes", servantSource, 
    AttributeSource.OperationType.ADD, true);
```

6. 让主人攻击目标,使 AttributePlus 的属性生效
```java
targetMonster.damage(attackValue, owner);
```

7. 1tick 后更新属性
```java
Bukkit.getScheduler().runTaskLater(FateServants.getInstance(), () -> {
    ownerData.updateAttribute(false);
    AttributeAPI.updateAttribute(owner);
}, 1L);
```

这种实现方式确保了:
- 英灵攻击时使用的是英灵的属性而不是主人的属性
- AttributePlus 的所有属性计算(暴击、吸血等)都能正确生效
- 攻击结束后主人的属性能够正确恢复

## 技能系统

### 技能类型
- **主动技能**: 需要特定条件触发，消耗魔法值使用
- **被动技能**: 根据概率自动触发，可能有冷却时间限制

### 技能配置
技能在classes.yml中为每个职阶单独配置：
```yaml
skills:
  技能ID:
    name: "技能名称"
    cast_type: "active/passive"  # 施法类型
    mana_cost: 10.0  # 魔法值消耗
    cooldown: 100    # 冷却时间(tick)
    chance: 100.0    # 触发概率(被动技能)
    description: "技能描述"
```

### 技能检测机制
- 主动技能通过独立任务每秒检测一次触发条件
- 被动技能在战斗或防御状态下自动检测

## 升级系统
- 英灵通过战斗获取经验值
- 经验获取公式和升级所需经验可在config.yml中配置
- 升级会提升英灵的所有属性
- 属性提升受品质和成长资质影响

## 目录结构

当前项目文件结构如下：

```
src/
├── main/
    ├── java/
    │   └── cn/i7mc/fateservants/
    │       ├── FateServants.java
    │       ├── ai/
    │       │   ├── ServantAIController.java
    │       │   └── ServantBehavior.java
    │       ├── attributes/
    │       │   ├── AttributeContainer.java
    │       │   ├── AttributeManager.java
    │       │   ├── AttributeModifier.java
    │       │   ├── ServantQuality.java
    │       │   └── provider/
    │       │       ├── AttributePlusProvider.java
    │       │       └── AttributeProvider.java
    │       ├── commands/
    │       │   ├── FSCommand.java
    │       │   └── FSTabCompleter.java
    │       ├── config/
    │       │   ├── ClassesConfig.java
    │       │   └── StatsConfig.java
    │       ├── database/
    │       │   ├── DatabaseManager.java
    │       │   └── LocalStorageManager.java
    │       ├── gui/
    │       │   ├── ServantGUI.java
    │       │   └── ServantGUIListener.java
    │       ├── listeners/
    │       │   ├── MonsterTargetListener.java
    │       │   └── PlayerJoinListener.java
    │       ├── manager/
    │       │   ├── ServantAIManager.java
    │       │   ├── ServantClassManager.java
    │       │   ├── ServantManager.java
    │       │   └── SkinManager.java
    │       ├── model/
    │       │   ├── Servant.java
    │       │   └── ServantClass.java
    │       ├── packets/
    │       │   └── PacketHandler.java
    │       └── skills/
    │           ├── Skill.java
    │           ├── SkillInfo.java
    │           └── SkillManager.java
    └── resources/
        ├── classes.yml
        ├── config.yml
        ├── gui.yml
        ├── message.yml
        ├── plugin.yml
        ├── save.yml
        └── stats.yml
```

### 功能模块说明

#### 1. 主类
- `FateServants.java` - 项目的主入口类，负责初始化和启动服务

#### 2. 功能模块

##### AI系统 (`ai/`)
- 实现英灵的智能行为控制
- 包含跟随、战斗、防御等行为模式
- 负责目标选择和决策逻辑

##### 属性系统 (`attributes/`)
- 负责处理角色和物品的属性相关功能
- 包含属性定义、计算和管理

##### 命令系统 (`commands/`)
- 实现游戏内的命令处理功能
- 包含各类游戏指令的具体实现

##### 配置系统 (`config/`)
- 处理项目的配置文件
- 管理各种游戏参数和设置

##### 数据库模块 (`database/`)
- 负责数据的持久化存储
- 管理数据库连接和操作
- 支持MySQL数据存储

##### 图形界面 (`gui/`)
- 实现游戏的用户界面
- 包含各种UI组件和交互功能

##### 监听器 (`listeners/`)
- 实现事件监听和处理
- 处理玩家加入、怪物目标等事件

##### 管理器 (`manager/`)
- 提供各种游戏系统的管理功能
- 统一管理游戏资源和状态

##### 数据模型 (`model/`)
- 定义游戏中的各种数据结构
- 包含实体类和数据对象

##### 网络通信 (`packets/`)
- 处理网络通信相关功能
- 实现数据包的收发和处理
- 通过ProtocolLib实现虚拟实体

##### 技能系统 (`skills/`)
- 实现游戏中的技能系统
- 包含技能定义和效果处理

### 配置文件 (`src/main/resources/`)
- `plugin.yml` - 插件基本信息、命令和权限定义
- `config.yml` - 主要配置文件，包含数据库、英灵基础设置等
- `message.yml` - 消息显示配置文件
- `classes.yml` - 职阶配置文件，定义各职阶属性和技能
- `gui.yml` - GUI界面配置文件
- `stats.yml` - 属性统计配置文件
- `save.yml` - 数据保存配置文件

### 技术栈
- 编程语言：Java
- 插件平台：Bukkit/Spigot
- 项目管理工具：Maven
- 配置格式：YAML
- 网络通信：ProtocolLib
- 属性系统：AttributePlus(可选)

## 开发环境
- IDE支持：VS Code（.vscode/）
- 构建输出：target/
- Minecraft版本：1.12+
