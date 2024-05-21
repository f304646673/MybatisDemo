在[《0基础学习Mybatis系列数据库操作框架——自定义拦截器》](https://blog.csdn.net/breaksoftware/article/details/137437192)中，我们在Mybatis向数据库发起请求前，拦截了Delete操作。而如果有些数据不希望业务代码查询到，则可以使用本文介绍的“查询结果过滤器”。
Mybatis并没有设计这样的组件，但是我们可以通过自定义对象工厂来解决这个问题。
我们将基于[《0基础学习Mybatis系列数据库操作框架——最小Demo》](https://fangliang.blog.csdn.net/article/details/13720934)来设计本案例。
# 代码
实现自定义工厂只需要继承org.apache.ibatis.reflection.factory.DefaultObjectFactory，并覆盖下面两个方法
```java
public <T> T create(Class<T> type);
public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);
```
我们着重关注第二个方法，因为它在constructorArgs中传递了构造参数的值。
但是要使得第二个方法生效，即要让constructorArgs有值，我们需要对Java模型类做个修改。
## Java模型类
我们删除了默认构造函数，显式声明了带参数的构造函数。这样第二个方法被调用时，Mybatis会用数据库返回的数据填充constructorArgs，进而让我们有拦截返回结果的可能。
```java
public class AllType {
//    public  AllType() {
//    }

    public AllType(int info_int, byte info_tint, short info_sint) {
        this.info_int = info_int;
        this.info_tint = info_tint;
        this.info_sint = info_sint;
    }    
……
	private int info_int;

    private byte info_tint;

    private short info_sint;
}
```
这样的修改也会带来一个副作用：如果数据库中info_int、info_tint、info_sint中任意一个字段为null，将会导致Mybatis反射出现错误。出现诸如下面的错误：
> org.apache.ibatis.reflection.ReflectionException: Error instantiating class org.example.model.AllType with invalid types (int,byte,short) or values (103,null,null). Cause: java.lang.IllegalArgumentException: java.lang.NullPointerException: Cannot invoke "java.lang.Number.shortValue()" because the return value of "sun.invoke.util.ValueConversions.primitiveConversion(sun.invoke.util.Wrapper, Object, boolean)" is null

我们会在对象工程类里来处理这个问题。

##  对象工厂
首先我们做一个基本判断，constructorArgTypes要和constructorArgs长度一致，即构造函数的类型列表长度和构造函数的值列表长度相同。
```java
 @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        if (type.equals(AllType.class) && constructorArgTypes != null && constructorArgs != null) {
            if (constructorArgs.size() == constructorArgTypes.size()) {
```
### 处理null值问题
我们会遍历每个参数，然后对于为null的值设置一个默认值。这样就解决了我们关闭了默认构造函数，而表中含有null值，导致Mybatis反射失败的问题。
```java
                for (int i = 0; i < constructorArgs.size(); i++) {
                    Object a = constructorArgs.get(i);
                    if (a == null) {
                        String className = constructorArgTypes.get(i).getName();
                        if (className.equals("int")) {
                            int vi = 0;
                            constructorArgs.set(i, vi);
                        } else if (className.equals("long")) {
                            long vl = 0;
                            constructorArgs.set(i, vl);
                        } else if (className.equals("float")) {
                            float vf = 0;
                            constructorArgs.set(i, vf);
                        } else if (className.equals("double")) {
                            double vd = 0;
                            constructorArgs.set(i, vd);
                        } else if (className.equals("boolean")) {
                            boolean vb = false;
                            constructorArgs.set(i, vb);
                        } else if (className.equals("char")) {
                            char vc = 0;
                            constructorArgs.set(i, vc);
                        } else if (className.equals("byte")) {
                            byte vb = 0;
                            constructorArgs.set(i, vb);
                        } else if (className.equals("short")) {
                            short sb = 0;
                            constructorArgs.set(i, sb);
                        } else {
                            try {
                                Constructor<?> constructor = Class.forName(className).getConstructor();
                                constructorArgs.set(i, constructor.newInstance());
                            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    }
```
### 过滤
```java
                    if (i == 0) {
                        int intInfo = (int)constructorArgs.get(i);
                        if (intInfo > this.maxIntInfo) {
                            return null;
                        }
                    }
                }
            }
        }
        return super.create(type, constructorArgTypes, constructorArgs);
    }
```
我们的过滤条件是：如果表中info_int大于我们定义的最大值，则返回null；否则创建这个对象。
this.maxIntInfo的值来源于配置，读取这个配置的代码如下
```java
    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
        this.maxIntInfo = Integer.parseInt(this.properties.getProperty("max_int_info", "1"));
    }
```
### 完整代码

```java
package org.example.factory;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.example.model.AllType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class ResultFactory extends DefaultObjectFactory {
    @Override
    public <T> T create(Class<T> type) {
        return super.create(type);
    }

    @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        if (type.equals(AllType.class) && constructorArgTypes != null && constructorArgs != null) {
            if (constructorArgs.size() == constructorArgTypes.size()) {
                for (int i = 0; i < constructorArgs.size(); i++) {
                    Object a = constructorArgs.get(i);
                    if (a == null) {
                        String className = constructorArgTypes.get(i).getName();
                        if (className.equals("int")) {
                            int vi = 0;
                            constructorArgs.set(i, vi);
                        } else if (className.equals("long")) {
                            long vl = 0;
                            constructorArgs.set(i, vl);
                        } else if (className.equals("float")) {
                            float vf = 0;
                            constructorArgs.set(i, vf);
                        } else if (className.equals("double")) {
                            double vd = 0;
                            constructorArgs.set(i, vd);
                        } else if (className.equals("boolean")) {
                            boolean vb = false;
                            constructorArgs.set(i, vb);
                        } else if (className.equals("char")) {
                            char vc = 0;
                            constructorArgs.set(i, vc);
                        } else if (className.equals("byte")) {
                            byte vb = 0;
                            constructorArgs.set(i, vb);
                        } else if (className.equals("short")) {
                            short sb = 0;
                            constructorArgs.set(i, sb);
                        } else {
                            try {
                                Constructor<?> constructor = Class.forName(className).getConstructor();
                                constructorArgs.set(i, constructor.newInstance());
                            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (i == 0) {
                        int intInfo = (int)constructorArgs.get(i);
                        if (intInfo > this.maxIntInfo) {
                            return null;
                        }
                    }
                }
            }
        }
        return super.create(type, constructorArgTypes, constructorArgs);
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
        this.maxIntInfo = Integer.parseInt(this.properties.getProperty("max_int_info", "1"));
    }

    @Override
    public <T> boolean isCollection(Class<T> type) {
        return Collection.class.isAssignableFrom(type);
    }

    private Properties properties;
    private int maxIntInfo = 1;
}
```
# 配置
我们只需要在mybatis-config.xml中加入如下配置即可
```xml
    <objectFactory type="org.example.factory.ResultFactory">
        <property name="max_int_info" value="102"/>
    </objectFactory>
```
上面setProperties方法的参数properties就是从这个XML中解析的。
# 测试
相较于之前的测试代码，我们需要做个改动：遍历List\<AllType\>后要判断元素是否为null。因为我们只是没有构造需要过滤的对象，但是返回了null。所以数组中数据个数并没有剔除需要过滤的对象——它们只是被null替换掉了。
```java
package org.example;

import org.example.model.AllType;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.AllTypeMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ResultFactoryTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-result-factory.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    void testFindAll() {
        List<AllType> all = null;
        try (SqlSession s = sqlSF.openSession()) {
            all = s.selectList("org.example.mapper.AllTypeMapper.findAll");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        for (AllType a : Objects.requireNonNull(all)) {
            if (a == null) {
                continue;
            }
            System.out.println(a.getInfo_int());
        }
    }

    @Test
    void testFind() {
        try (SqlSession s = sqlSF.openSession()) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            List<AllType> all = all_type_mapper.find(103);
            for (AllType a : Objects.requireNonNull(all)) {
                if (a == null) {
                    continue;
                }
                System.out.println(a.getInfo_int());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
```

# 参考资料
- [https://mybatis.org/mybatis-3/zh_CN/configuration.html#%E5%AF%B9%E8%B1%A1%E5%B7%A5%E5%8E%82%EF%BC%88objectfactory%EF%BC%89](https://mybatis.org/mybatis-3/zh_CN/configuration.html#%E5%AF%B9%E8%B1%A1%E5%B7%A5%E5%8E%82%EF%BC%88objectfactory%EF%BC%89)
