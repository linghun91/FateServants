package cn.i7mc.fateservants;

import cn.i7mc.fateservants.attributes.AttributeManager;
import cn.i7mc.fateservants.attributes.ServantQuality;
import cn.i7mc.fateservants.attributes.provider.AttributePlusProvider;
import cn.i7mc.fateservants.attributes.provider.AttributeProvider;
import cn.i7mc.fateservants.commands.FSCommand;
import cn.i7mc.fateservants.commands.FSTabCompleter;
import cn.i7mc.fateservants.database.DatabaseManager;
import cn.i7mc.fateservants.gui.GUIManager;
import cn.i7mc.fateservants.gui.ServantGUI;
import cn.i7mc.fateservants.gui.ServantGUIListener;
import cn.i7mc.fateservants.listeners.MonsterTargetListener;
import cn.i7mc.fateservants.listeners.PlayerJoinListener;
import cn.i7mc.fateservants.manager.ServantAIManager;
import cn.i7mc.fateservants.manager.ServantClassManager;
import cn.i7mc.fateservants.manager.ServantManager;
import cn.i7mc.fateservants.manager.SkinManager;
import cn.i7mc.fateservants.model.Servant;
import cn.i7mc.fateservants.packets.PacketHandler;
import cn.i7mc.fateservants.skills.SkillManager;
import cn.i7mc.fateservants.utils.DebugUtils;
import cn.i7mc.fateservants.utils.FormatUtils;
import cn.i7mc.fateservants.utils.MessageManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.i7mc.fateservants.config.ConfigManager;
import cn.i7mc.fateservants.gui.GUIConfigManager;
import cn.i7mc.fateservants.utils.MaterialAdapter;

public final class FateServants extends JavaPlugin {
    private static FateServants instance;
    private ProtocolManager protocolManager;
    private DatabaseManager databaseManager;
    private ServantManager servantManager;
    private PacketHandler packetHandler;
    private AttributeManager attributeManager;
    private ServantClassManager servantClassManager;
    private ExecutorService battleThreadPool;
    private FileConfiguration messageConfig;
    private FileConfiguration debugMessageConfig;
    private ServantGUI servantGUI;
    private ServantAIManager aiManager;
    private SkinManager skinManager;
    private SkillManager skillManager;
    private FileConfiguration classesConfig;
    private AttributePlusProvider attributePlusProvider;
    private ExecutorService battleExecutor;
    private GUIManager guiManager;
    private ConfigManager configManager;
    private GUIConfigManager guiConfigManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Initializing FateServants plugin...");
        
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化配置文件
        getLogger().info("Loading configuration files...");
        saveDefaultConfig();
        saveResource("message.yml", false);
        saveResource("classes.yml", false);
        saveResource("stats.yml", false);
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化DebugUtils
        DebugUtils.init(this);
        
        // 初始化MessageManager
        MessageManager.init(this);
        
        // 初始化消息管理器
        getLogger().info("Initializing message manager...");
        // 加载配置文件
        getLogger().info("Loading message.yml...");
        messageConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "message.yml"));
        
        // 加载调试消息配置文件
        File debugMessageFile = new File(getDataFolder(), "debugmessage.yml");
        if (!debugMessageFile.exists()) {
            saveResource("debugmessage.yml", false);
        }
        debugMessageConfig = YamlConfiguration.loadConfiguration(debugMessageFile);
        
        // 加载classes.yml配置
        getLogger().info("Loading classes.yml...");
        reloadClassesConfig();
        
        // 输出调试信息
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Debug mode enabled");
            DebugUtils.debug("system.startup");
            DebugUtils.debug("plugin.load_config", "config.yml");
            DebugUtils.debug("plugin.load_config", "message.yml");
            DebugUtils.debug("plugin.load_config", "debugmessage.yml");
            DebugUtils.debug("plugin.load_config", "classes.yml");
            DebugUtils.debug("plugin.load_config", "stats.yml");
        }
        
        // 加载品质配置
        getLogger().info("Loading quality configuration...");
        ServantQuality.loadQualities(getConfig().getConfigurationSection("qualities"));
        
        // 初始化协议管理器
        getLogger().info("Initializing protocol manager...");
        protocolManager = ProtocolLibrary.getProtocolManager();
        
        // 初始化战斗线程池
        getLogger().info("Initializing battle thread pool...");
        battleThreadPool = Executors.newFixedThreadPool(4);
        
        // 初始化属性管理器
        getLogger().info("Initializing attribute manager...");
        attributeManager = new AttributeManager(this);
        
        // 初始化AttributePlus提供者
        getLogger().info("Initializing AttributePlus provider...");
        attributePlusProvider = new AttributePlusProvider(this);
        
        // 注册AttributePlus提供者
        AttributeProvider provider = attributePlusProvider;
        if (provider.isAvailable()) {
            getLogger().info("Registering AttributePlus provider...");
        } else {
            getLogger().warning("AttributePlus provider is not available.");
        }
        
        // 初始化职阶管理器
        getLogger().info("Initializing servant class manager...");
        servantClassManager = new ServantClassManager(this);
        getLogger().info("Loading servant classes...");
        servantClassManager.loadClasses();
        
        // 初始化英灵管理器
        getLogger().info("Initializing servant manager...");
        servantManager = new ServantManager(this);
        
        // 初始化AI管理器
        getLogger().info("Initializing AI manager...");
        aiManager = new ServantAIManager(this);
        
        // 初始化皮肤管理器
        getLogger().info("Initializing skin manager...");
        skinManager = new SkinManager(this);
        
        // 初始化技能管理器
        getLogger().info("Initializing skill manager...");
        skillManager = new SkillManager(this);
        
        // 初始化新GUI管理器
        getLogger().info("Initializing GUI manager...");
        guiManager = new GUIManager(this);
        
        // 启动AI管理器
        getLogger().info("Starting AI update task...");
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Servant servant : servantManager.getAllServants()) {
                servant.updateAI();
            }
        }, 1L, 1L); // 每tick更新一次
        
        // 启动主动技能检测任务
        getLogger().info("Starting active skill detection task...");
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Servant servant : servantManager.getAllServants()) {
                if (servant.isAttacking() || servant.isDefending()) {  // 只在战斗或防御状态检测
                    LivingEntity target = servant.getAIController().getCurrentTarget();
                    if (target != null && !target.isDead() && target.isValid()) {
                        servant.checkAndTriggerActiveSkills(target);
                    }
                }
            }
        }, 20L, 20L); // 每秒检测一次
        
        // 初始化数据包处理器
        packetHandler = new PacketHandler(this);
        
        // 初始化GUI
        servantGUI = new ServantGUI(this);
        getServer().getPluginManager().registerEvents(servantGUI, this);
        getServer().getPluginManager().registerEvents(new ServantGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new MonsterTargetListener(this), this);
        
        // 注册命令
        FSCommand fsCommand = new FSCommand(this);
        getCommand("fs").setExecutor(fsCommand);
        getCommand("fs").setTabCompleter(new FSTabCompleter(this));
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        // 初始化材质适配器
        MaterialAdapter.init(this);

        // 初始化格式化工具
        FormatUtils.init(this, configManager);

        // 初始化GUI配置管理器
        guiConfigManager = new GUIConfigManager(this, configManager);
        
        // 初始化数据库管理器
        getLogger().info("Initializing database manager...");
        databaseManager = new DatabaseManager(this);
        
        // 启动插件初始化后加载所有数据
        getServer().getScheduler().runTaskLater(this, () -> {
            // 从数据库加载所有在线玩家的英灵数据
            if (servantManager != null) {
                getLogger().info("Loading all online players' servant data...");
                servantManager.loadAll();
            }
        }, 40L); // 延迟2秒(40ticks)后加载，确保服务器完全启动
        
        getLogger().info("FateServants plugin initialization complete!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down FateServants plugin...");
        
        // 输出调试信息
        if (getConfig().getBoolean("debug", false)) {
            DebugUtils.debug("plugin.reload");
        }
        
        // 停止AI管理器
        if (aiManager != null) {
            aiManager.stop();
        }
        
        // 关闭线程池
        if (battleExecutor != null) {
            battleExecutor.shutdown();
        }
        
        // 保存所有英灵数据
        if (servantManager != null) {
            servantManager.saveAll();
        }
        
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("FateServants plugin shutdown complete!");
    }

    public static FateServants getInstance() {
        return instance;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public ServantManager getServantManager() {
        return servantManager;
    }
    
    public PacketHandler getPacketHandler() {
        return packetHandler;
    }
    
    public AttributeManager getAttributeManager() {
        return attributeManager;
    }
    
    public ServantClassManager getServantClassManager() {
        return servantClassManager;
    }
    
    public ExecutorService getBattleExecutor() {
        return battleExecutor;
    }

    public FileConfiguration getMessageConfig() {
        if (configManager == null) {
            // 如果configManager尚未初始化，直接从文件加载
            File messageFile = new File(getDataFolder(), "message.yml");
            if (!messageFile.exists()) {
                saveResource("message.yml", false);
            }
            return YamlConfiguration.loadConfiguration(messageFile);
        }
        return configManager.getConfig("message.yml");
    }
    
    /**
     * 获取调试消息配置
     * @return 调试消息配置
     */
    public FileConfiguration getDebugMessageConfig() {
        return debugMessageConfig;
    }

    public void reloadMessageConfig() {
        messageConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "message.yml"));
        File debugMessageFile = new File(getDataFolder(), "debugmessage.yml");
        if (!debugMessageFile.exists()) {
            saveResource("debugmessage.yml", false);
        }
        debugMessageConfig = YamlConfiguration.loadConfiguration(debugMessageFile);
        
        // 重新加载消息管理器
        MessageManager.reload();
    }

    public ServantGUI getServantGUI() {
        return servantGUI;
    }

    public ServantAIManager getAIManager() {
        return aiManager;
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public AttributePlusProvider getAttributePlusProvider() {
        return attributePlusProvider;
    }

    /**
     * 获取classes.yml配置
     */
    public FileConfiguration getClassesConfig() {
        if (classesConfig == null) {
            reloadClassesConfig();
        }
        return classesConfig;
    }
    
    /**
     * 重新加载classes.yml配置
     */
    public void reloadClassesConfig() {
        if (getConfig().getBoolean("debug", false)) {
            DebugUtils.debug("config.reload", "classes");
        }
        
        File configFile = new File(getDataFolder(), "classes.yml");
        if (!configFile.exists()) {
            getLogger().info("classes.yml not found, creating default file...");
            saveResource("classes.yml", false);
        }
        classesConfig = YamlConfiguration.loadConfiguration(configFile);
        getLogger().info("classes.yml reloaded successfully!");
        
        // 加载默认配置
        InputStream defConfigStream = getResource("classes.yml");
        if (defConfigStream != null) {
            classesConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream)));
        }
    }

    /**
     * 获取GUI管理器
     * @return GUI管理器
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * 获取配置管理器
     * @return 配置管理器实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取GUI配置管理器
     * @return GUI配置管理器实例
     */
    public GUIConfigManager getGUIConfigManager() {
        return guiConfigManager;
    }
} 