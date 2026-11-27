package com.asset.controller;

import com.asset.common.Result;
import com.asset.entity.SysLog;
import com.asset.entity.SysUser;
import com.asset.mapper.SysLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统只读审计日志接口（不可删除、不可清空，满足审计追溯要求）
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final SysLogMapper logMapper;

    @GetMapping("/page")
    public Result<Page<SysLog>> page(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        SysUser me = (SysUser) req.getAttribute("currentUser");
        if (me == null || !"ASSET_ADMIN".equals(me.getRole())) {
            return Result.error(403, "仅资产管理员可查看审计日志");
        }
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(SysLog::getOperator, keyword)
                    .or().like(SysLog::getModule, keyword)
                    .or().like(SysLog::getDetail, keyword));
        }
        wrapper.orderByDesc(SysLog::getId);
        return Result.ok(logMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
