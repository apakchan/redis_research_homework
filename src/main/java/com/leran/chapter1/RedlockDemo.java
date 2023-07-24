package com.leran.chapter1;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;

public class RedlockDemo {

    private static final int LOCK_EXPIRE_TIME = 30000; // 锁的过期时间，单位毫秒
    private static final int LOCK_RETRY_TIMES = 3; // 获取锁的重试次数
    private static final int LOCK_RETRY_INTERVAL = 100; // 获取锁的重试间隔，单位毫秒

    private static final String LOCK_KEY = "my-lock"; // 锁的名称

    private static final JedisPool jedisPool;

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);

        jedisPool = new JedisPool(poolConfig, "192.168.10.128", 6379);
    }

    public static void main(String[] args) {
        try (Jedis jedis = jedisPool.getResource()) {
            // 获取锁
            String lockValue = acquireLock(jedis);
            if (lockValue != null) {
                try {
                    // 执行业务逻辑
                    System.out.println("Lock acquired, performing business logic...");
                    Thread.sleep(5000); // 模拟业务逻辑执行时间
                } finally {
                    // 释放锁
                    releaseLock(jedis, lockValue);
                }
            } else {
                System.out.println("Failed to acquire lock");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedisPool.close();
        }
    }

    private static String acquireLock(Jedis jedis) {
        String lockValue = Thread.currentThread().getId() + "-" + System.currentTimeMillis();
        for (int i = 0; i < LOCK_RETRY_TIMES; i++) {
            String result = jedis.set(LOCK_KEY, lockValue, new SetParams().nx().px(LOCK_EXPIRE_TIME));
            if ("OK".equals(result)) {
                return lockValue;
            }
            try {
                Thread.sleep(LOCK_RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    private static void releaseLock(Jedis jedis, String lockValue) {
        String script = " if redis.call('get', KEYS[1]) == ARGV[1]" +
                        "     then return redis.call('del', KEYS[1])" +
                        " else return 0" +
                        " end";
        jedis.eval(script,
                Collections.singletonList(LOCK_KEY),
                Collections.singletonList(Thread.currentThread().getId() + "-" + System.currentTimeMillis()));
    }
}

