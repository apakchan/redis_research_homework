package com.leran.chapter1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class ChapterOneHomework {
    private static final Jedis jedis = new Jedis("192.168.10.128", 6379);
    /**
     * 定义一个用户信息结构体，使用 fastjson 对用户信息对象进行序列化和反序列化
     * 使用 jedis 对 redis 缓存的用户信息进行存取
     */
    private static void homework1() {
        User user = new User(1L, "cafebabe", "idonntkonw");
        String userKeyName = "userKey";

        jedis.hset(userKeyName, "id", String.valueOf(user.getId()));
        jedis.hset(userKeyName, "userName", user.getUserName());
        jedis.hset(userKeyName, "password", user.getPassword());

        Map<String, String> userMap = jedis.hgetAll(userKeyName);
        String userMapJsonString = JSONObject.toJSONString(userMap);
        User finalUser = JSONObject.parseObject(userMapJsonString, User.class);
        System.out.println(finalUser);
    }

    private static void homework3() {
        /*
            如果用 hash 结构缓存用户信息，我觉得中间需要用一个过度对象存储，这个过度对象里面的 field 对象必须全是 String 类型
            而且这个用户信息不能过大，不然集群模式下节点间传递这个对象数据会出现卡顿。
         */
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @ToString
    private static class User {
        private Long id;
        private String userName;
        private String password;
    }

    public static void main(String[] args) {
//        homework1();
        System.out.println(jedis.ping());
    }

}
