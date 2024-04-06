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
//        super.setProperties(properties);
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