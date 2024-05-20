package org.example.model;

import lombok.Data;
import java.sql.Timestamp;
import org.locationtech.jts.geom.Geometry;

@Data
public class GeometryData {
    private Long id;
    private Geometry geometry;
    private Timestamp createTime;
    private Timestamp updateTime;
    private boolean deleted;
}