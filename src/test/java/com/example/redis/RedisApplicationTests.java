package com.example.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.redis.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class RedisApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
        stringRedisTemplate.opsForValue().set("name","小贝");
        System.out.println(stringRedisTemplate.opsForValue().get("name"));
    }

    @Test
    void userText() {
        User user = new User("雪豹",20,"理塘");

        stringRedisTemplate.opsForValue().set("user:100", JSON.toJSONString(user));

        User user1 = JSONObject.parseObject(stringRedisTemplate.opsForValue().get("user:100"),User.class);

        System.out.println(user1.toString());
    }
}
