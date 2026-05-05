package com.company.oa.ops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.ops.AppExceptionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AppExceptionLogMapper extends BaseMapper<AppExceptionLog> {

    @Select("""
            <script>
            select id, request_id as requestId, request_uri as requestUri, request_method as requestMethod,
                   user_id as userId, exception_class as exceptionClass, exception_message as exceptionMessage,
                   severity, occurred_at as occurredAt
            from app_exception_log
            <where>
                <if test="severity != null and severity != ''">and severity = #{severity}</if>
            </where>
            order by occurred_at desc, id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectPageByConditions(@Param("severity") String severity,
                                                      @Param("limit") long limit,
                                                      @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from app_exception_log
            <where>
                <if test="severity != null and severity != ''">and severity = #{severity}</if>
            </where>
            </script>
            """)
    Long countByConditions(@Param("severity") String severity);
}