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
import java.util.*;

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

            List<AllType> all = null;
            try (SqlSession s = sqlSFLocal.openSession()) {
                all = s.selectList("org.example.mapper.AllTypeMapper.findAll");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            for (AllType a : Objects.requireNonNull(all)) {
                System.out.println(a.getInfo_int());
            }
        }
    }

    @Test
    void testUpdate() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-1.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            AllType a = new AllType();
            a.setInfo_int(105);
            a.setInfo_tint((byte) 20);
            a.setInfo_sint((short) 10);
            long count = all_type_mapper.updateElems(a);
            System.out.println(count);
        }
    }

    @Test
    void testDelete() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-1.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.deleteElemWhereInfoIntLessThen(103);
            System.out.println(count);
            count = all_type_mapper.deleteElem("info_int", "<",105);
            System.out.println(count);
            count = all_type_mapper.deleteElem("info_int", ">",106);
            System.out.println(count);
        }
    }

    @Test
    void testBatchInsert() {
        List<AllType> allTypelist= new LinkedList<>();
        for (byte i = 100; i < 110; i++) {
            AllType allType = new AllType();
            allType.setInfo_int(i);
            allType.setInfo_sint(i);
            allType.setInfo_tint(i);
            allTypelist.add(allType);
        }

        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-1.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            long count = all_type_mapper.insertElems(allTypelist);
            System.out.println(count);
        }
    }

    @Test
    void testLog() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-2.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            AllType a = all_type_mapper.findOne(105);
            if (a != null) {
                System.out.println(a.getInfo_sint());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
