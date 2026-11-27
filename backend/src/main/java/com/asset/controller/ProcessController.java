package com.asset.controller;

import com.asset.common.Result;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Flowable 流程定义查看、在线部署与进度高亮接口
 */
@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
public class ProcessController {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    /**
     * 获取当前已部署的流程列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().desc()
                .list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProcessDefinition def : defs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", def.getId());
            item.put("key", def.getKey());
            item.put("name", def.getName());
            item.put("version", def.getVersion());
            item.put("deploymentId", def.getDeploymentId());
            result.add(item);
        }
        return Result.ok(result);
    }

    /**
     * 获取指定流程定义（或默认主流程）的 XML（供前端 bpmn-js 显示与编辑）
     */
    @GetMapping("/xml")
    public Result<String> getLatestXml(@RequestParam(required = false) String id) throws Exception {
        ProcessDefinition def;
        if (id != null && !id.isEmpty()) {
            def = repositoryService.createProcessDefinitionQuery().processDefinitionId(id).singleResult();
        } else {
            def = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey("asset_approval_process")
                    .latestVersion()
                    .singleResult();
        }
        if (def == null) {
            return Result.error("未找到流程定义");
        }
        try (InputStream is = repositoryService.getResourceAsStream(def.getDeploymentId(), def.getResourceName())) {
            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Result.ok(xml);
        }
    }

    /**
     * 获取指定流程实例的 XML + 已完成节点 + 当前进行中节点（支持多套流程图自适应）
     */
    @GetMapping("/highlight/{processInstanceId}")
    public Result<Map<String, Object>> highlight(@PathVariable String processInstanceId) throws Exception {
        var hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        String xml = null;
        if (hpi != null) {
            ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(hpi.getProcessDefinitionId())
                    .singleResult();
            if (def != null) {
                try (InputStream is = repositoryService.getResourceAsStream(def.getDeploymentId(), def.getResourceName())) {
                    xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        if (xml == null) {
            xml = getLatestXml(null).getData();
        }

        List<HistoricActivityInstance> historyList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        List<String> finishedIds = new ArrayList<>();
        List<String> activeIds = new ArrayList<>();

        for (HistoricActivityInstance act : historyList) {
            if (act.getEndTime() != null) {
                finishedIds.add(act.getActivityId());
            } else {
                activeIds.add(act.getActivityId());
            }
        }

        // 如果流程还在运行，也补充当前正在等待的任务节点
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        Map<String, Object> data = new HashMap<>();
        data.put("xml", xml);
        data.put("finishedIds", finishedIds);
        data.put("activeIds", activeIds);
        data.put("running", pi != null);
        return Result.ok(data);
    }

    /**
     * 在线部署前端 bpmn-js 修改后的流程图 XML
     */
    @PostMapping("/deploy")
    public Result<String> deploy(jakarta.servlet.http.HttpServletRequest req, @RequestBody Map<String, String> body) {
        com.asset.entity.SysUser me = (com.asset.entity.SysUser) req.getAttribute("currentUser");
        if (me == null || !"ASSET_ADMIN".equals(me.getRole())) {
            return Result.error(403, "仅资产管理员可发布审批流程");
        }
        String xml = body.get("xml");
        String name = body.getOrDefault("name", "固定资产审批流程");
        if (xml == null || xml.trim().isEmpty()) {
            return Result.error("流程图 XML 不能为空");
        }
        repositoryService.createDeployment()
                .name(name)
                .addString("asset_approval.bpmn20.xml", xml)
                .deploy();
        return Result.ok("新流程部署成功", null);
    }
}
