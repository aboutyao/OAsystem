package com.company.oa.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.file.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    @Select("""
            select f.id, f.folder_id as folderId, d.folder_name as folderName, f.file_name as fileName,
                   f.file_ext as fileExt, f.mime_type as mimeType, f.file_size as fileSize, f.storage_path as storagePath,
                   f.upload_user_id as uploadUserId, u.real_name as uploadUserName,
                   f.status, f.created_at as createdAt, f.updated_at as updatedAt, f.version
            from file_info f
            left join file_library_folder d on d.id = f.folder_id
            left join org_user u on u.id = f.upload_user_id
            where f.id = #{id} and f.deleted = 0
            """)
    List<Map<String, Object>> selectFileDetail(@Param("id") long id);

    @Select("""
            <script>
            select f.id, f.folder_id as folderId, d.folder_name as folderName, f.file_name as fileName,
                   f.file_ext as fileExt, f.mime_type as mimeType, f.file_size as fileSize, f.storage_path as storagePath,
                   f.upload_user_id as uploadUserId, u.real_name as uploadUserName,
                   f.status, f.created_at as createdAt, f.updated_at as updatedAt, f.version
            from file_info f
            left join file_library_folder d on d.id = f.folder_id
            left join org_user u on u.id = f.upload_user_id
            where f.deleted = 0
            <if test='folderId != null'>and f.folder_id = #{folderId}</if>
            <if test='keyword != null and keyword != ""'>and f.file_name like CONCAT('%', #{keyword}, '%')</if>
            order by f.created_at desc, f.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectFiles(@Param("folderId") Long folderId,
                                          @Param("keyword") String keyword,
                                          @Param("limit") long limit,
                                          @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from file_info f where f.deleted = 0
            <if test='folderId != null'>and f.folder_id = #{folderId}</if>
            <if test='keyword != null and keyword != ""'>and f.file_name like CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    Long countFiles(@Param("folderId") Long folderId, @Param("keyword") String keyword);

    @Select("""
            <script>
            select f.id, f.folder_id as folderId, d.folder_name as folderName, f.file_name as fileName,
                   f.file_ext as fileExt, f.mime_type as mimeType, f.file_size as fileSize,
                   f.upload_user_id as uploadUserId, u.real_name as uploadUserName,
                   f.status, f.created_at as createdAt, f.updated_at as updatedAt
            from file_info f
            left join file_library_folder d on d.id = f.folder_id
            left join org_user u on u.id = f.upload_user_id
            where f.deleted = 1
            <if test='keyword != null and keyword != ""'>and f.file_name like CONCAT('%', #{keyword}, '%')</if>
            order by f.updated_at desc, f.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectDeletedFiles(@Param("keyword") String keyword,
                                                  @Param("limit") long limit,
                                                  @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from file_info f where f.deleted = 1
            <if test='keyword != null and keyword != ""'>and f.file_name like CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    Long countDeletedFiles(@Param("keyword") String keyword);

    @Select("""
            select id, file_name from file_info where id = #{id} and deleted = 1
            """)
    List<Map<String, Object>> selectDeletedFileById(@Param("id") long id);

    @Select("""
            select l.id, l.file_id as fileId, l.user_id as userId, u.real_name as userName,
                   l.business_type as businessType, l.business_id as businessId,
                   l.ip_address as ipAddress, l.user_agent as userAgent, l.downloaded_at as downloadedAt
            from file_download_log l
            left join org_user u on u.id = l.user_id
            where l.file_id = #{fileId}
            order by l.downloaded_at desc, l.id desc
            """)
    List<Map<String, Object>> selectDownloadLogs(@Param("fileId") long fileId);

    @Delete("delete from file_download_log where file_id = #{fileId}")
    int deleteDownloadLogsByFileId(@Param("fileId") long fileId);

    @Delete("delete from file_info where id = #{id} and deleted = 1")
    int physicalDeleteFile(@Param("id") long id);

    @Update("update file_info set deleted = 0, status = 'NORMAL', updated_at = NOW(), version = version + 1 where id = #{id} and deleted = 1")
    int restoreFile(@Param("id") long id);
}
