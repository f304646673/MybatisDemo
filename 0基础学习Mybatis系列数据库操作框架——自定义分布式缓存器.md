Mysql这类的数据库，其查询性能往往不能100%扛住我们业务请求量。于是我们一般都会在查询数据库之前，先查询下缓存。如果缓存存在，则直接使用缓存中数据；如果缓存失效，则读取数据库，并将数据记录到缓存中。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8bf8fc110955470d83ad2b08f2f240de.png)
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9478b784a9d04de0ab0112dcd7bc92cf.png)
Mybatis有缓存机制，但是它只是本地缓存。在分布式环境下，这套机制就有很大的限制，于是本文我们将缓存内容保存在Redis上，这样部分于不同机器上的Mybatis都可以使用一个缓存库。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/18676721a33d4ee7b4b693c6c67c0c23.png)
# 依赖
我们将使用Fastjson将Java对象序列化，然后保存在Redis中。
```xml
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>2.0.48</version>
        </dependency>
```
Redis的连接库使用jedis

```bash
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>5.1.2</version>
        </dependency>
```
# 缓存器类
缓存器需要实现org.apache.ibatis.cache.Cache。

```java
package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

public interface Cache {
    String getId();

    void putObject(Object key, Object value);

    Object getObject(Object key);

    Object removeObject(Object key);

    void clear();

    int getSize();

    default ReadWriteLock getReadWriteLock() {
        return null;
    }
}
```
我们主要要实现putObject和getObject方法。
当我们向数据库发送Select请求时，会调用getObject方法。在这个方法中，我们可以查询自己的缓存。如果缓存中查到了数据，就构造对象直接返回，这样Mybatis就不会查询数据库了，直接用了我们缓存的数据；如果缓存不存在，则该函数返回null。Mybatis就会访问数据库。

```java
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
```
当数据库返回数据时，Mybatis会调用putObject通知我们将数据缓存起来。

```java
    public void putObject(Object key, Object value) {
        try (Jedis jedis = pool.getResource()) {
            String cacheKey = genCacheKeyForRedis(key);
            String jonsValue = JSON.toJSONString(value);
            jedis.set(cacheKey, jonsValue);
        }
    }
```
上例中，pool是缓存器类的成员变量。它会在缓存器属性设置完毕后构造。这个调用过程也要借助Mybatis框架——缓存器类需要继承于org.apache.ibatis.builder.InitializingObject，并实现initialize方法。

```java
public class JsonTypeCache implements Cache, InitializingObject {
……
	public void initialize() {
        pool = new JedisPool("localhost", 6379);
    }
……
```
完整代码如下

```java
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
```
需要注意的是genCacheKeyForRedis方法，它用于生成Redis的key。本例中的实现直接用了CacheKey的序列化方法。在分布式环境下，这个方法是否可以针对相同SQL和参数生成相同Key值，是需要进一步验证的。
# 配置
在SQL Mapper XML中新增如下项
```xml
    <cache type="org.example.cache.JsonTypeCache"/>
```
对于需要使用Cache的Select语句，新增useCache属性

```xml
    <select id="selectJsonTypeElems" resultMap="jsonTypeResultMap" useCache = "true">
        select * from all_type where info_int = #{intInfo}
    </select>
```
Update、Delete、Insert这类操作都会导致数据库变动，进而会影响Select的结果。这样缓存就会与数据库中数据不一致。一种办法是给这类语句加上flushCache属性，这样这些指令调用时，会调用缓存器的clear方法（本例中我们给这个方法填充有意义的操作）。我们可以在这个方法中删除所有缓存。这个方法的粒度太大了，所以并不推荐。一种更好的方法是借用后面介绍的拦截器，有针对性的清除缓存，而不是清除所有缓存。
```xml
    <delete id="deleteJsonTypeElems" flushCache = "true">
        delete from all_type where info_int = #{intInfo}
    </delete>
```
完整XML见下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- AllTypeMapperJsonCache.xml -->
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.example.mapper.AllTypeMapper">
    <parameterMap id="jsonTypeParameterMap" type="JsonType">
        <parameter property="jsonElemList" jdbcType="LONGVARCHAR" typeHandler="org.example.typehandlers.JsonListHandler"/>
    </parameterMap>

    <resultMap id="jsonTypeResultMap" type="JsonType">
        <result property="intInfo" column="info_int"/>
        <result property="jsonElemList" column="info_ltext" jdbcType="LONGVARCHAR" typeHandler="org.example.typehandlers.JsonListHandler"/>
    </resultMap>

    <insert id="insertJsonTypeElems" flushCache = "true">
        insert into all_type(info_int, info_ltext) values
        <foreach item="item" collection="list" separator=",">
            (#{item.intInfo}, #{item.jsonElemList, typeHandler=org.example.typehandlers.JsonListHandler})
        </foreach>
    </insert>

    <update id="updateJsonTypeElems" flushCache = "true">
        update all_type set info_ltext  = #{jsonElemList, jdbcType=LONGVARCHAR} where info_int = #{intInfo}
    </update>

    <cache type="org.example.cache.JsonTypeCache"/>

    <select id="selectJsonTypeElems" resultMap="jsonTypeResultMap" useCache = "true">
        select * from all_type where info_int = #{intInfo}
    </select>

    <delete id="deleteJsonTypeElems" flushCache = "true">
        delete from all_type where info_int = #{intInfo}
    </delete>
</mapper>
```
# 测试

```java
package org.example;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.AllTypeMapper;
import org.example.model.AllType;
import org.example.model.JsonType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CacheTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-json-cache.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    void testUpdate() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            JsonType a = new JsonType();
            a.setIntInfo(1);

            List<JsonType.JsonElem> jsonElemList = Arrays.asList(
                    new JsonType.JsonElem(1,"1"),
                    new JsonType.JsonElem(2,"2")
            );
            JsonType.JsonList jsonList = new JsonType.JsonList(jsonElemList);

            a.setJsonElemList(jsonList);
            long count = all_type_mapper.updateJsonTypeElems(a);
            System.out.println(count);
        }
    }


    @Test
    void testSelect() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            List<JsonType> all = all_type_mapper.selectJsonTypeElems(100);
            for (JsonType a : Objects.requireNonNull(all)) {
                JsonType.JsonList jsonList = a.getJsonElemList();
                if (null == jsonList) {
                    continue;
                }
                for (JsonType.JsonElem b: jsonList.getJsonElemList()) {
                    System.out.printf("%d %s\n", b.getFirst(), b.getSecond());
                }
            }
        }
    }


    @Test
    void testinsertJsonTypeElems() {
        List<JsonType> jsonTypeList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            JsonType a = new JsonType();
            a.setIntInfo(i+100);

            List<JsonType.JsonElem> jsonElemList = Arrays.asList(
                    new JsonType.JsonElem(i+1100, "1"),
                    new JsonType.JsonElem(i+1200, "2")
            );
            JsonType.JsonList jsonList = new JsonType.JsonList(jsonElemList);
            a.setJsonElemList(jsonList);
            jsonTypeList.add(a);
        }


        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.insertJsonTypeElems(jsonTypeList);
            System.out.println(count);
        }
    }

    @Test
    void testDelete() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.deleteJsonTypeElems(110);
            System.out.println(count);
        }
    }
}
```

# 总结

 - 继承org.apache.ibatis.cache.Cache接口，主要实现putObject和getObject方法。
 - getObject返回null时，Mybatis会查询数据库；getObject返回对象时，Mybatis直接返回该对象，而不会查询数据库。
 - 当Mybatis查询数据库后，会调用putObject方法，让我们有保存数据到缓存的机会。
 - 实现org.apache.ibatis.builder.InitializingObject接口，让缓存器在构造时有我们自定义的初始化的机会。
 - 需要在SQL Mapper XML中新增<cache type="org.example.cache.JsonTypeCache"/>标签，告知Mybatis这个Mapper中的缓存器是哪个。
 - 缓存器从属于Mapper，不同Mapper可以设置不同缓存器。
 - SQL Mapper XML中Select语句使用useCache = "true"表达这个SQL使用缓存器。
 - SQL Mapper XML中Update、Delete、Insert语句使用flushCache = "true"语句该SQL使用了缓存器，它的效果就是调用缓存器的clear方法。该方法没有任何参数，只能全部清空缓存。
 - 
代码样例见：[https://github.com/f304646673/mybatis_demo.git](https://github.com/f304646673/mybatis_demo.git)
# 参考资料
-  [https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html#cache](https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html#cache)
