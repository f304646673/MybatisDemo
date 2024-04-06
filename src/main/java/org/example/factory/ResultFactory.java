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
        if (type.equals(AllType.class)) {
            if (constructorArgs.size() == constructorArgTypes.size()) {
                for (int i = 0; i < constructorArgTypes.size(); i++) {
                    Object a = constructorArgs.get(i);
                    if (a == null) {
                        String className = constructorArgTypes.get(i).getName();
                        if (className.equals("byte")) {
                            byte vb = 0;
                            constructorArgs.set(i, vb);
                        } else if (className.equals("short")) {
                            short sb = 0;
                            constructorArgs.set(i, sb);
                        }
                    }
                }
            }
        }
        return super.create(type, constructorArgTypes, constructorArgs);
    }

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
    }

    @Override
    public <T> boolean isCollection(Class<T> type) {
        return Collection.class.isAssignableFrom(type);
    }
}