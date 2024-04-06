package org.example;

import org.example.model.AllType;
import org.example.mapper.AllTypeMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.model.AllTypeRename;
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
    void testFind() {
        try (SqlSession s = sqlSF.openSession()) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            List<AllType> all = all_type_mapper.find(101);
            for (AllType a : Objects.requireNonNull(all)) {
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
            AllType a = new AllType(0, (byte)0, (short)0);
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
            AllType allType = new AllType(0, (byte)0, (short)0);
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
            List<AllType> all = all_type_mapper.find(105);
            for (AllType a : Objects.requireNonNull(all)) {
                System.out.println(a.getInfo_int());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testRenameOne() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-rename.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession()) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            AllTypeRename a = all_type_mapper.findRenameOne(1);
            if (a != null) {
                System.out.println(a.getShortInfo());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void testRenameList() {
        InputStream in = null;
        try {
            in = Resources.getResourceAsStream("mybatis/config/mybatis-config-rename.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SqlSessionFactory sqlSFLocal = new SqlSessionFactoryBuilder().build(in);
        try (SqlSession s = sqlSFLocal.openSession()) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            List<AllTypeRename> all = null;
            all = all_type_mapper.findRenameList(1);
            for (AllTypeRename a : Objects.requireNonNull(all)) {
                System.out.println(a.getShortInfo());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
