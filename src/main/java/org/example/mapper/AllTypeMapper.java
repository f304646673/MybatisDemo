package org.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.model.AllType;
import org.example.model.AllTypeRename;

import java.util.List;

public interface AllTypeMapper {
    AllType findOne(int info_int);

    long insertElems(List<AllType> AllTypeList);

    long deleteElemWhereInfoIntLessThen(int info_int);

    long deleteElem(@Param("column_name") String column_name, @Param("comparison_operator") String comparison_operator, @Param("value") int value);

    long updateElems(AllType item);

    AllTypeRename findRenameOne(int intInfo);

    List<AllTypeRename> findRenameList(int intInfo);
}
