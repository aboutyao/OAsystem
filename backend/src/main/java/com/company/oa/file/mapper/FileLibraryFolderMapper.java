package com.company.oa.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.file.FileLibraryFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FileLibraryFolderMapper extends BaseMapper<FileLibraryFolder> {

    @Select("""
            select f.id, f.parent_id as parentId, f.folder_name as folderName, f.sort_order as sortOrder,
                   f.status, f.created_by as createdBy, f.created_at as createdAt
            from file_library_folder f
            where f.deleted = 0
            order by f.parent_id, f.sort_order, f.id
            """)
    List<Map<String, Object>> selectFolderTree();

    @Select("""
            select f.id, f.parent_id as parentId, f.folder_name as folderName, f.sort_order as sortOrder,
                   f.status, f.created_by as createdBy, f.created_at as createdAt, f.updated_at as updatedAt
            from file_library_folder f
            where f.id = #{id} and f.deleted = 0
            """)
    List<Map<String, Object>> selectFolderById(@Param("id") long id);

    @Select("""
            select coalesce(max(sort_order),0) from file_library_folder
            where deleted = 0 and ((parent_id is null and #{parentId} is null) or parent_id = #{parentId})
            """)
    Integer selectMaxSort(@Param("parentId") Long parentId);
}
