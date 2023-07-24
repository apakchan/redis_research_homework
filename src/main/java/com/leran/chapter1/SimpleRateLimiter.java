package com.leran.chapter1;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SimpleRateLimiter {
    private Jedis jedis;

    public SimpleRateLimiter(Jedis jedis) {
        this.jedis = jedis;
    }

    /**
     *
     * @param userId     userId
     * @param actionKey  行为 key
     * @param period     时间窗口大小
     * @param maxCount   最大尝试数
     * @return
     */
    public boolean isActionAllowed(String userId, String actionKey, int period, int maxCount) {
        String key = String.format("hist:%s:%s", userId, actionKey);
        long nowTs = System.currentTimeMillis();
        Pipeline pipeline = jedis.pipelined();
        pipeline.multi();
        pipeline.zadd(key, nowTs, "" + nowTs);
        // 移除窗口之前的行为记录, 剩下的都是窗口内的
        pipeline.zremrangeByScore(key, 0, nowTs - period * 1000L);
        // 计算窗口内的行为数量
        Response<Long> count = pipeline.zcard(key);
        // 设置 zset 过期时间, 避免冷用户占内存
        // 过期时间比窗口大一点
        pipeline.expire(key, period + 1);
        pipeline.exec();
        pipeline.close();
        return count.get() <= maxCount;
    }

    public static void main(String[] args) throws InterruptedException {
        Jedis jedis = new Jedis("192.168.10.128", 6379);
        SimpleRateLimiter simpleRateLimiter = new SimpleRateLimiter(jedis);
        for (int i = 0; i < 20; i++) {
            TimeUnit.SECONDS.sleep(new Random().nextInt(3));
            System.out.println(simpleRateLimiter.isActionAllowed("1314", "action", 5, 5));
        }
    }
}
