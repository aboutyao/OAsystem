package com.company.oa.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.file.FileInfo;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.entity.org.User;
import com.company.oa.file.mapper.FileInfoMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.org.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GlobalSearchService {
    private final UserMapper userMapper;
    private final OaLeaveMapper leaveMapper;
    private final FileInfoMapper fileInfoMapper;

    public GlobalSearchService(UserMapper userMapper, OaLeaveMapper leaveMapper, FileInfoMapper fileInfoMapper) {
        this.userMapper = userMapper;
        this.leaveMapper = leaveMapper;
        this.fileInfoMapper = fileInfoMapper;
    }

    public Map<String, List<Map<String, Object>>> search(String keyword, int limit) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("users", searchUsers(keyword, limit));
        result.put("leaves", searchLeaves(keyword, limit));
        result.put("files", searchFiles(keyword, limit));
        return result;
    }

    private List<Map<String, Object>> searchUsers(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .and(w -> w.like(User::getRealName, keyword)
                    .or().like(User::getUsername, keyword)
                    .or().like(User::getEmployeeNo, keyword))
                .last("LIMIT " + limit));
        for (User u : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getRealName());
            item.put("username", u.getUsername());
            item.put("employeeNo", u.getEmployeeNo());
            item.put("type", "user");
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchLeaves(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<OaLeave> leaves = leaveMapper.selectList(
            new LambdaQueryWrapper<OaLeave>()
                .eq(OaLeave::getDeleted, 0)
                .and(w -> w.like(OaLeave::getCreatedNameSnapshot, keyword)
                    .or().like(OaLeave::getReason, keyword))
                .last("LIMIT " + limit));
        for (OaLeave l : leaves) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", l.getId());
            item.put("leaveType", l.getLeaveType());
            item.put("createdName", l.getCreatedNameSnapshot());
            item.put("reason", l.getReason());
            item.put("status", l.getStatus());
            item.put("startAt", l.getStartAt());
            item.put("endAt", l.getEndAt());
            item.put("type", "leave");
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchFiles(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<FileInfo> files = fileInfoMapper.selectList(
            new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getDeleted, 0)
                .like(FileInfo::getFileName, keyword)
                .last("LIMIT " + limit));
        for (FileInfo f : files) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", f.getId());
            item.put("fileName", f.getFileName());
            item.put("fileExt", f.getFileExt());
            item.put("fileSize", f.getFileSize());
            item.put("type", "file");
            results.add(item);
        }
        return results;
    }
}
