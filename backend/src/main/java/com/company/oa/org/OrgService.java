package com.company.oa.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.org.Dept;
import com.company.oa.entity.org.Position;
import com.company.oa.entity.org.Rank;
import com.company.oa.entity.org.User;
import com.company.oa.entity.org.UserDept;
import com.company.oa.entity.perm.PermRole;
import com.company.oa.entity.perm.PermUserRole;
import com.company.oa.org.mapper.ChangeLogMapper;
import com.company.oa.org.mapper.DeptMapper;
import com.company.oa.org.mapper.PositionMapper;
import com.company.oa.org.mapper.RankMapper;
import com.company.oa.org.mapper.UserDeptMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.permission.mapper.PermRoleMapper;
import com.company.oa.permission.mapper.PermUserRoleMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgService {
    private static final String ENABLED = "ENABLED";
    private static final String DISABLED = "DISABLED";
    private static final String MAIN = "MAIN";
    private static final String ACTIVE = "ACTIVE";

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PositionMapper positionMapper;
    private final RankMapper rankMapper;
    private final UserDeptMapper userDeptMapper;
    private final ChangeLogMapper changeLogMapper;
    private final PermUserRoleMapper permUserRoleMapper;
    private final PermRoleMapper permRoleMapper;
    private final SysConfigMapper sysConfigMapper;
    private final PasswordEncoder passwordEncoder;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public OrgService(UserMapper userMapper, DeptMapper deptMapper, PositionMapper positionMapper,
                      RankMapper rankMapper, UserDeptMapper userDeptMapper, ChangeLogMapper changeLogMapper,
                      PermUserRoleMapper permUserRoleMapper, PermRoleMapper permRoleMapper,
                      SysConfigMapper sysConfigMapper, PasswordEncoder passwordEncoder,
                      SequenceService sequenceService, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.positionMapper = positionMapper;
        this.rankMapper = rankMapper;
        this.userDeptMapper = userDeptMapper;
        this.changeLogMapper = changeLogMapper;
        this.permUserRoleMapper = permUserRoleMapper;
        this.permRoleMapper = permRoleMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.passwordEncoder = passwordEncoder;
        this.sequenceService = sequenceService;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> entityToMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> deptTree() {
        List<Dept> depts = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>()
                        .orderByAsc(Dept::getSortOrder)
                        .orderByAsc(Dept::getId));
        List<Map<String, Object>> rows = depts.stream().map(this::entityToMap).toList();

        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(id, node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            Map<String, Object> node = byId.get(id);
            Object pid = row.get("parentId");
            if (pid == null) {
                roots.add(node);
            } else {
                long parentId = ((Number) pid).longValue();
                Map<String, Object> parent = byId.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        sortDeptChildren(roots);
        return roots;
    }

    private void sortDeptChildren(List<Map<String, Object>> nodes) {
        nodes.sort(Comparator.comparingInt(n -> ((Number) n.getOrDefault("sortOrder", 0)).intValue()));
        for (Map<String, Object> n : nodes) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ch = (List<Map<String, Object>>) n.get("children");
            if (ch != null && !ch.isEmpty()) {
                sortDeptChildren(ch);
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> deptDetail(long id) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        return entityToMap(dept);
    }

    @Transactional
    public Map<String, Object> createDept(OrgDtos.DeptCreateRequest req) {
        if (req.parentId() != null && !deptExists(req.parentId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上级部门不存在");
        }
        if (codeDeptTaken(req.deptCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门编码已存在");
        }
        long id = nextId("org_dept");
        String path = buildDeptPath(req.parentId(), id);
        int sort = req.sortOrder() == null ? 0 : req.sortOrder();
        LocalDateTime now = LocalDateTime.now();
        Dept dept = new Dept();
        dept.setId(id);
        dept.setParentId(req.parentId());
        dept.setDeptCode(req.deptCode());
        dept.setDeptName(req.deptName());
        dept.setDeptPath(path);
        dept.setLeaderUserId(req.leaderUserId());
        dept.setSortOrder(sort);
        dept.setStatus(ENABLED);
        dept.setCreatedAt(now);
        dept.setUpdatedAt(now);
        dept.setDeleted(0);
        deptMapper.insert(dept);
        return deptDetail(id);
    }

    @Transactional
    public Map<String, Object> updateDept(long id, OrgDtos.DeptUpdateRequest req) {
        Map<String, Object> before = deptDetail(id);
        if (req.parentId() != null) {
            if (req.parentId() == id) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能将部门设为自己的上级");
            }
            if (!deptExists(req.parentId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "上级部门不存在");
            }
            if (isDescendant(id, req.parentId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不能将上级设为本部门的下级");
            }
        }
        if (codeDeptTaken(req.deptCode(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门编码已存在");
        }
        String oldPath = String.valueOf(before.get("deptPath"));
        String newPath = buildDeptPath(req.parentId(), id);
        LocalDateTime now = LocalDateTime.now();
        if (!oldPath.equals(newPath)) {
            deptMapper.updateDescendantPaths(newPath, oldPath, id, oldPath + "%");
        }
        int sort = req.sortOrder() == null ? ((Number) before.getOrDefault("sortOrder", 0)).intValue() : req.sortOrder();
        Dept dept = new Dept();
        dept.setParentId(req.parentId());
        dept.setDeptCode(req.deptCode());
        dept.setDeptName(req.deptName());
        dept.setDeptPath(newPath);
        dept.setLeaderUserId(req.leaderUserId());
        dept.setSortOrder(sort);
        dept.setUpdatedAt(now);
        deptMapper.update(dept, new LambdaQueryWrapper<Dept>().eq(Dept::getId, id));
        return deptDetail(id);
    }

    @Transactional
    public Map<String, Object> setDeptStatus(long id, String status) {
        deptDetail(id);
        Dept dept = new Dept();
        dept.setId(id);
        dept.setStatus(status);
        dept.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(dept);
        return deptDetail(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> deptUsers(long deptId, long page, long size) {
        deptDetail(deptId);
        long[] ps = clampPage(page, size);
        long total = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .and(w -> w.eq(User::getMainDeptId, deptId)
                                .or().exists("select 1 from org_user_dept ud where ud.user_id = org_user.id and ud.dept_id = " + deptId)));
        List<Map<String, Object>> items = userMapper.selectDeptUsers(deptId, ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listUsers(long page, long size, String keyword,
                                                       Long mainDeptId, String employeeStatus,
                                                       String accountStatus) {
        long[] ps = clampPage(page, size);
        String kw = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;

        // Build count query with optional filters
        var countWrapper = new LambdaQueryWrapper<User>().eq(User::getDeleted, 0);
        if (kw != null) {
            countWrapper.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getEmployeeNo, kw));
        }
        if (mainDeptId != null) {
            countWrapper.eq(User::getMainDeptId, mainDeptId);
        }
        if (StringUtils.hasText(employeeStatus)) {
            countWrapper.eq(User::getEmployeeStatus, employeeStatus);
        }
        if (StringUtils.hasText(accountStatus)) {
            countWrapper.eq(User::getAccountStatus, accountStatus);
        }
        long total = userMapper.selectCount(countWrapper);

        String es = StringUtils.hasText(employeeStatus) ? employeeStatus.trim() : null;
        String accs = StringUtils.hasText(accountStatus) ? accountStatus.trim() : null;
        List<Map<String, Object>> items = userMapper.selectUserList(kw, ps[1], (ps[0] - 1) * ps[1],
                mainDeptId, es, accs);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> userDetail(long id) {
        Map<String, Object> user = userMapper.selectUserDetailById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>(user);
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(id);
        result.put("roleIds", roleIds);
        return result;
    }

    @Transactional
    public Map<String, Object> createUser(OrgDtos.UserCreateRequest req) {
        if (!deptExists(req.mainDeptId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "主部门不存在");
        }
        if (usernameTaken(req.username(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (employeeNoTaken(req.employeeNo(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "工号已存在");
        }
        if (req.managerUserId() != null && !userExists(req.managerUserId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "直属上级不存在");
        }
        long id = nextId("org_user");
        String rawPassword = StringUtils.hasText(req.password()) ? req.password() : defaultTempPassword();
        String hash = passwordEncoder.encode(rawPassword);
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(id);
        user.setUsername(req.username());
        user.setPasswordHash(hash);
        user.setEmployeeNo(req.employeeNo());
        user.setRealName(req.realName());
        user.setMobile(emptyToNull(req.mobile()));
        user.setEmail(emptyToNull(req.email()));
        user.setMainDeptId(req.mainDeptId());
        user.setPositionId(req.positionId());
        user.setRankId(req.rankId());
        user.setManagerUserId(req.managerUserId());
        user.setEmployeeStatus(ACTIVE);
        user.setAccountStatus(ENABLED);
        user.setEntryDate(LocalDate.now());
        user.setPasswordChangedAt(now);
        user.setLoginFailCount(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        userMapper.insert(user);
        upsertMainDeptRelation(id, req.mainDeptId());
        List<Long> roles = (req.roleIds() == null || req.roleIds().isEmpty()) ? List.of(3L) : req.roleIds();
        replaceUserRoles(id, roles);
        return userDetail(id);
    }

    @Transactional
    public Map<String, Object> updateUser(long id, OrgDtos.UserUpdateRequest req) {
        userDetail(id);
        if (!deptExists(req.mainDeptId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "主部门不存在");
        }
        if (employeeNoTaken(req.employeeNo(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "工号已存在");
        }
        if (req.managerUserId() != null && (!userExists(req.managerUserId()) || req.managerUserId() == id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "直属上级无效");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setEmployeeNo(req.employeeNo());
        user.setRealName(req.realName());
        user.setMainDeptId(req.mainDeptId());
        user.setPositionId(req.positionId());
        user.setRankId(req.rankId());
        user.setUpdatedAt(now);
        userMapper.update(user,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, id)
                        .set(User::getMobile, emptyToNull(req.mobile()))
                        .set(User::getEmail, emptyToNull(req.email()))
                        .set(User::getManagerUserId, req.managerUserId()));
        upsertMainDeptRelation(id, req.mainDeptId());
        if (req.roleIds() != null) {
            replaceUserRoles(id, req.roleIds());
        }
        return userDetail(id);
    }

    @Transactional
    public Map<String, Object> enableUser(long id) {
        userDetail(id);
        User user = new User();
        user.setId(id);
        user.setAccountStatus(ENABLED);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return userDetail(id);
    }

    @Transactional
    public Map<String, Object> disableUser(long id) {
        userDetail(id);
        User user = new User();
        user.setId(id);
        user.setAccountStatus(DISABLED);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return userDetail(id);
    }

    @Transactional
    public Map<String, Object> resignUser(long id) {
        userDetail(id);
        LocalDateTime now = LocalDateTime.now();
        userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, id)
                        .set(User::getEmployeeStatus, "RESIGNED")
                        .set(User::getAccountStatus, DISABLED)
                        .set(User::getResignDate, LocalDate.now())
                        .set(User::getUpdatedAt, now));
        return userDetail(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> contacts(String keyword) {
        String kw = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        if (kw != null) {
            return userMapper.selectContactsWithKeyword(kw);
        }
        return userMapper.selectAllContacts();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPositions() {
        List<Position> positions = positionMapper.selectList(
                new LambdaQueryWrapper<Position>()
                        .orderByAsc(Position::getSortOrder)
                        .orderByAsc(Position::getId));
        return positions.stream().map(this::entityToMap).toList();
    }

    @Transactional
    public Map<String, Object> createPosition(OrgDtos.PositionUpsertRequest req) {
        if (codePositionTaken(req.positionCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "岗位编码已存在");
        }
        long id = nextId("org_position");
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        int sort = req.sortOrder() == null ? 0 : req.sortOrder();
        Position position = new Position();
        position.setId(id);
        position.setPositionCode(req.positionCode());
        position.setPositionName(req.positionName());
        position.setStatus(status);
        position.setSortOrder(sort);
        position.setRemark(req.remark());
        positionMapper.insert(position);
        return positionRow(id);
    }

    @Transactional
    public Map<String, Object> updatePosition(long id, OrgDtos.PositionUpsertRequest req) {
        positionRow(id);
        if (codePositionTaken(req.positionCode(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "岗位编码已存在");
        }
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        int sort = req.sortOrder() == null ? 0 : req.sortOrder();
        Position position = new Position();
        position.setId(id);
        position.setPositionCode(req.positionCode());
        position.setPositionName(req.positionName());
        position.setStatus(status);
        position.setSortOrder(sort);
        position.setRemark(req.remark());
        positionMapper.updateById(position);
        return positionRow(id);
    }

    @Transactional
    public void deletePosition(long id) {
        positionRow(id);
        Long used = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPositionId, id)
                        .eq(User::getDeleted, 0));
        if (used != null && used > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该岗位下仍有用户，无法删除");
        }
        positionMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRanks() {
        List<Rank> ranks = rankMapper.selectList(
                new LambdaQueryWrapper<Rank>()
                        .orderByDesc(Rank::getRankLevel)
                        .orderByAsc(Rank::getId));
        return ranks.stream().map(this::entityToMap).toList();
    }

    @Transactional
    public Map<String, Object> createRank(OrgDtos.RankUpsertRequest req) {
        if (codeRankTaken(req.rankCode(), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "职级编码已存在");
        }
        long id = nextId("org_rank");
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        Rank rank = new Rank();
        rank.setId(id);
        rank.setRankCode(req.rankCode());
        rank.setRankName(req.rankName());
        rank.setRankLevel(req.rankLevel());
        rank.setStatus(status);
        rank.setRemark(req.remark());
        rankMapper.insert(rank);
        return rankRow(id);
    }

    @Transactional
    public Map<String, Object> updateRank(long id, OrgDtos.RankUpsertRequest req) {
        rankRow(id);
        if (codeRankTaken(req.rankCode(), id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "职级编码已存在");
        }
        String status = StringUtils.hasText(req.status()) ? req.status() : ENABLED;
        Rank rank = new Rank();
        rank.setId(id);
        rank.setRankCode(req.rankCode());
        rank.setRankName(req.rankName());
        rank.setRankLevel(req.rankLevel());
        rank.setStatus(status);
        rank.setRemark(req.remark());
        rankMapper.updateById(rank);
        return rankRow(id);
    }

    @Transactional
    public void deleteRank(long id) {
        rankRow(id);
        Long used = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRankId, id)
                        .eq(User::getDeleted, 0));
        if (used != null && used > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该职级下仍有用户，无法删除");
        }
        rankMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public String exportUsersCsv() {
        List<Map<String, Object>> rows = userMapper.selectAllUsersForExport();
        StringBuilder sb = new StringBuilder("﻿id,username,employeeNo,realName,mainDeptName,employeeStatus,accountStatus,mobile,email\n");
        for (Map<String, Object> r : rows) {
            sb.append(csv(r.get("id"))).append(',')
                    .append(csv(r.get("username"))).append(',')
                    .append(csv(r.get("employee_no"))).append(',')
                    .append(csv(r.get("real_name"))).append(',')
                    .append(csv(r.get("dept_name"))).append(',')
                    .append(csv(r.get("employee_status"))).append(',')
                    .append(csv(r.get("account_status"))).append(',')
                    .append(csv(r.get("mobile"))).append(',')
                    .append(csv(r.get("email"))).append('\n');
        }
        return sb.toString();
    }

    @Transactional
    public OrgDtos.UserImportResult importUsers(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        if (file == null || file.isEmpty()) {
            errors.add("文件为空");
            return new OrgDtos.UserImportResult(0, 0, errors);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                errors.add("无表头");
                return new OrgDtos.UserImportResult(0, 0, errors);
            }
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < 4) {
                    errors.add("第" + lineNo + "行列数不足(需 username,employeeNo,realName,mainDeptId)");
                    skipped++;
                    continue;
                }
                String username = cols[0].trim();
                String employeeNo = cols[1].trim();
                String realName = cols[2].trim();
                long mainDeptId;
                try {
                    mainDeptId = Long.parseLong(cols[3].trim());
                } catch (NumberFormatException ex) {
                    errors.add("第" + lineNo + "行 mainDeptId 无效");
                    skipped++;
                    continue;
                }
                try {
                    createUser(new OrgDtos.UserCreateRequest(
                            username, employeeNo, realName, null, null, mainDeptId, null, null, null, null, List.of()
                    ));
                    created++;
                } catch (BusinessException ex) {
                    errors.add("第" + lineNo + "行: " + ex.getMessage());
                    skipped++;
                }
            }
        } catch (Exception ex) {
            errors.add("读取失败: " + ex.getMessage());
        }
        return new OrgDtos.UserImportResult(created, skipped, errors);
    }

    private static String csv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private Map<String, Object> positionRow(long id) {
        Position position = positionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "岗位不存在");
        }
        return entityToMap(position);
    }

    private Map<String, Object> rankRow(long id) {
        Rank rank = rankMapper.selectById(id);
        if (rank == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "职级不存在");
        }
        return entityToMap(rank);
    }

    private void upsertMainDeptRelation(long userId, long mainDeptId) {
        userDeptMapper.delete(
                new LambdaQueryWrapper<UserDept>()
                        .eq(UserDept::getUserId, userId)
                        .eq(UserDept::getRelationType, MAIN));
        long relId = nextId("org_user_dept");
        UserDept ud = new UserDept();
        ud.setId(relId);
        ud.setUserId(userId);
        ud.setDeptId(mainDeptId);
        ud.setRelationType(MAIN);
        ud.setStartDate(LocalDate.now());
        ud.setCreatedAt(LocalDateTime.now());
        userDeptMapper.insert(ud);
    }

    private void replaceUserRoles(long userId, List<Long> roleIds) {
        permUserRoleMapper.delete(
                new LambdaQueryWrapper<PermUserRole>()
                        .eq(PermUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<Long> distinct = roleIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long roleId : distinct) {
            Long exists = permRoleMapper.selectCount(
                    new LambdaQueryWrapper<PermRole>()
                            .eq(PermRole::getId, roleId)
                            .eq(PermRole::getStatus, ENABLED));
            if (exists == null || exists == 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不存在: " + roleId);
            }
            PermUserRole pur = new PermUserRole();
            pur.setId(nextId("perm_user_role"));
            pur.setUserId(userId);
            pur.setRoleId(roleId);
            pur.setCreatedAt(LocalDateTime.now());
            permUserRoleMapper.insert(pur);
        }
    }

    @Transactional
    public Map<String, Object> resetPassword(long userId) {
        userDetail(userId);
        String tempPassword = stringConfig("security.defaultPassword", "Temp@123456");
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setLoginFailCount(0);
        user.setUpdatedAt(now);
        int updated = userMapper.update(user,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getLockedUntil, null));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return Map.of("success", true, "tempPassword", tempPassword);
    }

    private String stringConfig(String key, String defaultValue) {
        String value = sysConfigMapper.selectValueByKey(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String defaultTempPassword() {
        String value = sysConfigMapper.selectValueByKey("security.user.defaultTempPassword");
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return "ChangeMe123!";
    }

    private long[] clampPage(long page, long size) {
        int def = intConfig("paging.defaultSize", 20);
        int max = intConfig("paging.maxSize", 100);
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? def : size;
        if (s > max) {
            s = max;
        }
        return new long[]{p, s};
    }

    private int intConfig(String key, int defaultValue) {
        String value = sysConfigMapper.selectValueByKey(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    private boolean deptExists(long id) {
        Long n = deptMapper.selectCount(
                new LambdaQueryWrapper<Dept>().eq(Dept::getId, id));
        return n != null && n > 0;
    }

    private boolean userExists(long id) {
        Long n = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getId, id));
        return n != null && n > 0;
    }

    private boolean usernameTaken(String username, Long excludeId) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0);
        if (excludeId != null) {
            w.ne(User::getId, excludeId);
        }
        Long n = userMapper.selectCount(w);
        return n != null && n > 0;
    }

    private boolean employeeNoTaken(String employeeNo, Long excludeId) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<User>()
                .eq(User::getEmployeeNo, employeeNo)
                .eq(User::getDeleted, 0);
        if (excludeId != null) {
            w.ne(User::getId, excludeId);
        }
        Long n = userMapper.selectCount(w);
        return n != null && n > 0;
    }

    private boolean codeDeptTaken(String code, Long excludeId) {
        LambdaQueryWrapper<Dept> w = new LambdaQueryWrapper<Dept>()
                .eq(Dept::getDeptCode, code);
        if (excludeId != null) {
            w.ne(Dept::getId, excludeId);
        }
        Long n = deptMapper.selectCount(w);
        return n != null && n > 0;
    }

    private boolean codePositionTaken(String code, Long excludeId) {
        LambdaQueryWrapper<Position> w = new LambdaQueryWrapper<Position>()
                .eq(Position::getPositionCode, code);
        if (excludeId != null) {
            w.ne(Position::getId, excludeId);
        }
        Long n = positionMapper.selectCount(w);
        return n != null && n > 0;
    }

    private boolean codeRankTaken(String code, Long excludeId) {
        LambdaQueryWrapper<Rank> w = new LambdaQueryWrapper<Rank>()
                .eq(Rank::getRankCode, code);
        if (excludeId != null) {
            w.ne(Rank::getId, excludeId);
        }
        Long n = rankMapper.selectCount(w);
        return n != null && n > 0;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listChangeLogs(long page, long size, String targetType, String changeType) {
        long[] ps = clampPage(page, size);
        String tt = StringUtils.hasText(targetType) ? targetType.trim() : null;
        String ct = StringUtils.hasText(changeType) ? changeType.trim() : null;
        long total = changeLogMapper.selectChangeLogCount(tt, ct);
        List<Map<String, Object>> items = changeLogMapper.selectChangeLogList(tt, ct, ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    private long nextId(String table) {
        return sequenceService.nextId(table);
    }

    private String buildDeptPath(Long parentId, long id) {
        if (parentId == null) {
            return "/" + id + "/";
        }
        String parentPath = deptMapper.selectDeptPathById(parentId);
        if (parentPath == null) {
            return "/" + id + "/";
        }
        if (!parentPath.endsWith("/")) {
            parentPath = parentPath + "/";
        }
        return parentPath + id + "/";
    }

    private boolean isDescendant(long ancestorId, long possibleDescendantId) {
        if (ancestorId == possibleDescendantId) {
            return true;
        }
        List<Dept> allDepts = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>()
                        .select(Dept::getId, Dept::getParentId)
                        .eq(Dept::getDeleted, 0));
        Map<Long, Long> parentMap = new LinkedHashMap<>();
        for (Dept d : allDepts) {
            parentMap.put(d.getId(), d.getParentId());
        }
        Long cur = possibleDescendantId;
        int guard = 0;
        while (cur != null && guard++ < 1000) {
            if (cur == ancestorId) {
                return true;
            }
            cur = parentMap.get(cur);
        }
        return false;
    }

    private String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }
}
