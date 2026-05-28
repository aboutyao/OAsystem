package com.company.oa.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysSequenceMapper {

    @Update("UPDATE sys_sequence SET current_value = current_value + 1 WHERE seq_name = #{seqName}")
    int increment(@Param("seqName") String seqName);

    @Select("SELECT current_value FROM sys_sequence WHERE seq_name = #{seqName} FOR UPDATE")
    Long getCurrentValueForUpdate(@Param("seqName") String seqName);

    @Select("SELECT COUNT(*) FROM sys_sequence WHERE seq_name = #{seqName} AND current_value >= #{maxId}")
    Long countReached(@Param("seqName") String seqName, @Param("maxId") long maxId);

    @Update("UPDATE sys_sequence SET current_value = #{value} WHERE seq_name = #{seqName}")
    int setCurrentValue(@Param("seqName") String seqName, @Param("value") long value);
}
