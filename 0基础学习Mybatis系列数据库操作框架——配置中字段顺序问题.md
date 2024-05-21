@[TOC](大纲)

我们在[《0基础学习Mybatis系列数据库操作框架——多环境配置》](https://blog.csdn.net/breaksoftware/article/details/137290295)中，给配置文件新增了properties字段，让这些属性值可以被同文件中其他地方引用，简化了文件。
# typeAliases
我们还可以使用typeAliases定义一些值，让SQL Mapper XML中引用。
比如我们所有的查找操作，返回的都是"org.example.model.AllType"。在SQL Mapper XML（AllTypeMapper.xml）中如下使用。
```xml
<select id="findAll" resultType="org.example.model.AllType">
      select * from all_type
</select>
<select id="find" resultType="org.example.model.AllType">
    select * from all_type where info_int = #{info_int}
</select>
```
如果我们觉得这个值太长，可以在Mybatis配置文件（mybatis-config.xml）中新增如下字段
```xml
    <typeAliases>
        <typeAlias type="org.example.model.AllType" alias="AllType"/>
    </typeAliases>
```
然后将在SQL Mapper XML中org.example.model.AllType替换成AllType即可。
```xml
    <select id="findAll" resultType="AllType">
        select * from all_type
    </select>
    <select id="find" resultType="AllType">
        select * from all_type where info_int = #{info_int}
    </select>
```
# settings
除了这些简化配置的功能，Mybatis配置文件还可以给框架设置一些属性。比如我们希望执行过程可以输出到终端，则需要加入如下配置即可

```xml
    <settings>
        <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>
```
这样我们执行代码时就可以看大SQL语句模板

> Logging initialized using 'class org.apache.ibatis.logging.stdout.StdOutImpl' adapter.
Opening JDBC Connection
\=\=>  Preparing: insert into all_type(info_int, info_tint, info_sint) values (?, ?, ?) , (?, ?, ?) , (?, ?, ?) , (?, ?, ?) , (?, ?, ?) , (?, ?, ?) , (?, ?, ?) , (?, ?, ?) , (?, ?, ?) , (?, ?, ?)
\=\=> Parameters: 100(Integer), 100(Byte), 100(Short), 101(Integer), 101(Byte), 101(Short), 102(Integer), 102(Byte), 102(Short), 103(Integer), 103(Byte), 103(Short), 104(Integer), 104(Byte), 104(Short), 105(Integer), 105(Byte), 105(Short), 106(Integer), 106(Byte), 106(Short), 107(Integer), 107(Byte), 107(Short), 108(Integer), 108(Byte), 108(Short), 109(Integer), 109(Byte), 109(Short)
<==    Updates: 10
Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@78a287ed]
# 字段顺序
经过这番修改，我们的配置文件最后如下

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!-- mybatis-config-2.xml -->
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <properties>
        <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
        <property name="username" value="root"/>
        <property name="password" value="fangliang"/>
    </properties>
    <settings>
        <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>
    <typeAliases>
        <typeAlias type="org.example.model.AllType" alias="AllType"/>
    </typeAliases>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${driver}"/>
                <property name="url" value="jdbc:mysql://localhost:3306/testdb?useSSL=true&amp;useUnicode=true&amp;characterEncoding=utf8"/>
                <property name="username" value="${username}"/>
                <property name="password" value="${password}"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="mybatis/mapper/AllTypeMapper-2.xml"/>
    </mappers>

</configuration>
```
这儿特别需要注意的是：**configuration下字段是有顺序的**。
假如我们将settings放在properties前，如下<font color=Red>（错误的）</font>
```xml
<configuration>
    <settings>
        <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>
    <properties>
        <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
        <property name="username" value="root"/>
        <property name="password" value="fangliang"/>
    </properties>
    ……
<configuration>
```
就会报错：

> The content of element type "configuration" must match "(properties?,settings?,typeAliases?,typeHandlers?,objectFactory?,objectWrapperFactory?,reflectorFactory?,plugins?,environments?,databaseIdProvider?,mappers?)".

这句话的意思是：configuration下字段需要按如下顺序排列。

 1. properties
 2. settings
 3. typeAliases
 4. typeHandlers
 5. objectFactory
 6. objectWrapperFactory
 7. reflectorFactory
 8. plugins
 9. environments
 10. databaseIdProvider
 11. mappers

# 参考资料
- [https://mybatis.org/mybatis-3/zh_CN/configuration.html](https://mybatis.org/mybatis-3/zh_CN/configuration.html)
- [https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html](https://mybatis.org/mybatis-3/zh_CN/sqlmap-xml.html)
