package com.asset.controller;

import com.asset.common.Result;
import com.asset.entity.AssetInfo;
import com.asset.entity.SysDept;
import com.asset.entity.SysUser;
import com.asset.mapper.AssetInfoMapper;
import com.asset.mapper.SysDeptMapper;
import com.asset.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 固定资产台账接口（展示使用人/部门、支持“我的名下资产”筛选、分页与状态机守卫）
 */
@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetInfoMapper assetMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private static final AtomicInteger CODE_SEQ = new AtomicInteger(1);

    public static String nextAssetCode() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = CODE_SEQ.getAndIncrement() % 1000;
        return String.format("FA-%s-%03d", ts, seq);
    }

    private boolean isAdmin(HttpServletRequest req) {
        SysUser u = (SysUser) req.getAttribute("currentUser");
        return u != null && "ASSET_ADMIN".equals(u.getRole());
    }

    private void fillNames(List<AssetInfo> list) {
        if (list == null || list.isEmpty()) return;
        Map<Long, String> userMap = new HashMap<>();
        userMapper.selectList(null).forEach(u -> userMap.put(u.getId(), u.getRealName()));
        Map<Long, String> deptMap = new HashMap<>();
        deptMapper.selectList(null).forEach(d -> deptMap.put(d.getId(), d.getDeptName()));

        for (AssetInfo a : list) {
            a.setUserName(a.getUserId() != null ? userMap.getOrDefault(a.getUserId(), "未知") : "-");
            a.setDeptName(a.getDeptId() != null ? deptMap.getOrDefault(a.getDeptId(), "未分配") : "未分配");
        }
    }

    @GetMapping("/list")
    public Result<List<AssetInfo>> list(
            HttpServletRequest req,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onlyMine) {
        List<AssetInfo> list = assetMapper.selectList(buildWrapper(req, status, keyword, onlyMine));
        fillNames(list);
        return Result.ok(list);
    }

    @GetMapping("/page")
    public Result<Page<AssetInfo>> page(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onlyMine) {
        Page<AssetInfo> res = assetMapper.selectPage(new Page<>(page, size), buildWrapper(req, status, keyword, onlyMine));
        fillNames(res.getRecords());
        return Result.ok(res);
    }

    private LambdaQueryWrapper<AssetInfo> buildWrapper(HttpServletRequest req, Integer status, String keyword, Boolean onlyMine) {
        LambdaQueryWrapper<AssetInfo> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(onlyMine)) {
            SysUser me = (SysUser) req.getAttribute("currentUser");
            if (me != null) {
                wrapper.eq(AssetInfo::getUserId, me.getId());
            }
        }
        if (status != null) {
            wrapper.eq(AssetInfo::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(AssetInfo::getAssetName, keyword)
                    .or().like(AssetInfo::getAssetCode, keyword));
        }
        wrapper.orderByDesc(AssetInfo::getId);
        return wrapper;
    }

    @PostMapping("/add")
    public Result<AssetInfo> add(HttpServletRequest req, @RequestBody AssetInfo asset) {
        if (!isAdmin(req)) return Result.error(403, "仅资产管理员可执行入库操作");
        asset.setAssetCode(nextAssetCode());
        asset.setStatus(1);
        asset.setInboundTime(LocalDateTime.now());
        assetMapper.insert(asset);
        return Result.ok("入库成功", asset);
    }

    @PutMapping("/update")
    public Result<String> update(HttpServletRequest req, @RequestBody AssetInfo asset) {
        if (!isAdmin(req)) return Result.error(403, "仅资产管理员可修改资产信息");
        AssetInfo existing = assetMapper.selectById(asset.getId());
        if (existing != null && existing.getStatus() == 5) {
            return Result.error("已报废的资产不可修改");
        }
        assetMapper.updateById(asset);
        return Result.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(HttpServletRequest req, @PathVariable Long id) {
        if (!isAdmin(req)) return Result.error(403, "仅资产管理员可删除资产");
        assetMapper.deleteById(id);
        return Result.ok("删除成功", null);
    }

    @PostMapping("/batch-delete")
    public Result<String> batchDelete(HttpServletRequest req, @RequestBody List<Long> ids) {
        if (!isAdmin(req)) return Result.error(403, "仅资产管理员可批量删除资产");
        if (ids != null && !ids.isEmpty()) {
            assetMapper.deleteBatchIds(ids);
        }
        return Result.ok("批量删除成功", null);
    }

    @PutMapping("/return/{id}")
    public Result<String> returnAsset(HttpServletRequest req, @PathVariable Long id) {
        SysUser me = (SysUser) req.getAttribute("currentUser");
        AssetInfo asset = assetMapper.selectById(id);
        if (asset == null) return Result.error("资产不存在");
        if (asset.getStatus() != 2 && asset.getStatus() != 3) {
            return Result.error("只有“在用”或“借出”状态的资产才能归还");
        }
        if (!me.getId().equals(asset.getUserId())) {
            return Result.error(403, "只能由当前使用人本人操作归还，管理员可发送催还通知");
        }
        asset.setStatus(1);
        asset.setUserId(null);
        assetMapper.updateById(asset);
        return Result.ok("资产已归还入库", null);
    }

    @PutMapping("/repair/{id}")
    public Result<String> toggleRepair(HttpServletRequest req, @PathVariable Long id) {
        if (!isAdmin(req)) return Result.error(403, "普通员工请通过「申请报修」提交资产管理员审核");
        AssetInfo asset = assetMapper.selectById(id);
        if (asset == null) return Result.error("资产不存在");
        if (asset.getStatus() == 3 || asset.getStatus() == 5) {
            return Result.error("借出中或已报废的资产不可直接操作维修");
        }
        asset.setStatus(asset.getStatus() == 4 ? 1 : 4);
        assetMapper.updateById(asset);
        return Result.ok("状态更新成功", null);
    }
}
