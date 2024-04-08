package com.eyunpan.component;

import com.eyunpan.tuple.ITuple2;
import org.assertj.core.groups.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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

    public boolean zaddScore(String key,V value,int score){
        try {
            redisTemplate.opsForZSet().addIfAbsent(key,value,0);
            redisTemplate.opsForZSet().incrementScore(key,value,score);
        }catch (Exception e){
            logger.error("zadd error");
            return false;
        }
        return true;
    }

    public void zremove(String key,V value){
        try {
            redisTemplate.opsForZSet().remove(key,value);
        }catch (Exception e){
            logger.error("zremove error");
        }
    }

    public List<ITuple2> zrevrangeWithScore(String key,long start,long end){
        ArrayList<ITuple2> tuple2s = new ArrayList<>();
        try {
            Set<ZSetOperations.TypedTuple<V>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end-1);
            for (ZSetOperations.TypedTuple<V> tuple:typedTuples){
                tuple2s.add(ITuple2.valueOf((String)tuple.getValue(),tuple.getScore().intValue()));
            }
        }catch (Exception e){
            logger.error("zrevrangeWithScore error");
        }
        return tuple2s;
    }

    public Integer zscore(String key,V value){
        Double score=null;
        try {
            score = redisTemplate.opsForZSet().score(key, value);
        }catch (Exception e){
            logger.error("zscore error");
        }
        return score==null?0:score.intValue();
    }
}
