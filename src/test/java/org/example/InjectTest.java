package org.example;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.AllTypeMapper;
import org.example.model.AllType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class InjectTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-inject.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    void testUpdate() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            AllType a = new AllType(0, (byte)0, (short)0);
            a.setInfo_int(10);
            a.setInfo_tint((byte) 20);
            a.setInfo_sint((short) 10);
            long count = all_type_mapper.updateElems(a);
            System.out.println(count);
        }
    }

    @Test
    void testSelect() {
        try (SqlSession s = sqlSF.openSession(true)) {
            AllTypeMapper all_type_mapper = s.getMapper(AllTypeMapper.class);
            List<AllType> all = all_type_mapper.find(101);
            for (AllType a : Objects.requireNonNull(all)) {
                System.out.println(a.getInfo_int());
            }
        }
    }
}
