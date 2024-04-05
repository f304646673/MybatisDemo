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
            AllType a = new AllType();
            a.setInfo_int(10);
            a.setInfo_tint((byte) 20);
            a.setInfo_sint((short) 10);
            long count = all_type_mapper.updateElems(a);
            System.out.println(count);
        }
    }
}
