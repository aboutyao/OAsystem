package com.company.oa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.system.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    @Select("select config_value from sys_config where config_key = #{key}")
    String selectValueByKey(@Param("key") String key);
}
