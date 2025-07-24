package com.example.ratelimiter.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高性能本地缓存 - 基于Caffeine核心设计
 * 
 * 核心特性：
 * 1. 非阻塞清理机制
 * 2. 自适应清理触发
 * 3. 线程安全设计
 * 4. 内存占用控制
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 * @author yourkin666
 * @version 2.3.0 - Optimized
 */
public class LocalCache<K, V> {

    // ================== 核心常量 ==================

    /** 清理触发阈值 - 每64次操作检查一次 */
    private static final int CLEANUP_THRESHOLD = 64;

    /** 默认最大缓存容量 */
    private static final int DEFAULT_MAX_SIZE = 1000;

    // ================== 缓存节点 ==================

    /**
     * 缓存节点 - 轻量级设计
     */
    private static class Node<V> {
        final V value;
        final long expireTime;

        Node(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        boolean isExpired(long now) {
            return now > expireTime;
        }
    }

    // ================== 核心数据 ==================

    /** 数据存储 */
    private final ConcurrentHashMap<K, Node<V>> data = new ConcurrentHashMap<>();

    /** 操作计数器 */
    private final AtomicLong opCount = new AtomicLong();

    /** 清理锁 */
    private final Lock cleanupLock = new ReentrantLock();

    /** 过期时间(毫秒) */
    private final long ttl;

    /** 最大容量 */
    private final int maxSize;

    /** 上次清理时间 */
    private volatile long lastCleanTime;

    // ================== 构造函数 ==================

    public LocalCache(long ttl) {
        this(ttl, DEFAULT_MAX_SIZE);
    }

    public LocalCache(long ttl, int maxSize) {
        this.ttl = ttl;
        this.maxSize = maxSize;
        this.lastCleanTime = System.currentTimeMillis();
    }

    // ================== 核心API ==================

    /**
     * 获取数据
     */
    public V get(K key) {
        Node<V> node = data.get(key);
        if (node == null) {
            return null;
        }

        long now = System.currentTimeMillis();

        // 检查过期
        if (node.isExpired(now)) {
            data.remove(key);
            // 读操作也可能触发清理（低频）
            if (opCount.incrementAndGet() % (CLEANUP_THRESHOLD * 4) == 0) {
                tryCleanup();
            }
            return null;
        }

        return node.value;
    }

    /**
     * 存储数据
     */
    public void put(K key, V value) {
        long now = System.currentTimeMillis();
        Node<V> node = new Node<>(value, now + ttl);

        data.put(key, node);

        // 每64次操作触发清理检查
        if (opCount.incrementAndGet() % CLEANUP_THRESHOLD == 0) {
            tryCleanup();
        }
    }


    // ================== 清理机制 ==================

    /**
     * 尝试清理 - 非阻塞
     */
    private void tryCleanup() {
        if (cleanupLock.tryLock()) {
            try {
                cleanup();
            } finally {
                cleanupLock.unlock();
            }
        }
    }

    /**
     * 执行清理
     */
    private void cleanup() {
        long now = System.currentTimeMillis();

        // 防止过频清理
        if (now - lastCleanTime < ttl / 4) {
            return;
        }

        // 清理过期数据
        removeExpired(now);

        // 控制容量
        if (data.size() > maxSize) {
            removeOldest();
        }

        lastCleanTime = now;
    }

    /**
     * 移除过期数据
     */
    private void removeExpired(long now) {
        data.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * 移除最老的数据 - 简单的容量控制策略
     */
    private void removeOldest() {
        int targetSize = maxSize * 8 / 10; // 目标80%容量

        while (data.size() > targetSize && !data.isEmpty()) {
            try {
                // 移除第一个找到的元素（简化策略）
                K firstKey = data.keys().nextElement();
                data.remove(firstKey);
            } catch (Exception e) {
                // 并发情况下可能为空，直接跳出
                break;
            }
        }
    }
    
}