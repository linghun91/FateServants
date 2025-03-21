package cn.i7mc.fateservants.manager;

import cn.i7mc.fateservants.FateServants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkinManager {
    private final FateServants plugin;
    private final File cacheFile;
    private final Map<String, SkinData> skinCache;
    private static final long CACHE_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24小时

    public SkinManager(FateServants plugin) {
        this.plugin = plugin;
        this.cacheFile = new File(plugin.getDataFolder(), "skin_cache.yml");
        this.skinCache = new ConcurrentHashMap<>();
        loadCache();
    }

    private void loadCache() {
        if (!cacheFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cacheFile);
        for (String skinName : config.getKeys(false)) {
            if (config.contains(skinName + ".value") && config.contains(skinName + ".signature")) {
                String value = config.getString(skinName + ".value");
                String signature = config.getString(skinName + ".signature");
                long timestamp = config.getLong(skinName + ".timestamp", 0);
                skinCache.put(skinName, new SkinData(value, signature, timestamp));
            }
        }
    }

    private void saveCache() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, SkinData> entry : skinCache.entrySet()) {
            String path = entry.getKey();
            SkinData data = entry.getValue();
            config.set(path + ".value", data.getValue());
            config.set(path + ".signature", data.getSignature());
            config.set(path + ".timestamp", data.getTimestamp());
        }

        try {
            config.save(cacheFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存皮肤缓存: " + e.getMessage());
        }
    }

    public SkinData getSkin(String skinName) {
        // 1. 检查缓存
        SkinData cachedData = skinCache.get(skinName);
        if (cachedData != null && !isCacheExpired(cachedData)) {
            return cachedData;
        }

        // 2. 从Mojang API获取新数据
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + skinName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
                String uuid = jsonObject.get("id").getAsString();

                URL sessionURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                try (BufferedReader sessionReader = new BufferedReader(new InputStreamReader(sessionURL.openStream()))) {
                    JsonObject sessionProfile = new JsonParser().parse(sessionReader).getAsJsonObject();
                    JsonArray properties = sessionProfile.getAsJsonArray("properties");

                    for (JsonElement property : properties) {
                        JsonObject propertyObj = property.getAsJsonObject();
                        if (propertyObj.get("name").getAsString().equals("textures")) {
                            String value = propertyObj.get("value").getAsString();
                            String signature = propertyObj.get("signature").getAsString();

                            // 更新缓存
                            SkinData newData = new SkinData(value, signature, System.currentTimeMillis());
                            skinCache.put(skinName, newData);
                            saveCache();
                            return newData;
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("获取皮肤数据失败: " + e.getMessage());
            // 如果有缓存数据，无论是否过期都继续使用
            if (cachedData != null) {
                plugin.getLogger().info("使用缓存的皮肤数据: " + skinName);
                return cachedData;
            }
            plugin.getLogger().warning("没有找到缓存的皮肤数据，将使用默认皮肤: " + skinName);
        }

        // 3. 如果获取失败且没有缓存，返回默认皮肤
        return getDefaultSkin();
    }

    private boolean isCacheExpired(SkinData data) {
        return System.currentTimeMillis() - data.getTimestamp() > CACHE_EXPIRE_TIME;
    }

    private SkinData getDefaultSkin() {
        return new SkinData(
            "ewogICJ0aW1lc3RhbXAiIDogMTcwODUzNzE5ODY4MywKICAicHJvZmlsZUlkIiA6ICIxZWUxZGI4YTJiMmM0YjI3YWQ5NWM2ZDBkNjAxNTE2ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTYWJlciIsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yNTRiZDI1ZWFiMjQ4NjQ3ODFjZGY2MWFmYzJiOTJiYTM3NzRhNjQ5ZDQ5MzRhZGJmOGI4ODVlYzU4NTg0ZDQiCiAgICB9CiAgfQp9",
            "",
            System.currentTimeMillis()
        );
    }

    public static class SkinData {
        private final String value;
        private final String signature;
        private final long timestamp;

        public SkinData(String value, String signature, long timestamp) {
            this.value = value;
            this.signature = signature;
            this.timestamp = timestamp;
        }

        public String getValue() {
            return value;
        }

        public String getSignature() {
            return signature;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
