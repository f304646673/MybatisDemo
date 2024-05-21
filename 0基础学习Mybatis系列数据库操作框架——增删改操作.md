@[TOC](大纲)

在[《0基础学习Mybatis系列数据库操作框架——目录结构》](https://blog.csdn.net/breaksoftware/article/details/137242179)一文中，我们已经搭建了查询操作的框架。在这个基础上，我们将通过本文的学习掌握增、删、改的操作。
为了让数据维度更加丰富，我们给数据库单行数据映射的对象类增加两个变量——info_tint和info_sint，它们分别对应数据库表中对应的项。

```java
package org.example.model;

public class AllType {

    public int getInfo_int() {
        return info_int;
    }

    public void setInfo_int(int info_int) {
        this.info_int = info_int;
    }

    public byte getInfo_tint() {
        return info_tint;
    }

    public void setInfo_tint(byte info_tint) {
        this.info_tint = info_tint;
    }

    public short getInfo_sint() {
        return info_sint;
    }

    public void setInfo_sint(short info_sint) {
        this.info_sint = info_sint;
    }

    private int info_int;

    private byte info_tint;

    private short info_sint;
}

```
# 新增
```bash
INSERT INTO tableName(colomnAName, colomnBName……) VALUES(colomnAValue1, colomnBValue1),(colomnAValue2,colomnBValue2),(colomnAValue3,colomnBValue3)
```
## Mapper配置
新增类型的SQL比较特别，它需要依赖于批量的数据。这样SQL Mapper的XML文件中就无法写死SQL，因为具体有多少VALUES则依赖于运行时的数据，而无法在编写代码时确认。
因为这样的特性，就需要SQL Mapper的XML文件中可以一定一种具有“循环生成”语义的部分。

```xml
    <insert id="insertElems">
        insert into all_type(info_int, info_tint, info_sint) values
        <foreach item="item" collection="list" separator=",">
            (#{item.info_int}, #{item.info_tint}, #{item.info_sint})
        </foreach>
    </insert>
```
上例中foreach就会循环一个list，然后生成用","分割的一批VALUE值。
## 代码
### Mapper接口文件
在Mapper接口中，我们新增以下方法声明即可。

```java
long insertElems(List<AllType> AllTypeList);
```
**返回值表示成功新增的数据量。**
### 应用

```java
@Test
    void testBatchInsert() {
        List<AllType> allTypelist= new LinkedList<>();
        for (byte i = 100; i < 110; i++) {
            AllType allType = new AllType();
            allType.setInfo_int(i);
            allType.setInfo_sint(i);
            allType.setInfo_tint(i);
            allTypelist.add(allType);
        }

        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-1.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.insertElems(allTypelist);
            System.out.println(count);
        }
    }
```
需要注意的是要给openSession方法传递true，这样autoCommit会被设置，进而代码执行后，数据会真正添加到表中。如果测试时发现代码表达了插入成功（返回了成功条数），但是在表中没有执行的效果（无新增数据），往往就是这个参数没有被设置。后续的删、改操作都要这么设置。
# 删除
```bash
DELETE FROM tableName WHERE condition;
```
## 简单方案
### Mapper配置
删除的SQL定制性比较强的是condition部分。
一种简单的办法就是针对某种特点的SQL写一条独立的项，比如
```xml
    <delete id="deleteElemWhereInfoIntLessThen">
        delete from all_type where info_int &lt; #{value}
    </delete>
```
这样Mapper接口代码中只要新增deleteElemWhereInfoIntLessThen方法，并只传递一个参数——info_int即可。
这儿需要注意的是，&lt;是表达小于号（<）。由于XML中<等字符用于构建结构，我们不能直接使用这些字符，否则机会导致XML解析出错。

> \### Cause: org.apache.ibatis.builder.BuilderException: Error parsing SQL Mapper Configuration. Cause: org.apache.ibatis.builder.BuilderException: Error creating document instance.  Cause: org.xml.sax.SAXParseException; lineNumber:

需要替换的字符参考如下：
```xml
原符号       <        <=      >       >=       &        '        "
替换符号    &lt;    &lt;=   &gt;    &gt;=   &amp;   &apos;  &quot;
```
### 代码
我们只需要在mapper接口中新增如下方法即可。
```java
    long deleteElemWhereInfoIntLessThen(int info_int);
```
使用方法如下

```java
  AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
  long count = all_type_mapper.deleteElemWhereInfoIntLessThen(103);
  System.out.println(count);
```
## 高级方案
对于value值类型相同的、且condition结构相同的SQL，我们还可以采用如下的方案


### Mapper配置
我们让对比的列名、对比符号和值都通过代码来设置。
```xml
<delete id="deleteElem">
    delete from all_type where ${column_name} ${comparison_operator} #{value}
</delete>
```
### 代码
#### Mapper接口文件
我们只需要在mapper接口中新增如下方法即可。
```java
long deleteElem(@Param("column_name") String column_name, @Param("comparison_operator") String comparison_operator, @Param("value") int value);
```
#### 应用

```java
count = all_type_mapper.deleteElem("info_int", "<",105);
count = all_type_mapper.deleteElem("info_int", ">",106);
```

## 完整代码

```java
    @Test
    void testDelete() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-1.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.deleteElemWhereInfoIntLessThen(103);
            System.out.println(count);
            count = all_type_mapper.deleteElem("info_int", "<",105);
            System.out.println(count);
            count = all_type_mapper.deleteElem("info_int", ">",106);
            System.out.println(count);
        }
    }
```

# 修改
```bash
UPDATE tableName SET colomnAName = valueA, colomnBName = valueB where condition
```
比较复杂的condition我们已经在“删除”环节见过。当前我们将重心放在SET部分。
如果不存在数据库内部计算的场景，比如SET some=some+1，则需要更新的数据都来源于代码。我们只要给这条语句传递一个Java数据对象即可。
## Mapper配置

```xml
<update id="updateElems">
    update all_type set info_tint = #{info_tint}, info_sint = #{info_sint} where info_int &gt; #{info_int}
</update>
```
注意这儿的占位符都是Java对象类AllType中字段名。
## 代码
### Mapper接口文件

```java
    long updateElems(AllType item);
```
### 应用

```java
    @Test
    void testUpdate() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-1.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            AllType a = new AllType();
            a.setInfo_int(105);
            a.setInfo_tint((byte) 20);
            a.setInfo_sint((short) 10);
            long count = all_type_mapper.updateElems(a);
            System.out.println(count);
        }
    }
```
代码样例见：[https://github.com/f304646673/mybatis_demo.git](https://github.com/f304646673/mybatis_demo.git)

# 参考资料
- [https://mybatis.org/mybatis-3/zh_CN/configuration.html](https://mybatis.org/mybatis-3/zh_CN/configuration.html)
- [https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html](https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html)
