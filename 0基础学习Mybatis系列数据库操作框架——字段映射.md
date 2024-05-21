@[TOC](大纲)

在[《0基础学习Mybatis系列数据库操作框架——增删改操作》](https://fangliang.blog.csdn.net/article/details/137297044)的更新操作中，我们定义的SQL Mapper是
```xml
<update id="updateElems">
    update all_type set info_tint = #{info_tint}, info_sint = #{info_sint} where info_int &gt; #{info_int}
</update>
```
Mapper接口新增的方法是
```java
    long updateElems(AllType item);
```
可以看到SQL Mapper自动将info_tint映射到AllType.info_tint。这种映射成功的前提是AllType定义的字段名和表中列名一致：

```java
public class AllType {
……
    private int info_int;

    private byte info_tint;

    private short info_sint;
}
```

```bash
create table all_type(
        info_int int(10) comment 'int',
        info_tint tinyint comment 'tinyint',
        info_sint smallint comment 'smallint',
……
}
```
但是不同语言的编程规范不同，导致表中字段名和编程语言中的字段名无法一致。比如我们希望通过下面这个类来表达表中数据。
```java
package org.example.model;

public class AllTypeRename {
    public int getIntInfo() {
        return intInfo;
    }

    public void setIntInfo(int intInfo) {
        this.intInfo = intInfo;
    }

    public byte getByteInfo() {
        return byteInfo;
    }

    public void setByteInfo(byte byteInfo) {
        this.byteInfo = byteInfo;
    }

    public short getShortInfo() {
        return shortInfo;
    }

    public void setShortInfo(short shortInfo) {
        this.shortInfo = shortInfo;
    }

    private int intInfo;
    private byte byteInfo;
    private short shortInfo;
}
```
intInfo对应表中的info_int；byteInfo对应info_tint；shortInfo对应info_sint。
我们如何将这样的映射关系告诉Mybatis呢？
# AS绑定法
一种简单的办法就是在SQL中绑定。
```xml
    <select id="findRenameOne" resultType="AllTypeRename">
        select
            info_int as "intInfo",
            info_tint as "byteInfo",
            info_sint as "shortInfo"
        from all_type where info_int = #{intInfo}
    </select>
```
SQL的select部分，**as关键字前是表中列名，后面是Java类AllTypeRename的字段名**。
Mapper接口中我们新增如下代码
```java
    AllTypeRename findRenameOne(int intInfo);
```
## 测试代码
```java
    @Test
    void testRenameOne() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-rename.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession()) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            AllTypeRename a = all_type_mapper.findRenameOne(1);
            if (a != null) {
                System.out.println(a.getShortInfo());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
```
# resultMap法
AS方法虽然很直接明确，但是存在几个问题：
- 多个SQL需要配多次映射关系。
- 不能解决通配符的问题。比如Select * from table。

为了解决这类问题，可以使用本节介绍的方法。
```xml
    <resultMap id="allTypeRenameResultMap" type="AllTypeRename">
        <result property="intInfo" column="info_int"/>
        <result property="byteInfo" column="info_tint"/>
        <result property="shortInfo" column="info_sint"/>
    </resultMap>
```
resultMap的type字段是需要映射的类名AllTypeRename，这儿我们使用了简称，因为我们在Mybatis的Config中新增了别名
```xml
    <typeAliases>
        <typeAlias type="org.example.model.AllTypeRename" alias="AllTypeRename"/>
    </typeAliases>
```
result 的property是类的成员变量名；column是表中列名。
这样我们配置SQL Mapper时只要如下书写即可。
```xml
    <select id="findRenameList" resultMap="allTypeRenameResultMap">
        select * from all_type where info_int != #{intInfo}
    </select>
```
需要注意的是，此时select要使用resultMap属性，而不是之前的resultType。
Mapper接口中新增
```java
    AllTypeRename findRenameOne(int intInfo);
```
就可以访问数据库了
## 测试代码
```java
    @Test
    void testRenameList() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-rename.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession()) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            List<AllTypeRename> all = null;
            all = all_type_mapper.findRenameList(1);
            for (AllTypeRename a : Objects.requireNonNull(all)) {
                System.out.println(a.getShortInfo());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
```

代码样例见：[https://github.com/f304646673/mybatis_demo.git](https://github.com/f304646673/mybatis_demo.git)

# 参考资料
- [https://mybatis.org/mybatis-3/zh_CN/configuration.html](https://mybatis.org/mybatis-3/zh_CN/configuration.html)
- [https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html](https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html)
