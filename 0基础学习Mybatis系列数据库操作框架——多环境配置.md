@[TOC](大纲)

在实际开发中，我们往往会将开发环境分成：开发、测试、线上等环境。这些环境的数据源不一样，比如开发环境就不能访问线上环境，否则极容易出现线上数据污染等问题。Mybatis通过多环境配置分开定义来解决这个问题，即我们可以在Mybatis的配置文件中定义多个环境的信息。
# 配置
下面的配置在environments项下分出了两个environment：

- development。用于开发环境。开发环境连接的数据库是testdb，这在url中体现出来。
- production。用于生产环境。生产环境连接的数据库是db。

environments的default属性定义了默认选择哪个environment。这样如果我们代码没有指定环境名，则会使用这个默认的environment配置来配置环境。
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!-- mybatis-config-multi-env.xml -->
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/testdb?useSSL=true&amp;useUnicode=true&amp;characterEncoding=utf8"/>
                <property name="username" value="root"/>
                <property name="password" value="fangliang"/>
            </dataSource>
        </environment>
        <environment id="production">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/db?useSSL=true&amp;useUnicode=true&amp;characterEncoding=utf8"/>
                <property name="username" value="root"/>
                <property name="password" value="fangliang"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="mybatis/mapper/AllTypeMapper.xml"/>
    </mappers>
</configuration>
```
在上例中，dataSource下除了url不同外（因为要连接不同数据库），数据库其他信息（比如用户名和密码等）都是一样的。这样相同数据写多次，很容易在后续维护中出现问题，比如不小心的修改导致数据不一致的问题。
为了避免这类问题，我们可以将相同的字段放到properties字段下，然后在使用的地方使用${*PropertyName*}的形式引用。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!-- mybatis-config-multi-env-1.xml -->
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <properties>
        <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
        <property name="username" value="root"/>
        <property name="password" value="fangliang"/>
    </properties>
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
        <environment id="production">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${driver}"/>
                <property name="url" value="jdbc:mysql://localhost:3306/db?useSSL=true&amp;useUnicode=true&amp;characterEncoding=utf8"/>
                <property name="username" value="${username}"/>
                <property name="password" value="${password}"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="mybatis/mapper/AllTypeMapper.xml"/>
    </mappers>
</configuration>
```
# 代码
Mybatis的代码核心流程是：

 1. 构建SqlSessionFactoryBuilder
 2. 使用1返回的对象构建SqlSessionFactory
 3. 使用2返回的对象构建SqlSession

![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/90d824881e114fe78b904c3c686e4af2.png)
SqlSessionFactoryBuilder是不区分生产环境或者开发环境的，而SqlSessionFactory是区分的，即给build传递环境变量名。下面代码传递的是production，这样后续创建的session就是连着生产环境。

```java
  InputStream in = null;
  try {
      in = Resources.getResourceAsStream("mybatis/config/mybatis-config-multi-env.xml");
  } catch (IOException e) {
      throw new RuntimeException(e);
  }
  SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in, "production");
```
完整代码如下

```java
    @Test
    void testMultiEnv() {
        ArrayList<String> configs = new ArrayList<>(
            Arrays.asList("mybatis/config/mybatis-config-multi-env.xml",
                    "mybatis/config/mybatis-config-multi-env-1.xml")
        );
        for (String config : configs) {
            InputStream in = null;
            try {
                in = Resources.getResourceAsStream(config);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in, "production");

            List<AllType> all = null;
            try (SqlSession s = sqlSFLocal.openSession()) {
                all = s.selectList("org.example.mapper.AllTypeMapper.findAll");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            for (AllType a : Objects.requireNonNull(all)) {
                System.out.println(a.getInfo_int());
            }
        }
    }
```
代码样例见：[https://github.com/f304646673/mybatis_demo.git](https://github.com/f304646673/mybatis_demo.git)

# 参考资料
- [https://mybatis.org/mybatis-3/zh_CN/configuration.html](https://mybatis.org/mybatis-3/zh_CN/configuration.html)
