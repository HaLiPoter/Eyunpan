package com.eyunpan.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component("redisUtils")
public class RedisUtils<V> {

    @Resource
    private RedisTemplate<String,V> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    public void delete(String... key){
        try {
            if (key!=null&&key.length>0){
                if (key.length==1){
                    redisTemplate.delete(key[0]);
                }else{
                    redisTemplate.delete(Arrays.asList(key));
                }
            }
        }catch (Exception e){
            logger.error("del error, key:{}",key);
        }
    }

    public V get(String key){
        return key==null?null:redisTemplate.opsForValue().get(key);
    }

    public boolean set(String key, V value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        }catch (Exception e){
            logger.error("set error, key:{}, value:{}",key,value);
            return false;
        }
    }

    public boolean setex(String key, V value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("setex error, key:{}, value:{}",key,value);
            return false;
        }
    }
}
