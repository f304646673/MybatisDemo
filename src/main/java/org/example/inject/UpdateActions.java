package org.example.inject;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.example.model.AllType;

import java.util.Properties;

@Intercepts(@Signature(
        type = Executor.class,
        method = "update",
        args = {MappedStatement.class, Object.class}
))

public class UpdateActions implements Interceptor {
    private Properties properties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        for (Object a : invocation.getArgs()) {
            if (a.getClass() == AllType.class) {
                Object b = a.getClass().getMethod("getInfo_int").invoke(a);
                if (b.getClass() == Integer.class) {
                    int info_int = (int) b;
                    if (info_int < 0) {
                        a.getClass().getMethod("setInfo_int", int.class).invoke(a, 0);
                    }
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
