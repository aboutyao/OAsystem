package com.company.oa.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.org.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    @Select("select dept_path from org_dept where id = #{id} and deleted = 0")
    String selectDeptPathById(@Param("id") long id);

    @Update("update org_dept set dept_path = concat(#{newPath}, substring(dept_path, length(#{oldPath}) + 1)), updated_at = now() where id <> #{excludeId} and dept_path like #{oldPathLike}")
    int updateDescendantPaths(@Param("newPath") String newPath,
                               @Param("oldPath") String oldPath,
                               @Param("excludeId") long excludeId,
                               @Param("oldPathLike") String oldPathLike);
}
