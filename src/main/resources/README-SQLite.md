# FateServants SQLite存储实现

## 概述

本文档介绍了FateServants插件中使用Spigot 1.12.2内置SQLite驱动的数据存储实现。该实现可以实时保存英灵数据，确保服务器崩溃或异常关闭时不会丢失数据。

## 设计目标

1. 使用Spigot 1.12.2内置的SQLite驱动
2. 实现英灵数据的实时保存和加载
3. 优化数据存储结构，提高读写效率
4. 提供与MySQL的兼容性，可自由切换存储方式
5. 增强错误处理和备份机制

## 技术实现

### 数据库连接

采用Spigot内置的SQLite JDBC驱动：

```java
Class.forName("org.sqlite.JDBC");
String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
connection = DriverManager.getConnection(url);
```

### 数据库表结构

1. **servants表** - 存储英灵基本信息

```sql
CREATE TABLE IF NOT EXISTS servants (
    owner_uuid VARCHAR(36) PRIMARY KEY, 
    servant_uuid VARCHAR(36) NOT NULL, 
    class_id VARCHAR(36) NOT NULL, 
    level DOUBLE NOT NULL, 
    experience INT NOT NULL, 
    health DOUBLE NOT NULL, 
    mana DOUBLE NOT NULL, 
    world VARCHAR(36) NOT NULL, 
    x DOUBLE NOT NULL, 
    y DOUBLE NOT NULL, 
    z DOUBLE NOT NULL, 
    yaw FLOAT NOT NULL, 
    pitch FLOAT NOT NULL, 
    is_following BOOLEAN NOT NULL, 
    is_attacking BOOLEAN NOT NULL, 
    is_defending BOOLEAN NOT NULL, 
    summon_time BIGINT NOT NULL
)
```

2. **servant_attributes表** - 存储英灵属性信息

```sql
CREATE TABLE IF NOT EXISTS servant_attributes (
    servant_uuid VARCHAR(36) NOT NULL, 
    attribute_key VARCHAR(36) NOT NULL, 
    base_value DOUBLE NOT NULL, 
    growth_rate DOUBLE NOT NULL, 
    PRIMARY KEY (servant_uuid, attribute_key)
)
```

### 数据存储流程

1. **保存英灵数据**
   - 使用事务确保数据一致性
   - 先保存基本信息到servants表
   - 删除旧的属性记录
   - 保存新的属性记录到servant_attributes表
   - 提交事务

2. **加载英灵数据**
   - 根据玩家UUID从servants表读取基本信息
   - 根据英灵UUID从servant_attributes表读取属性信息
   - 重建英灵对象及其所有属性

3. **删除英灵数据**
   - 使用事务确保数据一致性
   - 先删除servant_attributes表中的记录
   - 再删除servants表中的记录
   - 提交事务

### 错误处理和备份机制

1. **备份机制**
   - 当数据库操作失败时，自动回退到本地文件(YAML)存储
   - 确保在任何情况下数据都不会丢失

2. **事务处理**
   - 所有数据库操作都在事务中执行
   - 出现异常时自动回滚事务
   - 有效防止数据不一致

3. **调试信息**
   - 所有数据库操作都有详细的调试日志
   - 通过debugmessage.yml配置调试信息格式
   - 配置中的debug选项控制是否输出调试信息

## 配置选项

在`config.yml`中新增了数据库类型选项：

```yaml
# 数据库设置
database:
  # 数据库类型: sqlite 或 mysql
  type: sqlite
  # MySQL设置 (仅在type为mysql时使用)
  host: localhost
  port: 3306
  database: fateservants
  username: root
  password: password
```

## 性能考虑

1. **连接池**
   - 当前实现使用单一连接
   - 未来可考虑实现简单的连接池，提高并发性能

2. **批量操作**
   - 使用JDBC批处理功能优化大量数据写入
   - 属性保存采用批处理方式，提高效率

3. **索引优化**
   - 主键和外键已经建立了索引
   - 未来可根据查询模式优化索引结构

## 兼容性和扩展性

1. **兼容性**
   - 完全兼容原有的本地文件存储方式
   - 可随时在SQLite和MySQL之间切换
   - 支持在运行时重新加载数据库配置

2. **扩展性**
   - 表结构设计考虑了未来功能扩展
   - 可轻松添加新字段支持新功能
   - 代码结构清晰，便于维护和扩展

## 使用方法

1. 在`config.yml`中设置`database.type: sqlite`
2. 重启服务器或使用`/fs reload`命令重载插件
3. 插件会自动创建SQLite数据库文件和相关表结构
4. 所有英灵数据将自动保存到SQLite数据库中

## 调试方法

1. 在`config.yml`中设置`debug: true`
2. 查看控制台输出的调试信息
3. 调试信息格式可在`debugmessage.yml`中配置 