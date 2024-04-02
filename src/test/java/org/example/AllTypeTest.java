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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AllTypeTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config.xml");
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
            System.out.println(a.getInfo_int());
        }
    }

    @Test
    void testFindOne() {
        try (SqlSession s = sqlSF.openSession()) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            AllType a = all_type_mapper.findOne(1);
            if (a != null) {
                System.out.println(a.getInfo_int());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

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
            try (SqlSession s = sqlSFLocal.openSession()) {
                AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
                AllType a = all_type_mapper.findOne(11);
                if (a != null) {
                    System.out.println(a.getInfo_int());
                }
            }
        }
    }
}
