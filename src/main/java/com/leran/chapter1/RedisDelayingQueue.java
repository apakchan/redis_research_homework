package com.leran.chapter1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

// 延时队列
public class RedisDelayingQueue <T>{
    static class TaskItem<T> {
        public String id;
        public T msg;
    }

    // fastjson 序列化对象中存在泛型时，需要使用 TypeReference
    private Type taskType = new TypeReference<TaskItem<T>>() {}.getType();

    private Jedis jedis;
    private String queueKey;

    public RedisDelayingQueue(Jedis jedis, String queueKey) {
        this.jedis = jedis;
        this.queueKey = queueKey;
    }

    public void delay(T msg) {
        TaskItem<T> taskItem = new TaskItem<>();
        taskItem.id = UUID.randomUUID().toString();
        taskItem.msg = msg;

        String json = JSON.toJSONString(taskItem);
        // 延时队列延时 5s
        jedis.zadd(queueKey, System.currentTimeMillis() + 5000, json);
    }

    public void loop() {
        while (!Thread.interrupted()) {
            Set<String> tasks = jedis.zrangeByScore(queueKey, 0, System.currentTimeMillis(), 0, 1);
            if (tasks.isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                continue;
            }
            tasks.forEach(task -> {
                // 成功从 queueKey 中移除 task
                if (jedis.zrem(queueKey, task) > 0) {
                    TaskItem<T> taskItem = JSON.parseObject(task, taskType);
                    this.handleMsg(taskItem.msg);
                }
            });
        }
    }

    public void loopV2() {
        String luaScript = "local members = redis.call('ZRANGEBYSCORE', KEYS[1], ARGV[1], ARGV[2], 'LIMIT', ARGV[3], ARGV[4])\n" +
                "if #members == 0 then\n" +
                "    return {}\n" +
                "else\n" +
                "    redis.call('ZREM', KEYS[1], members[1])\n" +
                "    return members\n" +
                "end";
        while (!Thread.interrupted()) {
            String min = String.valueOf(0);
            String max = String.valueOf(System.currentTimeMillis());
            String offset = String.valueOf(0);
            String count = String.valueOf(1);
            List<String> members = (List<String>) jedis.eval(luaScript,
                    Collections.singletonList(queueKey),
                    Arrays.asList(min, max, offset, count));
            if (members == null || members.isEmpty()) {
                try {
                    // 0.5s
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                continue;
            }
            members.forEach(task -> {
                System.out.println(task);
                TaskItem<T> taskItem = JSON.parseObject(task, taskType);
                this.handleMsg(taskItem.msg);
            });
        }
    }

    public void handleMsg(T msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis("192.168.10.128", 6379);
        final RedisDelayingQueue<String> queue = new RedisDelayingQueue<>(jedis, "queue");
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                queue.delay("cafebabe-" + i);
            }
        }, "producer");

        Thread consumer = new Thread(queue::loopV2, "consumer");
        producer.start();
        consumer.start();
        try {
            producer.join();
            Thread.sleep(6000);
            consumer.interrupt();
            consumer.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
