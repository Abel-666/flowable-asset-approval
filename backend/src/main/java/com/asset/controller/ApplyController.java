package com.asset.controller;

import com.asset.common.Result;
import com.asset.entity.AssetApply;
import com.asset.entity.AssetInfo;
import com.asset.entity.SysLog;
import com.asset.entity.SysUser;
import com.asset.mapper.AssetApplyMapper;
import com.asset.mapper.AssetInfoMapper;
import com.asset.mapper.SysLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 资产申请与 Flowable 审批流转核心接口
 * 支持 3 套流程：
 * 1. asset_approval_process（采购/领用/空闲借用/报废）
 * 2. repair_process（员工报修 -> 资产管理员审核）
 * 3. peer_borrow_process（借用他人占用设备 -> 占有人同意 -> >1000元上级主管审批）
 */
@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
public class ApplyController {

    private final AssetApplyMapper applyMapper;
    private final AssetInfoMapper assetMapper;
    private final SysLogMapper logMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    private static final AtomicInteger APPLY_SEQ = new AtomicInteger(1);

    private String nextApplyNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = APPLY_SEQ.getAndIncrement() % 1000;
        return String.format("AP-%s-%03d", ts, seq);
    }

    private SysUser currentUser(HttpServletRequest req) {
        return (SysUser) req.getAttribute("currentUser");
    }

    @PostMapping("/submit")
    @Transactional(rollbackFor = Exception.class)
    public Result<AssetApply> submit(HttpServletRequest req, @RequestBody AssetApply apply) {
        SysUser me = currentUser(req);
        String type = apply.getApplyType();
        String processKey = "asset_approval_process";

        Map<String, Object> vars = new HashMap<>();
        vars.put("applyType", type);
        vars.put("applicantName", me.getRealName());

        if (apply.getAssetId() != null) {
            AssetInfo target = assetMapper.selectById(apply.getAssetId());
            if (target == null) {
                return Result.error("关联资产不存在");
            }
            if ("USE".equals(type) && target.getStatus() != 1) {
                return Result.error("只有“在库闲置”的资产才能发起领用");
            }
            if ("BORROW".equals(type)) {
                if (target.getStatus() == 2) {
                    // 设备正在被别人占用：走同级借用流程（先占有人同意，>1000元再找主管）
                    if (me.getId().equals(target.getUserId())) {
                        return Result.error("该设备已经在您名下，无需借用");
                    }
                    if (target.getUserId() == null) {
                        return Result.error("该设备缺少当前使用人信息");
                    }
                    processKey = "peer_borrow_process";
                    vars.put("holderId", String.valueOf(target.getUserId()));
                    apply.setAmount(target.getPrice());
                } else if (target.getStatus() != 1) {
                    return Result.error("只有“在库闲置”或“在用”的设备才能借用");
                }
            }
            if ("REPAIR".equals(type)) {
                if (target.getStatus() == 4 || target.getStatus() == 5) {
                    return Result.error("该资产已在维修中或已报废");
                }
                processKey = "repair_process";
            }
        } else if ("REPAIR".equals(type)) {
            return Result.error("报修必须选择一台现有设备");
        }

        apply.setApplicantId(me.getId());
        apply.setApplicantName(me.getRealName());
        apply.setDeptId(me.getDeptId());
        apply.setApplyNo(nextApplyNo());
        apply.setStatus("PENDING");
        if (apply.getAmount() == null) {
            apply.setAmount(BigDecimal.ZERO);
        }
        applyMapper.insert(apply);

        vars.put("amount", apply.getAmount().doubleValue());
        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                processKey,
                String.valueOf(apply.getId()),
                vars
        );
        apply.setProcessInstanceId(pi.getId());
        applyMapper.updateById(apply);

        saveLog(me, "资产申请", "发起申请", "单号 " + apply.getApplyNo() + "（" + type + " - " + apply.getAssetName() + "）");
        return Result.ok("申请已提交，流程已启动", apply);
    }

    @PutMapping("/resubmit")
    @Transactional(rollbackFor = Exception.class)
    public Result<AssetApply> resubmit(HttpServletRequest req, @RequestBody AssetApply apply) {
        SysUser me = currentUser(req);
        AssetApply existing = applyMapper.selectById(apply.getId());
        if (existing == null) return Result.error("申请单不存在");
        if (!existing.getApplicantId().equals(me.getId()) && !"ASSET_ADMIN".equals(me.getRole())) {
            return Result.error(403, "无权修改他人的申请单");
        }
        apply.setApplicantId(existing.getApplicantId());
        apply.setApplicantName(existing.getApplicantName());
        apply.setDeptId(existing.getDeptId());
        apply.setApplyNo(existing.getApplyNo());
        apply.setStatus("PENDING");
        if (apply.getAmount() == null) apply.setAmount(BigDecimal.ZERO);

        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", apply.getAmount().doubleValue());
        vars.put("applyType", apply.getApplyType());
        vars.put("applicantName", existing.getApplicantName());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                "asset_approval_process",
                String.valueOf(apply.getId()),
                vars
        );
        apply.setProcessInstanceId(pi.getId());
        applyMapper.updateById(apply);

        saveLog(me, "资产申请", "修改重提", "单号 " + apply.getApplyNo());
        return Result.ok("已重新提交审批", apply);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(HttpServletRequest req, @PathVariable Long id) {
        SysUser me = currentUser(req);
        AssetApply existing = applyMapper.selectById(id);
        if (existing == null) return Result.error("申请单不存在");
        if ("FINISHED".equals(existing.getStatus())) {
            return Result.error("已办结归档的申请单不允许删除");
        }
        if (!existing.getApplicantId().equals(me.getId()) && !"ASSET_ADMIN".equals(me.getRole())) {
            return Result.error(403, "无权删除他人的申请单");
        }
        applyMapper.deleteById(id);
        return Result.ok("删除成功", null);
    }

    @GetMapping("/list")
    public Result<List<AssetApply>> list(HttpServletRequest req) {
        SysUser me = currentUser(req);
        LambdaQueryWrapper<AssetApply> wrapper = new LambdaQueryWrapper<>();
        if ("EMPLOYEE".equals(me.getRole())) {
            wrapper.eq(AssetApply::getApplicantId, me.getId());
        }
        wrapper.orderByDesc(AssetApply::getId);
        return Result.ok(applyMapper.selectList(wrapper));
    }

    @GetMapping("/page")
    public Result<Page<AssetApply>> page(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        SysUser me = currentUser(req);
        LambdaQueryWrapper<AssetApply> wrapper = new LambdaQueryWrapper<>();
        if ("EMPLOYEE".equals(me.getRole())) {
            wrapper.eq(AssetApply::getApplicantId, me.getId());
        }
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(AssetApply::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(AssetApply::getApplyNo, keyword).or().like(AssetApply::getAssetName, keyword));
        }
        wrapper.orderByDesc(AssetApply::getId);
        return Result.ok(applyMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /**
     * 查询待办任务：包含发给“我的角色”的审批 + 指定给“我本人”的借用确认
     */
    @GetMapping("/todo")
    public Result<List<Map<String, Object>>> todo(HttpServletRequest req) {
        SysUser me = currentUser(req);
        List<Task> tasks = taskService.createTaskQuery()
                .or()
                .taskCandidateGroup(me.getRole())
                .taskAssignee(String.valueOf(me.getId()))
                .endOr()
                .orderByTaskCreateTime().desc()
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            AssetApply apply = applyMapper.selectOne(
                    new LambdaQueryWrapper<AssetApply>()
                            .eq(AssetApply::getProcessInstanceId, task.getProcessInstanceId())
            );
            if (apply != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("taskId", task.getId());
                item.put("taskName", task.getName());
                item.put("createTime", task.getCreateTime());
                item.put("apply", apply);
                result.add(item);
            }
        }
        return Result.ok(result);
    }

    @GetMapping("/done")
    public Result<List<AssetApply>> done(HttpServletRequest req) {
        SysUser me = currentUser(req);
        LambdaQueryWrapper<AssetApply> wrapper = new LambdaQueryWrapper<AssetApply>()
                .ne(AssetApply::getStatus, "PENDING");
        if ("EMPLOYEE".equals(me.getRole())) {
            wrapper.eq(AssetApply::getApplicantId, me.getId());
        }
        wrapper.orderByDesc(AssetApply::getUpdateTime);
        return Result.ok(applyMapper.selectList(wrapper));
    }

    private Task findAuthorizedTask(String taskId, SysUser me) {
        return taskService.createTaskQuery()
                .taskId(taskId)
                .or()
                .taskCandidateGroup(me.getRole())
                .taskAssignee(String.valueOf(me.getId()))
                .endOr()
                .singleResult();
    }

    @PostMapping("/approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> approve(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        SysUser me = currentUser(req);
        String taskId = String.valueOf(body.get("taskId"));
        Long applyId = Long.valueOf(String.valueOf(body.get("applyId")));
        String comment = String.valueOf(body.getOrDefault("comment", "同意"));

        Task task = findAuthorizedTask(taskId, me);
        if (task == null) {
            return Result.error(403, "无权审批该任务，或任务已被处理");
        }

        taskService.addComment(taskId, task.getProcessInstanceId(), me.getRealName() + "：" + comment);
        taskService.complete(taskId);

        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();

        AssetApply apply = applyMapper.selectById(applyId);
        if (pi == null && apply != null) {
            apply.setStatus("FINISHED");
            applyMapper.updateById(apply);
            handleAssetAfterFinish(apply);
        }
        saveLog(me, "流程审批", "审批通过", "节点：" + task.getName() + "，单号：" + (apply != null ? apply.getApplyNo() : applyId));
        return Result.ok("审批通过", null);
    }

    @PostMapping("/reject")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> reject(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        SysUser me = currentUser(req);
        String taskId = String.valueOf(body.get("taskId"));
        Long applyId = Long.valueOf(String.valueOf(body.get("applyId")));
        String comment = String.valueOf(body.getOrDefault("comment", "驳回"));

        Task task = findAuthorizedTask(taskId, me);
        if (task == null) {
            return Result.error(403, "无权驳回该任务，或任务已被处理");
        }

        taskService.addComment(taskId, task.getProcessInstanceId(), me.getRealName() + "（驳回）：" + comment);
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "审批驳回：" + comment);

        AssetApply apply = applyMapper.selectById(applyId);
        if (apply != null) {
            apply.setStatus("REJECTED");
            applyMapper.updateById(apply);
        }
        saveLog(me, "流程审批", "审批驳回", "节点：" + task.getName() + "，原因：" + comment);
        return Result.ok("已驳回该申请", null);
    }

    @GetMapping("/history/{processInstanceId}")
    public Result<List<Map<String, Object>>> history(@PathVariable String processInstanceId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("userTask")
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
        Map<String, String> commentMap = new HashMap<>();
        for (Comment c : comments) {
            commentMap.put(c.getTaskId(), c.getFullMessage());
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (HistoricActivityInstance act : activities) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskName", act.getActivityName());
            item.put("startTime", act.getStartTime());
            item.put("endTime", act.getEndTime());
            item.put("comment", commentMap.getOrDefault(act.getTaskId(), act.getEndTime() == null ? "等待审批中..." : "已处理"));
            list.add(item);
        }
        return Result.ok(list);
    }

    private void handleAssetAfterFinish(AssetApply apply) {
        String type = apply.getApplyType();
        if ("PURCHASE".equals(type)) {
            int count = apply.getQuantity() != null && apply.getQuantity() > 0 ? apply.getQuantity() : 1;
            BigDecimal unitPrice = apply.getAmount().divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
            for (int i = 0; i < count; i++) {
                AssetInfo info = new AssetInfo();
                info.setAssetCode(AssetController.nextAssetCode());
                info.setAssetName(apply.getAssetName());
                info.setCategory(apply.getCategory() != null ? apply.getCategory() : "办公设备");
                info.setSpec(apply.getSpec());
                info.setPrice(unitPrice);
                info.setStatus(1);
                info.setDeptId(apply.getDeptId());
                info.setInboundTime(LocalDateTime.now());
                info.setRemark("采购单 " + apply.getApplyNo() + " 自动入库");
                assetMapper.insert(info);
            }
        } else if (apply.getAssetId() != null) {
            AssetInfo asset = assetMapper.selectById(apply.getAssetId());
            if (asset == null) {
                throw new RuntimeException("关联资产不存在");
            }
            if ("USE".equals(type)) {
                if (asset.getStatus() != 1) {
                    throw new RuntimeException("该资产已被他人占用，无法重复领用");
                }
                asset.setStatus(2);
                asset.setUserId(apply.getApplicantId());
                asset.setDeptId(apply.getDeptId());
            } else if ("BORROW".equals(type)) {
                if (asset.getStatus() != 1 && asset.getStatus() != 2) {
                    throw new RuntimeException("该资产当前不可借出");
                }
                asset.setStatus(3);
                asset.setUserId(apply.getApplicantId());
                asset.setDeptId(apply.getDeptId());
            } else if ("REPAIR".equals(type)) {
                asset.setStatus(4);
            } else if ("SCRAP".equals(type)) {
                asset.setStatus(5);
                asset.setUserId(null);
            }
            assetMapper.updateById(asset);
        }
    }

    private void saveLog(SysUser user, String module, String action, String detail) {
        SysLog log = new SysLog();
        log.setUserId(user.getId());
        log.setOperator(user.getRealName());
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }
}
