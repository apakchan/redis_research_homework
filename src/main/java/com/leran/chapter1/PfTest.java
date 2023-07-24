package com.leran.chapter1;

import redis.clients.jedis.Jedis;

import java.util.Random;

public class PfTest {
    private static void testPfCount() {
        Jedis jedis = new Jedis("192.168.10.128", 6379);
//        for (int i = 0; i < 100000; i++) {
//            jedis.pfadd("codehole", "user" + i);
//        }
        long pfcount = jedis.pfcount("codehole");
        // 99725
        System.out.println("100000 tries pfcount: " + pfcount);
        jedis.close();
    }

    private static void testPfMerge() {
        Jedis jedis = new Jedis("192.168.10.128", 6379);
        for (int i = 0; i < 33333; i++) {
            jedis.pfadd("codehole1", "user" + i);
        }
        for (int i = 33333; i < 66666; i++) {
            jedis.pfadd("codehole2", "user" + i);
        }
        for (int i = 66666; i < 100000; i++) {
            jedis.pfadd("codehole3", "user" + i);
        }
        jedis.pfmerge("destCodeHole", "codehole1", "codehole2", "codehole3");
        // 99725
        System.out.println(jedis.pfcount("destCodeHole"));
        jedis.close();
    }

    public static void main(String[] args) {
        testPfMerge();
    }
}
