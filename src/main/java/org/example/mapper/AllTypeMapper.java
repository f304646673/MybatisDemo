package org.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.model.AllType;

import java.util.List;

public interface AllTypeMapper {
    AllType findOne(int info_int);

    int insertElems(List<AllType> AllTypeList);

    int deleteElem(@Param("column_name") String column_name, @Param("value") int value);

    boolean updateElems(AllType item);
}
