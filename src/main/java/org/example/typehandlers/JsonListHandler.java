package org.example.typehandlers;

import com.alibaba.fastjson.JSON;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.example.model.JsonType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.LONGVARCHAR)
public class JsonListHandler extends BaseTypeHandler<JsonType.JsonList> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JsonType.JsonList parameter, JdbcType jdbcType) throws SQLException {
        String jsonStr = JSON.toJSONString(parameter);
        ps.setString(i, jsonStr);
    }

    @Override
    public JsonType.JsonList getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonStr = rs.getString(columnName);
        return JSON.parseObject(jsonStr, JsonType.JsonList.class);
    }


    @Override
    public JsonType.JsonList getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonStr = rs.getString(columnIndex);
        return JSON.parseObject(jsonStr, JsonType.JsonList.class);
    }

    @Override
    public JsonType.JsonList getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonStr = cs.getString(columnIndex);
        return JSON.parseObject(jsonStr, JsonType.JsonList.class);
    }
}
