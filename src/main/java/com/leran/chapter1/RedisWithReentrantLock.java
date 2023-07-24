package com.leran.chapter1;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.HashMap;
import java.util.Map;

public class RedisWithReentrantLock {
    private final ThreadLocal<Map<String, Integer>> lockers = new ThreadLocal<>();

    private Jedis jedis;

    public RedisWithReentrantLock(Jedis jedis) {
        this.jedis = jedis;
    }

    private boolean _lock(String key) {
        // nx 代表如果有这个 key 那么无法 set, 抢锁失败
        return this.jedis.set(key, "", new SetParams().nx().ex(5L)) != null;
    }

    private void _unlock(String key) {
        this.jedis.del(key);
    }

    private Map<String, Integer> currentLockers() {
        Map<String, Integer> refs = lockers.get();
        if (refs != null) {
            return refs;
        }
        Map<String, Integer> newRefs = new HashMap<>();
        lockers.set(newRefs);
        return newRefs;
    }

    public boolean lock(String key) {
        Map<String, Integer> refs = currentLockers();
        Integer ref = refs.get(key);
        if (ref != null) {
            refs.put(key, ref + 1);
            return true;
        }
        boolean lockRes = this._lock(key);
        if (!lockRes) {
            return false;
        }
        refs.put(key, 1);
        return true;
    }

    public boolean unlock(String key) {
        Map<String, Integer> refs = currentLockers();
        Integer ref = refs.get(key);
        if (ref != null) {
            if (ref == 1) {
                refs.remove(key);
                this._unlock(key);
            } else {
                refs.put(key, ref - 1);
            }
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis("192.168.10.128", 6379);
        RedisWithReentrantLock test = new RedisWithReentrantLock(jedis);
        System.out.println(test.lock("code"));
        System.out.println(test.lock("code"));
        System.out.println(test.unlock("code"));
        System.out.println(test.unlock("code"));
        System.out.println(test.unlock("code"));
    }
}
