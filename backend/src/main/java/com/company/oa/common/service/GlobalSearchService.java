package com.company.oa.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.contract.ContractInfo;
import com.company.oa.entity.file.FileInfo;
import com.company.oa.entity.notice.OaNotice;
import com.company.oa.entity.oa.OaExpense;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.entity.oa.OaPurchase;
import com.company.oa.entity.oa.OaSealApply;
import com.company.oa.entity.org.User;
import com.company.oa.entity.wf.WfProcessInstance;
import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.file.mapper.FileInfoMapper;
import com.company.oa.notice.mapper.OaNoticeMapper;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.oa.mapper.OaPurchaseMapper;
import com.company.oa.oa.mapper.OaSealApplyMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
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
    private final OaExpenseMapper expenseMapper;
    private final OaPurchaseMapper purchaseMapper;
    private final ContractInfoMapper contractInfoMapper;
    private final OaSealApplyMapper sealApplyMapper;
    private final OaNoticeMapper noticeMapper;
    private final WfProcessInstanceMapper processInstanceMapper;

    public GlobalSearchService(UserMapper userMapper,
                               OaLeaveMapper leaveMapper,
                               FileInfoMapper fileInfoMapper,
                               OaExpenseMapper expenseMapper,
                               OaPurchaseMapper purchaseMapper,
                               ContractInfoMapper contractInfoMapper,
                               OaSealApplyMapper sealApplyMapper,
                               OaNoticeMapper noticeMapper,
                               WfProcessInstanceMapper processInstanceMapper) {
        this.userMapper = userMapper;
        this.leaveMapper = leaveMapper;
        this.fileInfoMapper = fileInfoMapper;
        this.expenseMapper = expenseMapper;
        this.purchaseMapper = purchaseMapper;
        this.contractInfoMapper = contractInfoMapper;
        this.sealApplyMapper = sealApplyMapper;
        this.noticeMapper = noticeMapper;
        this.processInstanceMapper = processInstanceMapper;
    }

    public Map<String, Object> search(String keyword, int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> users = searchUsers(keyword, limit);
        List<Map<String, Object>> leaves = searchLeaves(keyword, limit);
        List<Map<String, Object>> files = searchFiles(keyword, limit);
        List<Map<String, Object>> expenses = searchExpenses(keyword, limit);
        List<Map<String, Object>> purchases = searchPurchases(keyword, limit);
        List<Map<String, Object>> contracts = searchContracts(keyword, limit);
        List<Map<String, Object>> seals = searchSeals(keyword, limit);
        List<Map<String, Object>> notices = searchNotices(keyword, limit);
        List<Map<String, Object>> workflows = searchWorkflows(keyword, limit);

        // Category totals
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("users", users.size());
        totals.put("leaves", leaves.size());
        totals.put("files", files.size());
        totals.put("expenses", expenses.size());
        totals.put("purchases", purchases.size());
        totals.put("contracts", contracts.size());
        totals.put("seals", seals.size());
        totals.put("notices", notices.size());
        totals.put("workflows", workflows.size());
        result.put("totals", totals);

        // Search results by category
        result.put("users", users);
        result.put("leaves", leaves);
        result.put("files", files);
        result.put("expenses", expenses);
        result.put("purchases", purchases);
        result.put("contracts", contracts);
        result.put("seals", seals);
        result.put("notices", notices);
        result.put("workflows", workflows);

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

    private List<Map<String, Object>> searchExpenses(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<OaExpense> expenses = expenseMapper.selectList(
            new LambdaQueryWrapper<OaExpense>()
                .eq(OaExpense::getDeleted, 0)
                .and(w -> w.like(OaExpense::getExpenseNo, keyword)
                    .or().like(OaExpense::getReason, keyword)
                    .or().like(OaExpense::getCreatedNameSnapshot, keyword))
                .last("LIMIT " + limit));
        for (OaExpense e : expenses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.getId());
            item.put("expenseNo", e.getExpenseNo());
            item.put("expenseType", e.getExpenseType());
            item.put("totalAmount", e.getTotalAmount());
            item.put("reason", e.getReason());
            item.put("status", e.getStatus());
            item.put("createdName", e.getCreatedNameSnapshot());
            item.put("type", "expense");
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchPurchases(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<OaPurchase> purchases = purchaseMapper.selectList(
            new LambdaQueryWrapper<OaPurchase>()
                .eq(OaPurchase::getDeleted, 0)
                .and(w -> w.like(OaPurchase::getPurchaseNo, keyword)
                    .or().like(OaPurchase::getSupplierName, keyword)
                    .or().like(OaPurchase::getReason, keyword)
                    .or().like(OaPurchase::getCreatedNameSnapshot, keyword))
                .last("LIMIT " + limit));
        for (OaPurchase p : purchases) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("purchaseNo", p.getPurchaseNo());
            item.put("purchaseType", p.getPurchaseType());
            item.put("supplierName", p.getSupplierName());
            item.put("totalAmount", p.getTotalAmount());
            item.put("reason", p.getReason());
            item.put("status", p.getStatus());
            item.put("createdName", p.getCreatedNameSnapshot());
            item.put("type", "purchase");
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchContracts(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<ContractInfo> contracts = contractInfoMapper.selectList(
            new LambdaQueryWrapper<ContractInfo>()
                .eq(ContractInfo::getDeleted, 0)
                .and(w -> w.like(ContractInfo::getContractNo, keyword)
                    .or().like(ContractInfo::getContractName, keyword)
                    .or().like(ContractInfo::getCounterparty, keyword)
                    .or().like(ContractInfo::getCreatedNameSnapshot, keyword))
                .last("LIMIT " + limit));
        for (ContractInfo c : contracts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("contractNo", c.getContractNo());
            item.put("contractName", c.getContractName());
            item.put("contractType", c.getContractType());
            item.put("counterparty", c.getCounterparty());
            item.put("amount", c.getAmount());
            item.put("status", c.getStatus());
            item.put("startDate", c.getStartDate());
            item.put("endDate", c.getEndDate());
            item.put("createdName", c.getCreatedNameSnapshot());
            item.put("type", "contract");
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchSeals(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<OaSealApply> seals = sealApplyMapper.selectList(
            new LambdaQueryWrapper<OaSealApply>()
                .eq(OaSealApply::getDeleted, 0)
                .and(w -> w.like(OaSealApply::getSealName, keyword)
                    .or().like(OaSealApply::getFileTitle, keyword)
                    .or().like(OaSealApply::getUseReason, keyword)
                    .or().like(OaSealApply::getCreatedNameSnapshot, keyword))
                .last("LIMIT " + limit));
        for (OaSealApply s : seals) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("sealType", s.getSealType());
            item.put("sealName", s.getSealName());
            item.put("fileTitle", s.getFileTitle());
            item.put("useReason", s.getUseReason());
            item.put("status", s.getStatus());
            item.put("createdName", s.getCreatedNameSnapshot());
            item.put("type", "seal");
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchNotices(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<OaNotice> notices = noticeMapper.selectList(
            new LambdaQueryWrapper<OaNotice>()
                .eq(OaNotice::getDeleted, 0)
                .and(w -> w.like(OaNotice::getTitle, keyword)
                    .or().like(OaNotice::getContent, keyword)
                    .or().like(OaNotice::getNoticeNo, keyword)
                    .or().like(OaNotice::getCreatedNameSnapshot, keyword))
                .last("LIMIT " + limit));
        for (OaNotice n : notices) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId());
            item.put("noticeNo", n.getNoticeNo());
            item.put("title", n.getTitle());
            item.put("category", n.getCategory());
            item.put("noticeType", n.getNoticeType());
            item.put("status", n.getStatus());
            item.put("publishAt", n.getPublishAt());
            item.put("createdName", n.getCreatedNameSnapshot());
            item.put("type", "notice");
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchWorkflows(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<WfProcessInstance> workflows = processInstanceMapper.selectList(
            new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getDeleted, 0)
                .and(w -> w.like(WfProcessInstance::getTitle, keyword)
                    .or().like(WfProcessInstance::getStarterNameSnapshot, keyword)
                    .or().like(WfProcessInstance::getBusinessType, keyword))
                .last("LIMIT " + limit));
        for (WfProcessInstance wf : workflows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", wf.getId());
            item.put("processInstanceId", wf.getProcessInstanceId());
            item.put("title", wf.getTitle());
            item.put("businessType", wf.getBusinessType());
            item.put("starterName", wf.getStarterNameSnapshot());
            item.put("currentNodeName", wf.getCurrentNodeName());
            item.put("status", wf.getStatus());
            item.put("startedAt", wf.getStartedAt());
            item.put("endedAt", wf.getEndedAt());
            item.put("type", "workflow");
            results.add(item);
        }
        return results;
    }
}
