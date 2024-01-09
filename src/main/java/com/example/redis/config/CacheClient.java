package com.example.redis.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.redis.pojo.RedisData;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient (StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate =  stringRedisTemplate;
    }


    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(value),time,unit);
    }


    public void setWithLogincalExpire(String key, Object value, Long time, TimeUnit unit){
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(redisData),time,unit);
    }


    public <R,ID> R queryWithPassThrougn(String keyPrefix, ID id,Long time, TimeUnit unit, Class<R> type, Function<ID,R> dbFailBack){
        String key = keyPrefix + id;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (StringUtils.isNotBlank(json)){
            return JSONObject.parseObject(json,type);
        }

        if (json != null){
            return null;
        }

        R r = dbFailBack.apply(id);
        if (r == null){
            this.setWithLogincalExpire(key,"",time, unit);
            return null;
        }

        this.setWithLogincalExpire(key,JSON.toJSONString(r),time, unit);

        return r;
    }


    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R,ID> R queryWithLogincalExpire(String keyPrefix,ID id,Long time, TimeUnit unit, Class<R> type, Function<ID,R> dbFailBack){
        String key = keyPrefix + id;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (StringUtils.isBlank(json)){
            return null;
        }

        RedisData redisData = JSONObject.parseObject(json,RedisData.class);
        R r = JSONObject.parseObject(JSON.toJSONString(redisData.getData()),type);

        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime.isAfter(LocalDateTime.now())){
            return r;
        }

        String lockKey = "LOCK_KEY_OF" + key;
        boolean isLock = tryLock(lockKey);

        if (isLock){
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFailBack.apply(id);

                    this.setWithLogincalExpire(key,JSON.toJSONString(r),time, unit);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }
            });
        }

        return r;
    }


    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtils.isTrue(flag);
    }


    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
