package org.example.cache;

import com.alibaba.fastjson2.JSON;
import org.apache.ibatis.builder.InitializingObject;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.example.model.JsonType;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

public class JsonTypeCache implements Cache, InitializingObject {

    private JedisPool pool;
    private final String id;

    public JsonTypeCache(String id) {
        this.id = id;
    }

    public void initialize() {
        pool = new JedisPool("localhost", 6379);
    }

    public String getId() {
        return id;
    }

    private String genCacheKeyForRedis(Object key) {
        CacheKey cacheKey = (CacheKey) key;
        return cacheKey.toString();
    }

    public void putObject(Object key, Object value) {
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = genCacheKeyForRedis(key);
            String jonsValue = JSON.toJSONString(value);
            jedis.set(cacheKey, jonsValue);
        }
    }

    public Object getObject(Object key) {
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = genCacheKeyForRedis(key);
            if (jedis.exists(cacheKey)) {
                String jonsValue = jedis.get(cacheKey);
                List<JsonType> jsonTypeList = JSON.parseArray(jonsValue, JsonType.class);
                System.out.println(jonsValue);
                return jsonTypeList;
            }
        }
        return null;
    }

    public Object removeObject(Object key) {
        System.out.println("removeObject");
        return null;
    }

    public void clear() {
        System.out.println("clear");
    }

    public int getSize() {
        return 0;
    }
}
