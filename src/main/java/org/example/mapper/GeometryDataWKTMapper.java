package org.example.mapper;

import java.util.List;

import org.example.model.GeometryData;

public interface GeometryDataWKTMapper {
    public List<GeometryData> findAll();
    public GeometryData find(Long id);
    public Long insertOne(GeometryData geometryData);
    public Long insertList(List<GeometryData> geometryDataList);
    public Long updateOne(GeometryData geometryData);
    public Long insertGeometryCollection(GeometryData geometryData);
} 
