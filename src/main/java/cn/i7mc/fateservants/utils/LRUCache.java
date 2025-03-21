package cn.i7mc.fateservants.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) 缓存实现
 * 当缓存满时，移除最久未使用的元素
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class LRUCache<K, V> {
    private final LinkedHashMap<K, V> cache;
    private final int capacity;
    
    /**
     * 创建LRU缓存
     * @param capacity 缓存容量
     */
    public LRUCache(int capacity) {
        this.capacity = capacity;
        // 创建一个具有访问顺序的LinkedHashMap
        // accessOrder设为true表示按访问顺序排序，而不是插入顺序
        this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                // 当大小超过容量时，返回true表示移除最老的元素
                return size() > LRUCache.this.capacity;
            }
        };
    }
    
    /**
     * 获取缓存中的值
     * @param key 键
     * @return 值，如果不存在则返回null
     */
    public synchronized V get(K key) {
        return cache.get(key);
    }
    
    /**
     * 将值放入缓存
     * @param key 键
     * @param value 值
     */
    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }
    
    /**
     * 检查键是否存在
     * @param key 键
     * @return 是否存在
     */
    public synchronized boolean containsKey(K key) {
        return cache.containsKey(key);
    }
    
    /**
     * 从缓存中移除键
     * @param key 键
     * @return 被移除的值，如果不存在则返回null
     */
    public synchronized V remove(K key) {
        return cache.remove(key);
    }
    
    /**
     * 清空缓存
     */
    public synchronized void clear() {
        cache.clear();
    }
    
    /**
     * 获取缓存大小
     * @return 缓存大小
     */
    public synchronized int size() {
        return cache.size();
    }
    
    /**
     * 获取缓存容量
     * @return 缓存容量
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * 获取所有缓存键
     * @return 键集合
     */
    public synchronized Iterable<K> keys() {
        return new LinkedHashMap<>(cache).keySet();
    }
    
    /**
     * 获取所有缓存内容
     * @return 所有缓存项
     */
    public synchronized Iterable<Map.Entry<K, V>> entries() {
        return new LinkedHashMap<>(cache).entrySet();
    }
} 