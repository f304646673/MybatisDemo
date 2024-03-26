package org.example;

import pojo.AllType;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;

import java.io.InputStream;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
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
            System.out.println(e);
        }

    }
}