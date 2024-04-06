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
