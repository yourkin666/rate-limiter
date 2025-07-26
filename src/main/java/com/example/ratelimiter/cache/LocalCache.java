package com.example.ratelimiter.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级本地缓存 - 限流场景专用
 * 
 * 特性：
 * 1. 懒清理机制 - 仅在访问时检查过期
 * 2. 线程安全设计
 * 3. 零维护开销
 * 
 * @param <K> 键类型
 * @param <V> 值类型
 * @author yourkin666
 * @version 3.0.0 - Simplified
 */
public class LocalCache<K, V> {

    /**
     * 缓存节点
     */
    private static class Node<V> {
        final V value;
        final long expireTime;

        Node(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /** 数据存储 */
    private final ConcurrentHashMap<K, Node<V>> data = new ConcurrentHashMap<>();

    /** 过期时间(毫秒) */
    private final long ttl;

    public LocalCache(long ttl) {
        this.ttl = ttl;
    }

    /**
     * 获取数据
     */
    public V get(K key) {
        Node<V> node = data.get(key);
        if (node == null) {
            return null;
        }

        if (node.isExpired()) {
            data.remove(key);
            return null;
        }

        return node.value;
    }

    /**
     * 存储数据
     */
    public void put(K key, V value) {
        long expireTime = System.currentTimeMillis() + ttl;
        data.put(key, new Node<>(value, expireTime));
    }
}