一般我们在生产环境中，业务代码是不允许删除数据库中任何一项数据的。只可以通过逻辑删除的形式来表达删除状态，即给表新增一个类似deleted的字段，默认值false表示该项没有被标记为“删除状态”；如果业务代码想删除该条目，则将该条目的deleted设置为true。查询时带上条件deleted=false来查询“存在”的数据。
我们作为代码设计者，可以通过设计Mybatis的拦截器来拦截通过Mybatis执行的Delete操作。
具体做法就是使用插件技术。
# 代码
我们需要设计一个类继承于org.apache.ibatis.plugin.Interceptor，并覆盖intercept方法。
在intercept中，我们需要找到MappedStatement对象，然后查看其SqlCommandType是不是DELETE。如果是DELETE，则抛出异常，告诉开发人员，不能执行Delete操作。否则交给调用器按正常流程调用。
因为Delete行为会修改表中项目，所以它属于更新行为，于是Intercepts注解中，我们给method传递的是update。
```java
package org.example.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.util.Properties;

@Intercepts(@Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
))
public class DeleteInterceptor implements Interceptor {
    private Properties properties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        for (Object a : invocation.getArgs()) {
            if (a.getClass() == MappedStatement.class) {
                MappedStatement statement = (MappedStatement) a;
                if (statement.getSqlCommandType() == SqlCommandType.DELETE) {
                    throw new RuntimeException("Delete operation is not allowed");
                }
            }
        }
        return invocation.proceed();
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
```
# 配置
我们只需要在mybatis-config.xml下新增plugins配置即可。
```xml
    <plugins>
        <plugin interceptor="org.example.interceptor.DeleteInterceptor">
        </plugin>
    </plugins>
```
# 测试
下面代码中两个例子都是试图执行Delete操作，都会抛出异常。
```java
package org.example;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.AllTypeMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

public class InterceptorTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-interceptor.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    void testDeleteElem() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.deleteElem("info_int", "<",105);
            System.out.println(count);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testDeleteElemWhereInfoIntLessThen() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.deleteElemWhereInfoIntLessThen(103);
            System.out.println(count);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
```
# 总结
Mybatis的拦截器除了可以拦截Delete操作，还可以拦截很多Mybatis框架内部其他行为。具体见：
- Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
- ParameterHandler (getParameterObject, setParameters)
- ResultSetHandler (handleResultSets, handleOutputParameters)
- StatementHandler (prepare, parameterize, batch, update, query)

代码样例见：[https://github.com/f304646673/mybatis_demo.git](https://github.com/f304646673/mybatis_demo.git)
# 参考资料
- [https://mybatis.org/mybatis-3/zh_CN/configuration.html#plugins](https://mybatis.org/mybatis-3/zh_CN/configuration.html#plugins)
