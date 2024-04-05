package org.example;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.AllTypeMapper;
import org.example.mapper.ShirtsMapper;
import org.example.model.Shirts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class ShirtsTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-shirts.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    void testShirtsSelect() {
        try (SqlSession s = sqlSF.openSession()) {
            ShirtsMapper shirts_mapper = s.getMapper(ShirtsMapper.class);
            List<Shirts> all = null;
            all = shirts_mapper.findShirts(Shirts.ShirtSize.small);
            for (Shirts a : Objects.requireNonNull(all)) {
                System.out.printf("%s %s\n",
                        a.getName(),
                        a.getSize());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
