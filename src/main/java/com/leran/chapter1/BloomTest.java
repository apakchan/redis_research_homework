package com.leran.chapter1;


import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.clients.jedis.Client;

public class BloomTest {
    public static void main(String[] args) {
        // 创建RedissonClient实例
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.10.128:6379");
        RedissonClient redissonClient = Redisson.create(config);

        // 获取布隆过滤器实例
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("myBloomFilter");

        // 初始化布隆过滤器，设置期望插入的元素数量和误差率
        bloomFilter.tryInit(1000000L, 0.03);

        // 添加元素到布隆过滤器
        bloomFilter.add("element1");
        bloomFilter.add("element2");

        // 检查元素是否存在于布隆过滤器中
        boolean exists1 = bloomFilter.contains("element1");
        boolean exists2 = bloomFilter.contains("element2");
        boolean exists3 = bloomFilter.contains("element3");

        System.out.println("Element 1 exists: " + exists1);
        System.out.println("Element 2 exists: " + exists2);
        System.out.println("Element 3 exists: " + exists3);

        // 关闭RedissonClient实例
        redissonClient.shutdown();
    }
}
