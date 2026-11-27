package com.asset.controller;

import com.asset.common.Result;
import com.asset.entity.AssetApply;
import com.asset.entity.AssetInfo;
import com.asset.entity.SysDept;
import com.asset.entity.SysUser;
import com.asset.mapper.AssetApplyMapper;
import com.asset.mapper.AssetInfoMapper;
import com.asset.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

/**
 * 首页可视化看板统计接口（含财务金额权限隔离）
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AssetInfoMapper assetMapper;
    private final AssetApplyMapper applyMapper;
    private final SysDeptMapper deptMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest req) {
        SysUser me = (SysUser) req.getAttribute("currentUser");
        boolean canViewAmount = me != null && ("FINANCE_MANAGER".equals(me.getRole()) || "ASSET_ADMIN".equals(me.getRole()));

        List<AssetInfo> allAssets = assetMapper.selectList(null);
        long pendingCount = applyMapper.selectCount(
                new LambdaQueryWrapper<AssetApply>().eq(AssetApply::getStatus, "PENDING")
        );

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal myAssetAmount = BigDecimal.ZERO;
        long myAssetCount = 0;
        Map<Integer, Integer> statusCount = new HashMap<>();
        Map<Long, Integer> deptCount = new HashMap<>();

        for (AssetInfo a : allAssets) {
            if (canViewAmount && a.getPrice() != null) {
                totalAmount = totalAmount.add(a.getPrice());
            }
            if (me != null && me.getId().equals(a.getUserId())) {
                myAssetCount++;
                if (a.getPrice() != null) {
                    myAssetAmount = myAssetAmount.add(a.getPrice());
                }
            }
            statusCount.merge(a.getStatus(), 1, Integer::sum);
            if (a.getDeptId() != null) {
                deptCount.merge(a.getDeptId(), 1, Integer::sum);
            }
        }

        // 组装饼图数据（资产状态分布）
        List<Map<String, Object>> statusChart = List.of(
                Map.of("name", "在库闲置", "value", statusCount.getOrDefault(1, 0)),
                Map.of("name", "在用", "value", statusCount.getOrDefault(2, 0)),
                Map.of("name", "借出", "value", statusCount.getOrDefault(3, 0)),
                Map.of("name", "维修中", "value", statusCount.getOrDefault(4, 0)),
                Map.of("name", "已报废", "value", statusCount.getOrDefault(5, 0))
        );

        // 组装柱状图数据（各部门资产数量）
        List<SysDept> depts = deptMapper.selectList(null);
        List<String> deptNames = new ArrayList<>();
        List<Integer> deptValues = new ArrayList<>();
        for (SysDept d : depts) {
            deptNames.add(d.getDeptName());
            deptValues.add(deptCount.getOrDefault(d.getId(), 0));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalCount", allAssets.size());
        data.put("canViewAmount", canViewAmount);
        data.put("totalAmount", canViewAmount ? totalAmount : null);
        data.put("myAssetCount", myAssetCount);
        data.put("myAssetAmount", myAssetAmount);
        data.put("inStockCount", statusCount.getOrDefault(1, 0));
        data.put("pendingCount", pendingCount);
        data.put("statusChart", statusChart);
        data.put("deptNames", deptNames);
        data.put("deptValues", deptValues);
        return Result.ok(data);
    }
}
