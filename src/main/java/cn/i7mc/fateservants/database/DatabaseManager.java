package cn.i7mc.fateservants.database;

import cn.i7mc.fateservants.FateServants;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.model.ServantClass;
import cn.i7mc.fateservants.utils.MessageManager;
import cn.i7mc.fateservants.utils.DebugUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private final FateServants plugin;
    private Connection connection;
    private LocalStorageManager localStorageManager;
    private boolean useLocalStorage;
    private boolean useSQLite;
    
    // SQL语句常量
    private static final String CREATE_SERVANTS_TABLE = 
            "CREATE TABLE IF NOT EXISTS servants (" +
            "owner_uuid VARCHAR(36) PRIMARY KEY, " +
            "servant_uuid VARCHAR(36) NOT NULL, " +
            "class_id VARCHAR(36) NOT NULL, " +
            "level DOUBLE NOT NULL, " +
            "experience INT NOT NULL, " +
            "health DOUBLE NOT NULL, " +
            "mana DOUBLE NOT NULL, " +
            "world VARCHAR(36) NOT NULL, " +
            "x DOUBLE NOT NULL, " +
            "y DOUBLE NOT NULL, " +
            "z DOUBLE NOT NULL, " +
            "yaw FLOAT NOT NULL, " +
            "pitch FLOAT NOT NULL, " +
            "is_following BOOLEAN NOT NULL, " +
            "is_attacking BOOLEAN NOT NULL, " +
            "is_defending BOOLEAN NOT NULL, " +
            "summon_time BIGINT NOT NULL)";
    
    private static final String CREATE_ATTRIBUTES_TABLE = 
            "CREATE TABLE IF NOT EXISTS servant_attributes (" +
            "servant_uuid VARCHAR(36) NOT NULL, " +
            "attribute_key VARCHAR(36) NOT NULL, " +
            "base_value DOUBLE NOT NULL, " +
            "growth_rate DOUBLE NOT NULL, " +
            "PRIMARY KEY (servant_uuid, attribute_key))";
    
    private static final String INSERT_SERVANT = 
            "INSERT OR REPLACE INTO servants VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String SELECT_SERVANT = 
            "SELECT * FROM servants WHERE owner_uuid = ?";
    
    private static final String DELETE_SERVANT = 
            "DELETE FROM servants WHERE owner_uuid = ?";
    
    private static final String INSERT_ATTRIBUTE = 
            "INSERT OR REPLACE INTO servant_attributes VALUES (?, ?, ?, ?)";
    
    private static final String SELECT_ATTRIBUTES = 
            "SELECT * FROM servant_attributes WHERE servant_uuid = ?";
    
    private static final String DELETE_ATTRIBUTES = 
            "DELETE FROM servant_attributes WHERE servant_uuid = ?";

    public DatabaseManager(FateServants plugin) {
        this.plugin = plugin;
        this.localStorageManager = new LocalStorageManager(plugin);
        
        String storageType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        
        if (storageType.equals("sqlite")) {
            useSQLite = true;
            try {
                connectSQLite();
                useLocalStorage = false;
                createTables();
                plugin.getLogger().info("成功连接到SQLite数据库");
            } catch (SQLException e) {
                plugin.getLogger().warning("SQLite数据库连接失败，将使用本地存储: " + e.getMessage());
                useLocalStorage = true;
            }
        } else if (storageType.equals("mysql")) {
            useSQLite = false;
            try {
                connectMySQL();
                useLocalStorage = false;
                createTables();
                plugin.getLogger().info("成功连接到MySQL数据库");
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL数据库连接失败，将使用本地存储: " + e.getMessage());
                useLocalStorage = true;
            }
        } else {
            plugin.getLogger().warning("不支持的数据库类型: " + storageType + "，将使用本地存储");
            useLocalStorage = true;
        }
    }

    private void connectSQLite() throws SQLException {
        // 使用Spigot 1.12.2内置的SQLite驱动
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("找不到SQLite JDBC驱动", e);
        }
        
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, "database.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        DebugUtils.debug("database.connect_sqlite", url);
        connection = DriverManager.getConnection(url);
        
        // 启用外键约束
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
    }

    private void connectMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "fateservants");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true", host, port, database);
        connection = DriverManager.getConnection(url, username, password);
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_SERVANTS_TABLE);
            stmt.execute(CREATE_ATTRIBUTES_TABLE);
        }
    }

    public void saveServant(Player owner, Servant servant) {
        if (useLocalStorage) {
            localStorageManager.saveServant(owner, servant);
            return;
        }
        
        String ownerUuid = owner.getUniqueId().toString();
        String servantUuid = servant.getUuid().toString();
        Location loc = servant.getLocation();
        
        try {
            // 开始事务
            connection.setAutoCommit(false);
            
            // 保存英灵基本信息
            try (PreparedStatement pstmt = connection.prepareStatement(INSERT_SERVANT)) {
                pstmt.setString(1, ownerUuid);
                pstmt.setString(2, servantUuid);
                pstmt.setString(3, servant.getServantClass().getId());
                pstmt.setDouble(4, servant.getLevel());
                pstmt.setInt(5, servant.getExperience());
                pstmt.setDouble(6, servant.getCurrentHealth());
                pstmt.setDouble(7, servant.getCurrentMana());
                pstmt.setString(8, loc.getWorld().getName());
                pstmt.setDouble(9, loc.getX());
                pstmt.setDouble(10, loc.getY());
                pstmt.setDouble(11, loc.getZ());
                pstmt.setFloat(12, loc.getYaw());
                pstmt.setFloat(13, loc.getPitch());
                pstmt.setBoolean(14, servant.isFollowing());
                pstmt.setBoolean(15, servant.isAttacking());
                pstmt.setBoolean(16, servant.isDefending());
                pstmt.setLong(17, servant.getSummonTime());
                pstmt.executeUpdate();
            }
            
            // 删除旧的属性记录
            try (PreparedStatement pstmt = connection.prepareStatement(DELETE_ATTRIBUTES)) {
                pstmt.setString(1, servantUuid);
                pstmt.executeUpdate();
            }
            
            // 保存属性信息
            try (PreparedStatement pstmt = connection.prepareStatement(INSERT_ATTRIBUTE)) {
                Map<String, Double> attributes = servant.getAttributes().getAll();
                Map<String, Double> growths = servant.getAttributeGrowths();
                
                // 首先确保保存核心属性，包括movement_speed
                String[] coreAttributes = {"health", "mana", "movement_speed"};
                for (String coreAttr : coreAttributes) {
                    double baseValue = 0.0;
                    if (coreAttr.equals("movement_speed")) {
                        baseValue = servant.getServantClass().getBaseMovementSpeed();
                    } else {
                        baseValue = servant.getAttributes().getBaseValue(coreAttr);
                    }
                    double growth = growths.getOrDefault(coreAttr, 0.0);
                    
                    pstmt.setString(1, servantUuid);
                    pstmt.setString(2, coreAttr);
                    pstmt.setDouble(3, baseValue);
                    pstmt.setDouble(4, growth);
                    pstmt.addBatch();
                    
                    DebugUtils.debug("database.save_attribute", 
                        coreAttr, baseValue, growth);
                }
                
                // 然后保存其他属性
                for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                    String attrKey = entry.getKey();
                    // 跳过已处理的核心属性
                    if (attrKey.equals("health") || attrKey.equals("mana") || attrKey.equals("movement_speed")) {
                        continue;
                    }
                    
                    double baseValue = servant.getAttributes().getBaseValue(attrKey);
                    double growth = growths.getOrDefault(attrKey, 0.0);
                    
                    pstmt.setString(1, servantUuid);
                    pstmt.setString(2, attrKey);
                    pstmt.setDouble(3, baseValue);
                    pstmt.setDouble(4, growth);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            // 提交事务
            connection.commit();
            
            DebugUtils.debug("database.save_success", owner.getName());
            
        } catch (SQLException e) {
            try {
                // 回滚事务
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().severe("回滚事务失败: " + ex.getMessage());
            }
            
            plugin.getLogger().severe("保存英灵数据到SQLite数据库时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            // 如果数据库保存失败，使用本地存储作为备份
            localStorageManager.saveServant(owner, servant);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().severe("重置自动提交失败: " + e.getMessage());
            }
        }
    }

    public Servant loadServant(Player owner) {
        if (useLocalStorage) {
            return localStorageManager.loadServant(owner);
        }
        
        String ownerUuid = owner.getUniqueId().toString();
        
        try {
            // 查询英灵基本信息
            Servant servant = null;
            
            try (PreparedStatement pstmt = connection.prepareStatement(SELECT_SERVANT)) {
                pstmt.setString(1, ownerUuid);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String classId = rs.getString("class_id");
                        ServantClass servantClass = plugin.getServantClassManager().getClass(classId);
                        
                        if (servantClass == null) {
                            plugin.getLogger().warning("找不到职阶: " + classId);
                            return null;
                        }
                        
                        // 创建英灵实例
                        servant = new Servant(owner, servantClass);
                        servant.setUuid(UUID.fromString(rs.getString("servant_uuid")));
                        
                        // 设置基本属性
                        servant.setLevel(rs.getDouble("level"));
                        servant.setExperience(rs.getInt("experience"));
                        servant.setHealth(rs.getDouble("health"));
                        servant.setMana(rs.getDouble("mana"));
                        
                        // 设置位置
                        Location loc = new Location(
                            Bukkit.getWorld(rs.getString("world")),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                        );
                        servant.teleport(loc);
                        
                        // 设置行为状态
                        servant.setFollowing(rs.getBoolean("is_following"));
                        servant.setAttacking(rs.getBoolean("is_attacking"));
                        servant.setDefending(rs.getBoolean("is_defending"));
                        servant.setSummonTime(rs.getLong("summon_time"));
                    } else {
                        // 数据库中没有此玩家的英灵记录
                        return null;
                    }
                }
            }
            
            if (servant == null) {
                return null;
            }
            
            // 查询属性信息
            Map<String, Double> growths = new HashMap<>();
            
            try (PreparedStatement pstmt = connection.prepareStatement(SELECT_ATTRIBUTES)) {
                pstmt.setString(1, servant.getUuid().toString());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String attrKey = rs.getString("attribute_key");
                        double baseValue = rs.getDouble("base_value");
                        double growth = rs.getDouble("growth_rate");
                        
                        servant.getAttributes().setBaseValue(attrKey, baseValue);
                        growths.put(attrKey, growth);
                        
                        // 记录找到的movement_speed属性
                        if (attrKey.equals("movement_speed")) {
                            DebugUtils.debug("database.load_movement_speed", baseValue);
                        }
                    }
                }
            }
            
            // 设置成长资质
            servant.setAttributeGrowths(growths);
            
            // 确保加载了所有必要的核心属性，如果数据库中没有，则从职阶配置中加载
            String[] coreAttributes = {"health", "mana", "movement_speed"};
            for (String coreAttr : coreAttributes) {
                if (!servant.getAttributes().hasAttribute(coreAttr)) {
                    double baseValue = 0.0;
                    if (coreAttr.equals("movement_speed")) {
                        baseValue = servant.getServantClass().getBaseMovementSpeed();
                        DebugUtils.debug("database.use_class_movement_speed", baseValue);
                    } else if (coreAttr.equals("health")) {
                        baseValue = servant.getServantClass().getBaseHealth();
                    } else if (coreAttr.equals("mana")) {
                        baseValue = servant.getServantClass().getBaseMana();
                    }
                    servant.getAttributes().setBaseValue(coreAttr, baseValue);
                }
            }
            
            // 确保英灵的AI控制器被正确设置为跟随模式
            if (servant.getAIController() != null) {
                servant.getAIController().setBehavior(cn.i7mc.fateservants.ai.ServantBehavior.FOLLOW);
                DebugUtils.debug("database.ai_controller_set", 
                    owner.getName(),
                    servant.getServantClass() != null ? servant.getServantClass().getId() : "null");
            }
            
            DebugUtils.debug("database.load_success", owner.getName());
            return servant;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("从SQLite数据库加载英灵数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            // 如果数据库读取失败，尝试从本地存储加载
            return localStorageManager.loadServant(owner);
        }
    }

    public void removeServant(Player owner) {
        if (useLocalStorage) {
            localStorageManager.removeServant(owner);
            return;
        }
        
        String ownerUuid = owner.getUniqueId().toString();
        
        try {
            // 开始事务
            connection.setAutoCommit(false);
            
            // 先查询servant_uuid
            String servantUuid = null;
            try (PreparedStatement pstmt = connection.prepareStatement("SELECT servant_uuid FROM servants WHERE owner_uuid = ?")) {
                pstmt.setString(1, ownerUuid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        servantUuid = rs.getString("servant_uuid");
                    }
                }
            }
            
            if (servantUuid != null) {
                // 删除属性记录
                try (PreparedStatement pstmt = connection.prepareStatement(DELETE_ATTRIBUTES)) {
                    pstmt.setString(1, servantUuid);
                    pstmt.executeUpdate();
                }
            }
            
            // 删除英灵记录
            try (PreparedStatement pstmt = connection.prepareStatement(DELETE_SERVANT)) {
                pstmt.setString(1, ownerUuid);
                pstmt.executeUpdate();
            }
            
            // 提交事务
            connection.commit();
            
            DebugUtils.debug("database.remove_success", owner.getName());
            
        } catch (SQLException e) {
            try {
                // 回滚事务
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().severe("回滚事务失败: " + ex.getMessage());
            }
            
            plugin.getLogger().severe("从SQLite数据库删除英灵数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            // 如果数据库删除失败，使用本地存储作为备份
            localStorageManager.removeServant(owner);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().severe("重置自动提交失败: " + e.getMessage());
            }
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭");
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库连接时发生错误: " + e.getMessage());
            }
        }
        
        if (useLocalStorage) {
            localStorageManager.saveAll();
        }
    }

    public void reload() {
        // 关闭现有连接
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库连接时发生错误: " + e.getMessage());
            }
            connection = null;
        }
        
        // 重新读取配置并连接
        String storageType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        
        if (storageType.equals("sqlite")) {
            useSQLite = true;
            try {
                connectSQLite();
                useLocalStorage = false;
                createTables();
                plugin.getLogger().info("成功重新连接到SQLite数据库");
            } catch (SQLException e) {
                plugin.getLogger().warning("SQLite数据库重新连接失败，将使用本地存储: " + e.getMessage());
                useLocalStorage = true;
            }
        } else if (storageType.equals("mysql")) {
            useSQLite = false;
            try {
                connectMySQL();
                useLocalStorage = false;
                createTables();
                plugin.getLogger().info("成功重新连接到MySQL数据库");
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL数据库重新连接失败，将使用本地存储: " + e.getMessage());
                useLocalStorage = true;
            }
        } else {
            plugin.getLogger().warning("不支持的数据库类型: " + storageType + "，将使用本地存储");
            useLocalStorage = true;
        }
        
        // 如果使用本地存储，重载本地存储
        if (useLocalStorage) {
            localStorageManager.reload();
        }
    }
} 