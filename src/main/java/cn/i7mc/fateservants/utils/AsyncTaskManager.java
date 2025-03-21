package cn.i7mc.fateservants.utils;

import cn.i7mc.fateservants.FateServants;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 异步任务管理器，用于处理异步操作
 */
public class AsyncTaskManager {
    private final FateServants plugin;
    private final Map<String, BukkitTask> runningTasks = new HashMap<>();
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public AsyncTaskManager(FateServants plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 执行异步任务
     * @param task 任务
     * @return 完成后的Future
     */
    public <T> CompletableFuture<T> runAsync(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    T result = task.get();
                    // 在主线程完成Future
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            future.complete(result);
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    // 在主线程完成Future(异常)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            future.completeExceptionally(e);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * 执行异步任务(无返回值)
     * @param task 任务
     * @return 完成后的Future
     */
    public CompletableFuture<Void> runAsyncTask(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    task.run();
                    // 在主线程完成Future
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            future.complete(null);
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    // 在主线程完成Future(异常)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            future.completeExceptionally(e);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }
    
    /**
     * 执行定时异步任务
     * @param name 任务名称
     * @param task 任务
     * @param delay 延迟(ticks)
     * @param period 周期(ticks)
     */
    public void runAsyncRepeating(String name, Runnable task, long delay, long period) {
        cancelTask(name);
        
        BukkitTask bukkitTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().severe("异步任务 " + name + " 执行出错: " + e.getMessage());
                    DebugUtils.log("async.task_error", name, e.getMessage());
                    e.printStackTrace();
                    cancel();
                    runningTasks.remove(name);
                }
            }
        }.runTaskTimerAsynchronously(plugin, delay, period);
        
        runningTasks.put(name, bukkitTask);
        DebugUtils.log("async.task_scheduled", name, delay, period);
    }
    
    /**
     * 延迟执行任务
     * @param name 任务名称
     * @param task 任务
     * @param delay 延迟(ticks)
     */
    public void runLater(String name, Runnable task, long delay) {
        cancelTask(name);
        
        BukkitTask bukkitTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    task.run();
                    runningTasks.remove(name);
                } catch (Exception e) {
                    plugin.getLogger().severe("延迟任务 " + name + " 执行出错: " + e.getMessage());
                    DebugUtils.log("async.task_error", name, e.getMessage());
                    e.printStackTrace();
                    runningTasks.remove(name);
                }
            }
        }.runTaskLater(plugin, delay);
        
        runningTasks.put(name, bukkitTask);
        DebugUtils.log("async.task_scheduled_once", name, delay);
    }
    
    /**
     * 取消任务
     * @param name 任务名称
     */
    public void cancelTask(String name) {
        BukkitTask task = runningTasks.remove(name);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            DebugUtils.log("async.task_cancelled", name);
        }
    }
    
    /**
     * 取消所有任务
     */
    public void cancelAll() {
        for (Map.Entry<String, BukkitTask> entry : runningTasks.entrySet()) {
            BukkitTask task = entry.getValue();
            if (task != null && !task.isCancelled()) {
                task.cancel();
                DebugUtils.log("async.task_cancelled", entry.getKey());
            }
        }
        runningTasks.clear();
        DebugUtils.log("async.all_tasks_cancelled");
    }
    
    /**
     * 检查任务是否运行中
     * @param name 任务名称
     * @return 是否运行中
     */
    public boolean isTaskRunning(String name) {
        return runningTasks.containsKey(name) && !runningTasks.get(name).isCancelled();
    }
    
    /**
     * 获取运行中任务数量
     * @return 任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }
} 