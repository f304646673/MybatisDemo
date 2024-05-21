在[《0基础学习Mybatis系列数据库操作框架——Mysql的Geometry数据处理之WKT方案》](https://fangliang.blog.csdn.net/article/details/139094495)中，我们介绍WTK方案的优点，也感受到它的繁琐和缺陷。比如：

- 需要借助ST_GeomFromText和ST_AsText，让SQL语句显得复杂。
```sql
select id, ST_AsText(geometry) AS geometry, update_time, create_time from geometry_data
```

- 没有一种GeomFromText方案可以覆盖所有的Geometry结构，使得类似的SQL要写多份。

```sql
insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())

insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomCollFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
```

-  没有针对LinearRing（一种特殊的LineString）的处理方法。

而本文介绍的WKB方法，则可以解决上述问题。
WKB全程Well-Known Binary，它是一种二进制存储几何信息的方法。
在[《0基础学习Mybatis系列数据库操作框架——Mysql的Geometry数据处理之WKT方案》](https://fangliang.blog.csdn.net/article/details/139094495)中介绍的WKT方法，可以用字符串形式表达几何信息，如POINT (1 -1)。
WKB则表达为

> 0101000000000000000000F03F000000000000F0BF

这段二进制的拆解如下
|Component|	Size	|Value|
|-|-|-|
|Byte order|	1 byte	|01|
|WKB type|	4 bytes	|01000000|
|X coordinate|	8 bytes	|000000000000F03F|
|Y coordinate|	8 bytes	|000000000000F0BF|

 byte order可以是0或者1，它表示是大顶堆（0）还是小顶堆（1）存储。
 WKB type表示几何类型。值的对应关系如下：
 - 1 Point
 - 2 LineString
 - 3 Polygon
 - 4 MultiPoint
 - 5 MultiLineString
 - 6 MultiPolygon
 - 7 GeometryCollection
 
剩下的是坐标信息。

虽然这个结构已经很基础，但是**Mysql的Geometry结构并不是WKB。准确的说，WKB只是Mysql的Geometry结构中的一部分。**它们的差异是，**Mysql的Geometry结构是在WKB之前加了4个字节，用于存储SRID。**
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/a11f0b29e24b495095fad57a335b32d9.png)
还有一点需要注意的是，Mysql存储Geometry数据使用的是小顶堆。所以WKB的Byte order字段值一定是1。
有了这些知识，我们就可以定义WKB类型的TypeHandler了。
# 序列化
这段代码先从org.locationtech.jts.geom.Geometry中获取SRID码；然后以小顶堆模式，使用WKBWriter将几何信息保存为WKB的二进制码。然后申请比WKB大4个字节的空间，分别填入SRID和WKB。这样整个内存结构就匹配Mysql内部的Geometry内存结构了。
```java
    private byte[] serializeGeometry(Geometry geometry) {
        int srid = geometry.getSRID();
        byte[] bytes = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(geometry);
        return ByteBuffer.allocate(bytes.length + 4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(srid)
                .put(bytes)
                .array();
    }
```
# 反序列化
这段代码会将Mysql内部的Geometry内存结构读出来，转换成小顶堆模式。然后获取SRID，并以此创建GeometryFactory。剩下的内容就是WKB的内存了，最后使用WKBReader将这段内存转换成org.locationtech.jts.geom.Geometry。
```java
    private static Geometry deserializeGeometry(byte[] bytes) throws ParseException {
        if (bytes == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int srid = buffer.getInt();
        byte[] geometryBytes = new byte[buffer.remaining()];
        buffer.get(geometryBytes);

        GeometryFactory geometryFactory = GEOMETRY_FACTORIES.computeIfAbsent(srid, i -> new GeometryFactory(PRECISION_MODEL, i));

        WKBReader reader = new WKBReader(geometryFactory);
        return reader.read(geometryBytes);
    }
```
# 完整TypeHandler

```java
package org.example.typehandlers;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeometryTypeWKBHandler extends BaseTypeHandler<Geometry>  {
    private static final PrecisionModel PRECISION_MODEL = new PrecisionModel(PrecisionModel.FIXED);
    private static final Map<Integer, GeometryFactory> GEOMETRY_FACTORIES = new ConcurrentHashMap<>();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        byte[] bytes = serializeGeometry(parameter);
        ps.setBytes(i, bytes);
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        try {
            return deserializeGeometry(bytes);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        try {
            return deserializeGeometry(bytes);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        try {
            return deserializeGeometry(bytes);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    private static Geometry deserializeGeometry(byte[] bytes) throws ParseException {
        if (bytes == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int srid = buffer.getInt();
        byte[] geometryBytes = new byte[buffer.remaining()];
        buffer.get(geometryBytes);

        GeometryFactory geometryFactory = GEOMETRY_FACTORIES.computeIfAbsent(srid, i -> new GeometryFactory(PRECISION_MODEL, i));

        WKBReader reader = new WKBReader(geometryFactory);
        return reader.read(geometryBytes);
    }

    private byte[] serializeGeometry(Geometry geometry) {
        int srid = geometry.getSRID();
        byte[] bytes = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(geometry);
        return ByteBuffer.allocate(bytes.length + 4).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(srid)
                .put(bytes)
                .array();
    }
}
```
# SQL XML
使用了WKB模式，SQL就会写的很简洁，而不需要用ST_GeomFromText和ST_AsText转来转去。比如之前因为要用ST_AsText处理返回值，导致需要写明每个返回的字段。而使用WKB后，可以写成
```xml
    <resultMap id="GeometryDataResultMap" type="org.example.model.GeometryData">
        <result property="id" column="id"/>
        <result property="geometry" column="geometry" typeHandler="org.example.typehandlers.GeometryTypeWKBHandler" jdbcType="BLOB"/>
        <result property="updateTime" column="update_time"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <select id="findAll" resultMap="GeometryDataResultMap">
        select * from geometry_data
    </select>
```
作为对比可以看下WKT的模式，如下。

```xml
    <resultMap id="GeometryDataResultMap" type="org.example.model.GeometryData">
        <result property="id" column="id"/>
        <result property="geometry" column="geometry" typeHandler="org.example.typehandlers.GeometryTypeWKTHandler" jdbcType="BLOB"/>
        <result property="updateTime" column="update_time"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <select id="findAll" resultMap="GeometryDataResultMap">
        select id, ST_AsText(geometry) AS geometry, update_time, create_time from geometry_data
    </select>
```
插入操作也会变得简单，下面是WKB模式

```xml
    <insert id="insertOne" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, #{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKBHandler}, now(), now())
    </insert>
```
而WKT模式，因为不能使用ST_GeomFromText处理GeometryCollection，导致只能拆成两条SQL。如下

```xml
    <insert id="insertOne" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
    </insert>

    <insert id="insertGeometryCollection" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomCollFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
    </insert>
```
可以见得WKB模式让SQL XML变得简单。
## 完整XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- AllTypeMapper-1.xml -->
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.example.mapper.GeometryDataWKBMapper">
    <resultMap id="GeometryDataResultMap" type="org.example.model.GeometryData">
        <result property="id" column="id"/>
        <result property="geometry" column="geometry" typeHandler="org.example.typehandlers.GeometryTypeWKBHandler" jdbcType="BLOB"/>
        <result property="updateTime" column="update_time"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <select id="findAll" resultMap="GeometryDataResultMap">
        select * from geometry_data
    </select>

    <select id="find" resultMap="GeometryDataResultMap">
        select * from geometry_data where id = #{id}
    </select>

    <insert id="insertOne" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, #{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKBHandler}, now(), now())
    </insert>

    <insert id="insertList" parameterType="list">
        insert into geometry_data(id, geometry, update_time, create_time) values
        <foreach item="item" collection="list" separator=",">
            (#{item.id}, #{item.geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKBHandler}, now(), now())
        </foreach>
    </insert>

    <update id="updateOne" parameterType="org.example.model.GeometryData">
        update geometry_data set geometry = #{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKBHandler}, update_time = now() where id = #{id} 
    </update>
</mapper>
```
# Mapper

```java
package org.example.mapper;

import java.util.List;

import org.example.model.GeometryData;

public interface GeometryDataWKBMapper {
    public List<GeometryData> findAll();
    public GeometryData find(Long id);
    public Long insertOne(GeometryData geometryData);
    public Long insertList(List<GeometryData> geometryDataList);
    public Long updateOne(GeometryData geometryData);
} 
```
# 测试代码
相较于WKT模式，我们给WKB模式的测试用例增加了LinearRing类型。这是WKT模式所不支持的。
```java
package org.example;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.GeometryDataWKBMapper;
import org.example.model.GeometryData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class GeometryDataWKBTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-geometry-wkb.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    public void testFindAll() {
        List<GeometryData> all = null;
        try (SqlSession session = sqlSF.openSession()) {
            all = session.getMapper(GeometryDataWKBMapper.class).findAll();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        for (GeometryData a : Objects.requireNonNull(all)) {
            System.out.println(a.getGeometry());
        }
    }

    @Test
    public void testFind() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);
            GeometryData one = GeometryDataWKBMapper.find(1L);
            System.out.println(one.getGeometry());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testInsert() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);
            GeometryData geometryData = new GeometryData();
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate coordinate = new Coordinate(1, 1);
            Geometry geometry = geometryFactory.createPoint(coordinate);
            geometryData.setGeometry(geometry);
            long count = GeometryDataWKBMapper.insertOne(geometryData);
            System.out.println(count);
            session.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testUpdate() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);
            GeometryData geometryData = new GeometryData();
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate coordinate = new Coordinate(2, 2);
            Geometry geometry = geometryFactory.createPoint(coordinate);
            geometryData.setId(1L);
            geometryData.setGeometry(geometry);
            long count = GeometryDataWKBMapper.updateOne(geometryData);
            System.out.println(count);
            session.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testInsertList() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);

            List<GeometryData> geometryDataList = new ArrayList<>();
            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                Coordinate coordinate = new Coordinate(3, 3);
                Geometry geometry = geometryFactory.createPoint(coordinate);
                geometryData.setGeometry(geometry);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                LineString lineString = geometryFactory
                        .createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) });
                geometryData.setGeometry(lineString);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                MultiLineString multiLineString = geometryFactory.createMultiLineString(new LineString[] {
                        geometryFactory
                                .createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
                        geometryFactory
                                .createLineString(new Coordinate[] { new Coordinate(3, 3), new Coordinate(4, 4) })
                });
                geometryData.setGeometry(multiLineString);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[] {
                        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
                                new Coordinate(3, 3), new Coordinate(1, 1) }),
                        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(4, 4), new Coordinate(5, 5),
                                new Coordinate(6, 6), new Coordinate(4, 4) })
                });
                geometryData.setGeometry(multiPolygon);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                LinearRing linearRing = geometryFactory.createLinearRing(new Coordinate[] { new Coordinate(1, 1),
                        new Coordinate(2, 2), new Coordinate(3, 3), new Coordinate(1, 1) });
                geometryData.setGeometry(linearRing);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                MultiPoint multiPoint = geometryFactory.createMultiPointFromCoords(
                        new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2), new Coordinate(3, 3) });
                geometryData.setGeometry(multiPoint);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                Polygon polygon = geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1),
                        new Coordinate(2, 2), new Coordinate(3, 3), new Coordinate(1, 1) });
                geometryData.setGeometry(polygon);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(new Geometry[] {
                        geometryFactory.createPoint(new Coordinate(1, 1)),
                        geometryFactory.createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
                        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
                                new Coordinate(3, 3), new Coordinate(1, 1) })
                });
                geometryData.setGeometry(geometryCollection);
                geometryDataList.add(geometryData);
            }

            long count = GeometryDataWKBMapper.insertList(geometryDataList);
            System.out.println(count);
            session.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/3cfecffae0184ee3a969caf36b0dfd8d.png)

