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
