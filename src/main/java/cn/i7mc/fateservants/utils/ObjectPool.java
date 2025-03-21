package cn.i7mc.fateservants.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 通用对象池，用于减少对象创建和GC压力
 * @param <T> 对象类型
 */
public class ObjectPool<T> {
    private final Queue<T> pool = new LinkedList<>();
    private final Supplier<T> factory;
    private final Consumer<T> reset;
    private final int maxSize;
    
    /**
     * 创建对象池
     * @param factory 对象创建工厂
     * @param reset 对象重置方法
     * @param maxSize 池最大大小
     */
    public ObjectPool(Supplier<T> factory, Consumer<T> reset, int maxSize) {
        this.factory = factory;
        this.reset = reset;
        this.maxSize = maxSize;
    }
    
    /**
     * 获取对象
     * @return 对象实例
     */
    public T get() {
        T obj = pool.poll();
        if (obj == null) {
            obj = factory.get();
        }
        return obj;
    }
    
    /**
     * 归还对象
     * @param obj 对象实例
     */
    public void release(T obj) {
        if (obj == null) return;
        
        if (pool.size() < maxSize) {
            reset.accept(obj);
            pool.offer(obj);
        }
    }
    
    /**
     * 清空对象池
     */
    public void clear() {
        pool.clear();
    }
    
    /**
     * 获取当前池大小
     * @return 池大小
     */
    public int size() {
        return pool.size();
    }
    
    /**
     * 获取池最大大小
     * @return 池最大大小
     */
    public int getMaxSize() {
        return maxSize;
    }
} 