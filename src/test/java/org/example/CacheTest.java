package org.example;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.AllTypeMapper;
import org.example.model.AllType;
import org.example.model.JsonType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CacheTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-json-cache.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    void testUpdate() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            JsonType a = new JsonType();
            a.setIntInfo(1);

            List<JsonType.JsonElem> jsonElemList = Arrays.asList(
                    new JsonType.JsonElem(1,"1"),
                    new JsonType.JsonElem(2,"2")
            );
            JsonType.JsonList jsonList = new JsonType.JsonList(jsonElemList);

            a.setJsonElemList(jsonList);
            long count = all_type_mapper.updateJsonTypeElems(a);
            System.out.println(count);
        }
    }


    @Test
    void testSelect() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            List<JsonType> all = all_type_mapper.selectJsonTypeElems(100);
            for (JsonType a : Objects.requireNonNull(all)) {
                JsonType.JsonList jsonList = a.getJsonElemList();
                if (null == jsonList) {
                    continue;
                }
                for (JsonType.JsonElem b: jsonList.getJsonElemList()) {
                    System.out.printf("%d %s\n", b.getFirst(), b.getSecond());
                }
            }
        }
    }


    @Test
    void testinsertJsonTypeElems() {
        List<JsonType> jsonTypeList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            JsonType a = new JsonType();
            a.setIntInfo(i+100);

            List<JsonType.JsonElem> jsonElemList = Arrays.asList(
                    new JsonType.JsonElem(i+1100, "1"),
                    new JsonType.JsonElem(i+1200, "2")
            );
            JsonType.JsonList jsonList = new JsonType.JsonList(jsonElemList);
            a.setJsonElemList(jsonList);
            jsonTypeList.add(a);
        }


        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.insertJsonTypeElems(jsonTypeList);
            System.out.println(count);
        }
    }

    @Test
    void testDelete() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.deleteJsonTypeElems(110);
            System.out.println(count);
        }
    }
}
