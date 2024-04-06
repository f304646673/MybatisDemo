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

public class TestInterceptor {
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
