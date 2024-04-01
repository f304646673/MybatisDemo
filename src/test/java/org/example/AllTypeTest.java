package org.example;

import org.example.model.AllType;
import org.example.mapper.AllTypeMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AllTypeTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    void testFindAll() {
        try {
            List<AllType> all;
            try (SqlSession s = sqlSF.openSession()) {
                all = s.selectList("org.example.mapper.AllTypeMapper.findAll");
            }
            for (AllType a : all) {
                System.out.println(a.getInfo_int());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testFindOne() {
        try {
            try (SqlSession s = sqlSF.openSession()) {
                AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
                AllType a = all_type_mapper.findOne(1);
                System.out.println(a.getInfo_int());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
