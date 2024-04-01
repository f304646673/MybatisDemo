package com.all_type;

import com.all_type.AllType;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

public class AllTypeTest {
    AllType allType;

    @Test
    void testFindAll() {
        try {
            InputStream in = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSF = new SqlSessionFactoryBuilder().build(in);
            List<AllType> all;
            try (SqlSession s = sqlSF.openSession()) {
                all = s.selectList("AllTypeMapper.findAll");
            }
            for (AllType a : all) {
                System.out.println(a.getInfo_int());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
