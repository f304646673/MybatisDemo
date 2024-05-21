WKT全称是Well-Known Text。它是一种表达几何信息的字符串内容。比如点可以用WKT表示为POINT (3 3)；线可以用WKT表示为LINESTRING (1 1, 2 2)。
Mysql数据库可以存储一些几何类型数据，比如点、线、多边形等。这在一些基于地理信息的服务上比较有用，比如在地图上的商店地理坐标（点），或者路径规划中的行进路线（线）等。
目前我使用的Mysql是8.4.0版本，它支持如下几何类型数据结构。
|类型  |说明 |样例 |图例|
|--|--|--|--|
|点  |  | POINT (3 3) |![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/cc6acae17e10462482905dafab11fd1f.png)|
|点集合|  |MULTIPOINT ((1 1), (2 2), (3 3)) |
|  线|  |LINESTRING (1 1, 2 2) |![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/385cadf341524caf962217d56bb6692c.png)|
|  线集合|  |MULTILINESTRING ((1 1, 2 2), (3 3, 4 4)) |
| 多边形 |  |POLYGON ((1 1, 2 2, 3 3, 1 1)) |![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/fd607dfd10ff47c9b6a161435640b514.png)|
| 多边形集合 |  |MULTIPOLYGON (((1 1, 2 2, 3 3, 1 1)), ((4 4, 5 5, 6 6, 4 4))) |
| 多种几何类型集合 |  |GEOMETRYCOLLECTION (POINT (1 1), LINESTRING (1 1, 2 2), POLYGON ((1 1, 2 2, 3 3, 1 1))) |

一般我们会使用org.locationtech.jts的Geometry类来表达几何信息。
```xml
<dependency>
    <groupId>org.locationtech.jts</groupId>
    <artifactId>jts-core</artifactId>
    <version>1.19.0</version>
</dependency>
```
然后使用下面的方法构建各种结构
# 几何结构构建
## 点

```java
GeometryFactory geometryFactory = new GeometryFactory();
Geometry geometry = geometryFactory.createPoint(new Coordinate(3, 3));
```

## 点集合

```java
GeometryFactory geometryFactory = new GeometryFactory();
LineString lineString = geometryFactory
        .createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) });
```
## 线

```java
GeometryFactory geometryFactory = new GeometryFactory();
LineString lineString = geometryFactory
        .createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) });
```
## 线集合

```java
GeometryFactory geometryFactory = new GeometryFactory();
MultiLineString multiLineString = geometryFactory.createMultiLineString(new LineString[] {
        geometryFactory
                .createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
        geometryFactory
                .createLineString(new Coordinate[] { new Coordinate(3, 3), new Coordinate(4, 4) })
});
```
## 面

```java
GeometryFactory geometryFactory = new GeometryFactory();
Polygon polygon = geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1),
 					new Coordinate(2, 2), new Coordinate(3, 3), new Coordinate(1, 1) });
```
## 面集合

```java
GeometryFactory geometryFactory = new GeometryFactory();
MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[] {
        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
                new Coordinate(3, 3), new Coordinate(1, 1) }),
        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(4, 4), new Coordinate(5, 5),
                new Coordinate(6, 6), new Coordinate(4, 4) })
});
```
## 几何信息集合

```java
GeometryFactory geometryFactory = new GeometryFactory();
GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(new Geometry[] {
        geometryFactory.createPoint(new Coordinate(1, 1)),
        geometryFactory.createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
                new Coordinate(3, 3), new Coordinate(1, 1) })
});
```
下面我们需要将这些结构保存到Mysql数据库中。
**由于org.locationtech.jts.geom.Geometry和Mysql内部存储的Geometry不配，所以需要转换操作，于是就要引入typehandler。**
# TypeHandler
```java
package org.example.typehandlers;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GeometryTypeWKTHandler extends BaseTypeHandler<Geometry>  {
    private static GeometryFactory factory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        String str = serializeGeometry(parameter);
        ps.setString(i, str);;
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String str = rs.getString(columnName);
        try {
            return deserializeGeometry(str);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String str = rs.getString(columnIndex);
        try {
            return deserializeGeometry(str);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String str = cs.getString(columnIndex);
        try {
            return deserializeGeometry(str);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    private static String serializeGeometry(Geometry geometry) {
        WKTWriter writer = new WKTWriter(2);
        return writer.write(geometry);
    }

    private static Geometry deserializeGeometry(String wkt) throws ParseException {
        return new WKTReader(factory).read(wkt);
    }

}
```
WKTWriter会将org.locationtech.jts.geom.Geometry转换为String，然后交由SQL语句处理；
WKTReader会将SQL语句读取出来的String转换为org.locationtech.jts.geom.Geometry对象。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/f5312c05f1fc4bc9ab7c4367bc3ef70c.png)
# SQL操作
在上图我们看到，TypeHandler主要使用String类型作为媒介来和SQL语句联系。那么SQL语句是如何把String转成Mysql的Geometry内部结构的呢？
这就需要引入ST_GeomFromText和ST_AsText。
**ST_GeomFromText可以将WKT格式的几何信息转换为Mysql内部的Geometry结构**。比如
```sql
ST_GeomFromText('MULTIPOINT (1 1, 2 2, 3 3)')
```
**ST_AsText则可以将Mysql内部的Geometry结构转换为WKT格式的几何信息**。
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/9214c13403f84f6e967086ae404a5927.png)

在Mybatis的SQL XML中
## 写入操作
对org.locationtech.jts.geom.Geometry结构（即geometry字段）使用org.example.typehandlers.GeometryTypeWKTHandler处理成WTK（字符串）几何信息格式后，用ST_GeomFromText转换成Mysql内部的Geometry结构，然后存储。
```xml
    <insert id="insertOne" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
    </insert>
```
这儿需要注意的是ST_GeomFromText不是万能的。比如针对“几何信息集合”（GeometryCollection）则需要使用ST_GeomCollFromText来转换

```xml
    <insert id="insertGeometryCollection" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomCollFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
    </insert>
```
## 读取操作
由于需要对geometry字段特殊处理，所以不能使用Select * From geometry_data，而需要把每个参数都写好。
ST_AsText会将Mysql的内部的Geometry结构转换成WKT格式（字符串）的几何信息，然后交由org.example.typehandlers.GeometryTypeWKTHandler转换成org.locationtech.jts.geom.Geometry结构。
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
## 完整XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- AllTypeMapper-1.xml -->
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="org.example.mapper.GeometryDataWKTMapper">
    <resultMap id="GeometryDataResultMap" type="org.example.model.GeometryData">
        <result property="id" column="id"/>
        <result property="geometry" column="geometry" typeHandler="org.example.typehandlers.GeometryTypeWKTHandler" jdbcType="BLOB"/>
        <result property="updateTime" column="update_time"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <select id="findAll" resultMap="GeometryDataResultMap">
        select id, ST_AsText(geometry) AS geometry, update_time, create_time from geometry_data
    </select>

    <select id="find" resultMap="GeometryDataResultMap">
        select id, ST_AsText(geometry) AS geometry, update_time, create_time from geometry_data where id = #{id}
    </select>

    <insert id="insertOne" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
    </insert>

    <insert id="insertList" parameterType="list">
        insert into geometry_data(id, geometry, update_time, create_time) values
        <foreach item="item" collection="list" separator=",">
            (#{item.id}, ST_GeomFromText(#{item.geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
        </foreach>
    </insert>

    <update id="updateOne" parameterType="org.example.model.GeometryData">
        update geometry_data set geometry = ST_GeomFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), update_time = now() where id = #{id} 
    </update>

    <insert id="insertGeometryCollection" parameterType="org.example.model.GeometryData"  useGeneratedKeys="true" keyProperty="id">
        insert into geometry_data(id, geometry, update_time, create_time) values
        (#{id}, ST_GeomCollFromText(#{geometry, jdbcType=BLOB, typeHandler=org.example.typehandlers.GeometryTypeWKTHandler}), now(), now())
    </insert>

</mapper>
```
# Mapper
```java
package org.example.mapper;

import java.util.List;

import org.example.model.GeometryData;

public interface GeometryDataWKTMapper {
    public List<GeometryData> findAll();
    public GeometryData find(Long id);
    public Long insertOne(GeometryData geometryData);
    public Long insertList(List<GeometryData> geometryDataList);
    public Long updateOne(GeometryData geometryData);
    public Long insertGeometryCollection(GeometryData geometryData);
} 
```
# 测试代码

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
import org.example.mapper.GeometryDataWKTMapper;
import org.example.model.GeometryData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class GeometryDataWKTTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-geometry-wkt.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    public void testFindAll() {
        List<GeometryData> all = null;
        try (SqlSession session = sqlSF.openSession()) {
            all = session.getMapper(GeometryDataWKTMapper.class).findAll();
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
            GeometryDataWKTMapper GeometryDataWKTMapper = session.getMapper(GeometryDataWKTMapper.class);
            GeometryData one = GeometryDataWKTMapper.find(1L);
            System.out.println(one.getGeometry());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testInsert() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKTMapper GeometryDataWKTMapper = session.getMapper(GeometryDataWKTMapper.class);
            GeometryData geometryData = new GeometryData();
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate coordinate = new Coordinate(1, 1);
            Geometry geometry = geometryFactory.createPoint(coordinate);
            geometryData.setGeometry(geometry);
            long count = GeometryDataWKTMapper.insertOne(geometryData);
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
            GeometryDataWKTMapper GeometryDataWKTMapper = session.getMapper(GeometryDataWKTMapper.class);
            GeometryData geometryData = new GeometryData();
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate coordinate = new Coordinate(2, 2);
            Geometry geometry = geometryFactory.createPoint(coordinate);
            geometryData.setId(1L);
            geometryData.setGeometry(geometry);
            long count = GeometryDataWKTMapper.updateOne(geometryData);
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
            GeometryDataWKTMapper GeometryDataWKTMapper = session.getMapper(GeometryDataWKTMapper.class);

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

            // {
            //     GeometryData geometryData = new GeometryData();
            //     GeometryFactory geometryFactory = new GeometryFactory();
            //     LinearRing linearRing = geometryFactory.createLinearRing(new Coordinate[] { new Coordinate(1, 1),
            //             new Coordinate(2, 2), new Coordinate(3, 3), new Coordinate(1, 1) });
            //     geometryData.setGeometry(linearRing);
            //     geometryDataList.add(geometryData);
            // }

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

            // {
            //     GeometryData geometryData = new GeometryData();
            //     GeometryFactory geometryFactory = new GeometryFactory();
            //     GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(new Geometry[] {
            //             geometryFactory.createPoint(new Coordinate(1, 1)),
            //             geometryFactory.createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
            //             geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
            //                     new Coordinate(3, 3), new Coordinate(1, 1) })
            //     });
            //     geometryData.setGeometry(geometryCollection);
            //     geometryDataList.add(geometryData);
            // }

            long count = GeometryDataWKTMapper.insertList(geometryDataList);
            System.out.println(count);
            session.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testInsertGeometryCollection() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKTMapper GeometryDataWKTMapper = session.getMapper(GeometryDataWKTMapper.class);
            GeometryData geometryData = new GeometryData();
            GeometryFactory geometryFactory = new GeometryFactory();
            GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(new Geometry[] {
                    geometryFactory.createPoint(new Coordinate(1, 1)),
                    geometryFactory.createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
                    geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
                            new Coordinate(3, 3), new Coordinate(1, 1) })
            });
            geometryData.setGeometry(geometryCollection);
            Long index = GeometryDataWKTMapper.insertGeometryCollection(geometryData);
            System.out.println(index);
            session.commit();
        } catch(Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/ce417e9ff0d14a69a4b4a8b157ed9756.png)

# 建表SQL

```sql
CREATE TABLE `geometry_data` (
    `id` BIGINT(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `geometry` GEOMETRY NOT NULL COMMENT '几何信息',
    `create_time` timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` boolean DEFAULT false COMMENT '是否已被删除',
    PRIMARY KEY (`id`),
    SPATIAL INDEX `spatial_geometry` (`geometry`)
) COMMENT='几何数据表';
```
# 总结
很多数据库为了兼容Mysql，针对Geometry类型，在WKT模式下是兼容的。因为如何将WKT转换成自己数据库内部的结构，即对ST_GeomFromText等方法的实现是可以自己内部处理，让用户不会感知。这让WKT方案在跨数据库时有比较好的兼容性。
但是如果只是针对Mysql数据库，或者像OceanBase这类对Mysql底层也兼容很好的数据库，则可以考虑WKB方案。这块的内容我们会在[《0基础学习Mybatis系列数据库操作框架——Mysql的Geometry数据处理之WKB方案》](https://fangliang.blog.csdn.net/article/details/139097706)中介绍。
# 代码
[https://github.com/f304646673/mybatis_demo](https://github.com/f304646673/mybatis_demo)

# 参考资料
- [https://dev.mysql.com/doc/refman/8.0/en/gis-data-formats.html#gis-wkb-format](https://dev.mysql.com/doc/refman/8.0/en/gis-data-formats.html#gis-wkb-format)
- [https://dev.mysql.com/doc/refman/8.0/en/gis-wkt-functions.html#function_st-geomfromtext](https://dev.mysql.com/doc/refman/8.0/en/gis-wkt-functions.html#function_st-geomfromtext)
- [https://www.keene.edu/campus/maps/tool/](https://www.keene.edu/campus/maps/tool/)
