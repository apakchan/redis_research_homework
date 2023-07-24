package com.leran.chapter1;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

public class RedissonDemo {

    public static void main(String[] args) {
        // 创建 Redisson 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.10.128:6379");

        // 创建 Redisson 客户端
        RedissonClient redisson = Redisson.create(config);

        // 获取分布式锁
        RLock lock = redisson.getLock("my-lock");
        try {
            // 尝试获取锁，最多等待 10 秒
            boolean lockAcquired = lock.tryLock(20, TimeUnit.SECONDS);
            if (lockAcquired) {
                try {
                    // 执行业务逻辑
                    System.out.println("Lock acquired, performing business logic...");
                    Thread.sleep(10000); // 模拟业务逻辑执行时间
                    boolean lockAcquired1 = lock.tryLock(10, TimeUnit.SECONDS);
                    if (lockAcquired1) {
                        try {
                            System.out.println("relock");
                            Thread.sleep(10000);
                        } finally {
                            lock.unlock();
                        }
                    }
                } finally {
                    // 释放锁
                    lock.unlock();
                }
            } else {
                System.out.println("Failed to acquire lock");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭 Redisson 客户端
            redisson.shutdown();
        }
    }
}
