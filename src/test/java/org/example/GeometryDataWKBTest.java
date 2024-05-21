package org.example;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.example.mapper.GeometryDataWKBMapper;
import org.example.model.GeometryData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class GeometryDataWKBTest {
    private static SqlSessionFactory sqlSF;

    @BeforeAll
    static void CreateSessionFactory() throws IOException {
        InputStream in = Resources.getResourceAsStream("mybatis/config/mybatis-config-geometry-wkb.xml");
        sqlSF = new SqlSessionFactoryBuilder().build(in);
    }

    @Test
    public void testFindAll() {
        List<GeometryData> all = null;
        try (SqlSession session = sqlSF.openSession()) {
            all = session.getMapper(GeometryDataWKBMapper.class).findAll();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        for (GeometryData a : Objects.requireNonNull(all)) {
            System.out.println(a.getGeometry());
        }
    }

    @Test
    public void testFind() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);
            GeometryData one = GeometryDataWKBMapper.find(1L);
            System.out.println(one.getGeometry());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testInsert() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);
            GeometryData geometryData = new GeometryData();
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate coordinate = new Coordinate(1, 1);
            Geometry geometry = geometryFactory.createPoint(coordinate);
            geometryData.setGeometry(geometry);
            long count = GeometryDataWKBMapper.insertOne(geometryData);
            System.out.println(count);
            session.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testUpdate() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);
            GeometryData geometryData = new GeometryData();
            GeometryFactory geometryFactory = new GeometryFactory();
            Coordinate coordinate = new Coordinate(2, 2);
            Geometry geometry = geometryFactory.createPoint(coordinate);
            geometryData.setId(1L);
            geometryData.setGeometry(geometry);
            long count = GeometryDataWKBMapper.updateOne(geometryData);
            System.out.println(count);
            session.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testInsertList() {
        try (SqlSession session = sqlSF.openSession()) {
            GeometryDataWKBMapper GeometryDataWKBMapper = session.getMapper(GeometryDataWKBMapper.class);

            List<GeometryData> geometryDataList = new ArrayList<>();
            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                Coordinate coordinate = new Coordinate(3, 3);
                Geometry geometry = geometryFactory.createPoint(coordinate);
                geometryData.setGeometry(geometry);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                LineString lineString = geometryFactory
                        .createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) });
                geometryData.setGeometry(lineString);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                MultiLineString multiLineString = geometryFactory.createMultiLineString(new LineString[] {
                        geometryFactory
                                .createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
                        geometryFactory
                                .createLineString(new Coordinate[] { new Coordinate(3, 3), new Coordinate(4, 4) })
                });
                geometryData.setGeometry(multiLineString);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[] {
                        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
                                new Coordinate(3, 3), new Coordinate(1, 1) }),
                        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(4, 4), new Coordinate(5, 5),
                                new Coordinate(6, 6), new Coordinate(4, 4) })
                });
                geometryData.setGeometry(multiPolygon);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                LinearRing linearRing = geometryFactory.createLinearRing(new Coordinate[] { new Coordinate(1, 1),
                        new Coordinate(2, 2), new Coordinate(3, 3), new Coordinate(1, 1) });
                geometryData.setGeometry(linearRing);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                MultiPoint multiPoint = geometryFactory.createMultiPointFromCoords(
                        new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2), new Coordinate(3, 3) });
                geometryData.setGeometry(multiPoint);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                Polygon polygon = geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1),
                        new Coordinate(2, 2), new Coordinate(3, 3), new Coordinate(1, 1) });
                geometryData.setGeometry(polygon);
                geometryDataList.add(geometryData);
            }

            {
                GeometryData geometryData = new GeometryData();
                GeometryFactory geometryFactory = new GeometryFactory();
                GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(new Geometry[] {
                        geometryFactory.createPoint(new Coordinate(1, 1)),
                        geometryFactory.createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2) }),
                        geometryFactory.createPolygon(new Coordinate[] { new Coordinate(1, 1), new Coordinate(2, 2),
                                new Coordinate(3, 3), new Coordinate(1, 1) })
                });
                geometryData.setGeometry(geometryCollection);
                geometryDataList.add(geometryData);
            }

            long count = GeometryDataWKBMapper.insertList(geometryDataList);
            System.out.println(count);
            session.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }
}
