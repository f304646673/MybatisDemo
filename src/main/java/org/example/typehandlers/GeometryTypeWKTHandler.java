package org.example.typehandlers;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GeometryTypeWKTHandler extends BaseTypeHandler<Geometry>  {
    private static GeometryFactory factory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        String str = serializeGeometry(parameter);
        ps.setString(i, str);;
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String str = rs.getString(columnName);
        try {
            return deserializeGeometry(str);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String str = rs.getString(columnIndex);
        try {
            return deserializeGeometry(str);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String str = cs.getString(columnIndex);
        try {
            return deserializeGeometry(str);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    private static String serializeGeometry(Geometry geometry) {
        WKTWriter writer = new WKTWriter(2);
        return writer.write(geometry);
    }

    private static Geometry deserializeGeometry(String wkt) throws ParseException {
        return new WKTReader(factory).read(wkt);
    }

}