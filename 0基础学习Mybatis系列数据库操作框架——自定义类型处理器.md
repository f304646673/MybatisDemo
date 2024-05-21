@[TOC](大纲)

我们有时候会在数据库中放入一个扩展字段，用于保存在表设计时尚未考虑到的、未来会加入的一些信息。这个字段我们一般使用字符串存储，格式是个Json。这样后续就可以很方便进行序列化和反序列化。
本文主要讲解如何自定义类型处理器，让Mybatis自动帮我们做序列化和反序列化。Json序列化工具我们采用fastjson库。
为了使用这个库，我们在Maven的pom.xml中加入如下片段
```xml
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>2.0.48</version>
        </dependency>
```
然后让Maven把依赖包下载到相应为止。
# Java模型类
我们将Json字符串存储在[《0基础学习Mybatis系列数据库操作框架——最小Demo》](https://blog.csdn.net/breaksoftware/article/details/137209341)中创建的表的info_ltext字段。

> info_ltext longtext comment 'longtext',

我们设计一个Java对象映射表结构，其中jsonElemList字段对应于表中info_ltext列，只是它不是String类型，而是我们自定义的JsonList 类型。
```java
public class JsonType {
……
    private int intInfo;
    private JsonList jsonElemList;
}
```
我们将JsonList定义在JsonType内部，因为它只有在JsonType内中才有意义。JsonList中有一个成员变量jsonElemList 用于保存JsonElem数组。
```java
    public static class JsonList {
    ……
        @JSONField()
        private List<JsonElem> jsonElemList = new ArrayList<>();
   }
```
JsonElem也定义在JsonType内部。它有两个成员变量，用于丰富Json结构。它实现了Cloneable接口，以方便后续对这个结构的深拷贝。
```java
    public static class JsonElem implements Cloneable {
        @JSONField(name = "First")
        private int first;
        @JSONField(name = "Second")
        private String second;

        @Override
        public JsonElem clone() throws CloneNotSupportedException {
            JsonElem clonedJsonElem = null;
            try {
                clonedJsonElem = (JsonElem) super.clone();
                clonedJsonElem.setFirst(this.getFirst());
                clonedJsonElem.setSecond(this.getSecond());
            } finally {
            }

            return clonedJsonElem;
        }
    }
```
完整代码如下

```java
package org.example.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class JsonType {

    public static class JsonList {
        public List<JsonElem> getJsonElemList() {
            return jsonElemList;
        }

        public void setJsonElemList(List<JsonElem> jsonElemList) {
            this.jsonElemList = jsonElemList;
        }
        @JSONField()
        private List<JsonElem> jsonElemList = new ArrayList<>();

        public JsonList(List<JsonElem> jsonElemList) {
            this.jsonElemList.addAll(jsonElemList);
        }
    }

    public static class JsonElem implements Cloneable {
        public int getFirst() {
            return first;
        }

        public void setFirst(int first) {
            this.first = first;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second;
        }

        @JSONField(name = "First")
        private int first;
        @JSONField(name = "Second")
        private String second;

        public JsonElem(int first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public JsonElem clone() throws CloneNotSupportedException {
            JsonElem clonedJsonElem = null;
            try {
                clonedJsonElem = (JsonElem) super.clone();
                clonedJsonElem.setFirst(this.getFirst());
                clonedJsonElem.setSecond(this.getSecond());
            } finally {
            }

            return clonedJsonElem;
        }
    }

    public int getIntInfo() {
        return intInfo;
    }

    public void setIntInfo(int intInfo) {
        this.intInfo = intInfo;
    }

    public JsonList getJsonElemList() {
        return jsonElemList;
    }

    public void setJsonElemList(JsonList jsonElemList) {
        this.jsonElemList = jsonElemList;
    }

    private int intInfo;

    private JsonList jsonElemList;
}
```
# 定义类型处理器
我们的类型处理器继承于org.apache.ibatis.type.BaseTypeHandler，只要覆盖其几个NonNull方法即可
```java
package org.example.typehandlers;

import com.alibaba.fastjson.JSON;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.example.model.JsonType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.LONGVARCHAR)
public class JsonListHandler extends BaseTypeHandler<JsonType.JsonList> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JsonType.JsonList parameter, JdbcType jdbcType) throws SQLException {
        String jsonStr = JSON.toJSONString(parameter);
        ps.setString(i, jsonStr);
    }

    @Override
    public JsonType.JsonList getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonStr = rs.getString(columnName);
        return JSON.parseObject(jsonStr, JsonType.JsonList.class);
    }


    @Override
    public JsonType.JsonList getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonStr = rs.getString(columnIndex);
        return JSON.parseObject(jsonStr, JsonType.JsonList.class);
    }

    @Override
    public JsonType.JsonList getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonStr = cs.getString(columnIndex);
        return JSON.parseObject(jsonStr, JsonType.JsonList.class);
    }
}
```
这儿有个概念，就是Java类型、JDBC类型和数据库字段类型的对应关系。我们选用的longtext是Mysql数据库字段类型，其对应的Java类型是java.lang.string，对应的jdbc类型是LONGVARCHAR。所以上述代码我们加了@MappedJdbcTypes(JdbcType.LONGVARCHAR)注解。
关于这些对应关系，可以见文后内容。
setNonNullParameter方法是将JsonType.JsonList对象序列化成String，这样就和数据库中longtext类型对应上了。
> info_ltext longtext comment 'longtext',

getNullableResult方法则是将String内容反序列化成为JsonType.JsonList对象，这样就和Java模型类对应上了。

```java
public class JsonType {
……
    private int intInfo;
    private JsonList jsonElemList;
}
```
# 配置文件
配置文件主要完成自定义类型处理器JsonListHandler绑定的问题。主要有两种方法
## 和类型绑定
和类型绑定，即让自定义类型处理器和JDBC或者Java类型绑定。
本例我们采用和JDBC类型绑定。下面代码放置于mybatis-config-json.xml中。
```xml
    <typeHandlers>
        <typeHandler handler="org.example.typehandlers.JsonListHandler" jdbcType="LONGVARCHAR"/>
    </typeHandlers>
```
然后在SQL Mapper XML中，指定jsonElemList字段类型是上述绑定的类型。这样Mybatis在处理info_ltext字段时，就会使用自定义类型处理器JsonListHandler来处理。
```xml
    <update id="updateJsonTypeElems">
        update all_type set info_ltext  = #{jsonElemList, jdbcType=LONGVARCHAR} where info_int = #{intInfo}
    </update>
```
## 和字段绑定
和字段绑定就不用在mybatis-config-json.xml中做任何配置，只要在SQL Mapper XML中配置即可。
比如下例，定义item.jsonElemList类型处理器即可。
```xml
    <insert id="insertJsonTypeElems">
        insert into all_type(info_int, info_ltext) values
        <foreach item="item" collection="list" separator=",">
            (#{item.intInfo}, #{item.jsonElemList, typeHandler=org.example.typehandlers.JsonListHandler})
        </foreach>
    </insert>
```

## resultMap中绑定
对于Select语句，我们可以使用[《0基础学习Mybatis系列数据库操作框架——字段映射》](https://blog.csdn.net/breaksoftware/article/details/137347612)中的方法，在映射表中定义。也不用在mybatis-config-json.xml中做任何配置。
```xml
    <resultMap id="jsonTypeResultMap" type="JsonType">
        <result property="intInfo" column="info_int"/>
        <result property="jsonElemList" column="info_ltext" jdbcType="LONGVARCHAR" typeHandler="org.example.typehandlers.JsonListHandler"/>
    </resultMap>

    <select id="selectJsonTypeElems" resultMap="jsonTypeResultMap">
        select * from all_type
    </select>
```
完整配置代码如下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- AllTypeMapper-1.xml -->
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

    <insert id="insertJsonTypeElems">
        insert into all_type(info_int, info_ltext) values
        <foreach item="item" collection="list" separator=",">
            (#{item.intInfo}, #{item.jsonElemList, typeHandler=org.example.typehandlers.JsonListHandler})
        </foreach>
    </insert>

    <update id="updateJsonTypeElems">
        update all_type set info_ltext  = #{jsonElemList, jdbcType=LONGVARCHAR} where info_int = #{intInfo}
    </update>

    <select id="selectJsonTypeElems" resultMap="jsonTypeResultMap">
        select * from all_type
    </select>
</mapper>
```
# Mapper代码
在[《0基础学习Mybatis系列数据库操作框架——增删改操作》](https://blog.csdn.net/breaksoftware/article/details/137297044)引入的AllTypeMapper中新增如下三行方法以对应SQL Mapper XML中的三个方法id。

```java
    long insertJsonTypeElems(List<JsonType> jsonTypeList);
    long updateJsonTypeElems(JsonType jsonType);
    List<JsonType> selectJsonTypeElems(int intInfo);
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

public class JsonTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-json.xml");
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
            List<JsonType> all = all_type_mapper.selectJsonTypeElems(1);
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
}
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8fabba45ecea4ffdb3c0700f61081146.png)

# 类型对应关系表
|	类型处理器	|	Java 类型	|	JDBC 类型	|
|	-	|	-	|	-	|
|	BooleanTypeHandler	|	java.lang.Boolean, boolean	|	数据库兼容的 BOOLEAN	|
|	ByteTypeHandler	|	java.lang.Byte, byte	|	数据库兼容的 NUMERIC 或 BYTE	|
|	ShortTypeHandler	|	java.lang.Short, short	|	数据库兼容的 NUMERIC 或 SMALLINT	|
|	IntegerTypeHandler	|	java.lang.Integer, int	|	数据库兼容的 NUMERIC 或 INTEGER	|
|	LongTypeHandler	|	java.lang.Long, long	|	数据库兼容的 NUMERIC 或 BIGINT	|
|	FloatTypeHandler	|	java.lang.Float, float	|	数据库兼容的 NUMERIC 或 FLOAT	|
|	DoubleTypeHandler	|	java.lang.Double, double	|	数据库兼容的 NUMERIC 或 DOUBLE	|
|	BigDecimalTypeHandler	|	java.math.BigDecimal	|	数据库兼容的 NUMERIC 或 DECIMAL	|
|	StringTypeHandler	|	java.lang.String	|	CHAR, VARCHAR	|
|	ClobReaderTypeHandler	|	java.io.Reader	|	-	|
|	ClobTypeHandler	|	java.lang.String	|	CLOB, LONGVARCHAR	|
|	NStringTypeHandler	|	java.lang.String	|	NVARCHAR, NCHAR	|
|	NClobTypeHandler	|	java.lang.String	|	NCLOB	|
|	BlobInputStreamTypeHandler	|	java.io.InputStream	|	-	|
|	ByteArrayTypeHandler	|	byte[]	|	数据库兼容的字节流类型	|
|	BlobTypeHandler	|	byte[]	|	BLOB, LONGVARBINARY	|
|	DateTypeHandler	|	java.util.Date	|	TIMESTAMP	|
|	DateOnlyTypeHandler	|	java.util.Date	|	DATE	|
|	TimeOnlyTypeHandler	|	java.util.Date	|	TIME	|
|	SqlTimestampTypeHandler	|	java.sql.Timestamp	|	TIMESTAMP	|
|	SqlDateTypeHandler	|	java.sql.Date	|	DATE	|
|	SqlTimeTypeHandler	|	java.sql.Time	|	TIME	|
|	ObjectTypeHandler	|	Any	|	OTHER 或未指定类型	|
|	EnumTypeHandler	|	Enumeration Type	|	VARCHAR 或任何兼容的字符串类型，用来存储枚举的名称（而不是索引序数值）	|
|	EnumOrdinalTypeHandler	|	Enumeration Type	|	任何兼容的 NUMERIC 或 DOUBLE 类型，用来存储枚举的序数值（而不是名称）。	|
|	SqlxmlTypeHandler	|	java.lang.String	|	SQLXML	|
|	InstantTypeHandler	|	java.time.Instant	|	TIMESTAMP	|
|	LocalDateTimeTypeHandler	|	java.time.LocalDateTime	|	TIMESTAMP	|
|	LocalDateTypeHandler	|	java.time.LocalDate	|	DATE	|
|	LocalTimeTypeHandler	|	java.time.LocalTime	|	TIME	|
|	OffsetDateTimeTypeHandler	|	java.time.OffsetDateTime	|	TIMESTAMP	|
|	OffsetTimeTypeHandler	|	java.time.OffsetTime	|	TIME	|
|	ZonedDateTimeTypeHandler	|	java.time.ZonedDateTime	|	TIMESTAMP	|
|	YearTypeHandler	|	java.time.Year	|	INTEGER	|
|	MonthTypeHandler	|	java.time.Month	|	INTEGER	|
|	YearMonthTypeHandler	|	java.time.YearMonth	|	VARCHAR 或 LONGVARCHAR	|
|	JapaneseDateTypeHandler	|	java.time.chrono.JapaneseDate	|	DATE	|

# 总结

 - 自定义类型处理器类比较好写。只要定义好序列化和反序列化即可。
 - 主要容易混乱的点是在配置文件。
 	- **JDBC、Jave类型和自定义处理器绑定，需要在mybatis-config.xml中定义它们绑定关系；同时需要在SQL Mapper XML中的需要处理的字段上，用jdbcType或者javaType强调类型，才能让自定义类型处理器在这个字段上生效。**
 	- **直接在SQL Mapper XML中的需要处理的字段上指定类型处理器。这样就不用在mybatis-config.xml中做任何配置。**
 	- Select类型的SQL，除了直接在字段上标明类型处理器，还可以在resultMap上指定。

代码样例见：[https://github.com/f304646673/mybatis_demo.git](https://github.com/f304646673/mybatis_demo.git)
# 参考资料
-  [https://mybatis.org/mybatis-3/zh_CN/configuration.html#typeHandlers](https://mybatis.org/mybatis-3/zh_CN/configuration.html#typeHandlers)
