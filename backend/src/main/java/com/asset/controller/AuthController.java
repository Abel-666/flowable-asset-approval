package com.asset.controller;

import com.asset.common.Result;
import com.asset.common.TokenStore;
import com.asset.entity.SysDept;
import com.asset.entity.SysLog;
import com.asset.entity.SysPwdReset;
import com.asset.entity.SysUser;
import com.asset.mapper.SysDeptMapper;
import com.asset.mapper.SysLogMapper;
import com.asset.mapper.SysPwdResetMapper;
import com.asset.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录鉴权与用户部门管理接口（BCrypt 密码加密 + 服务端 Token + 管理员权限校验）
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysLogMapper logMapper;
    private final SysPwdResetMapper pwdResetMapper;
    private final TokenStore tokenStore;
    private final BCryptPasswordEncoder passwordEncoder;

    private SysUser currentUser(HttpServletRequest req) {
        return (SysUser) req.getAttribute("currentUser");
    }

    private boolean isAdmin(HttpServletRequest req) {
        SysUser u = currentUser(req);
        return u != null && "ASSET_ADMIN".equals(u.getRole());
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("账号或密码错误");
        }

        String token = tokenStore.createToken(user);
        SysDept dept = user.getDeptId() != null ? deptMapper.selectById(user.getDeptId()) : null;

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("deptId", user.getDeptId());
        info.put("deptName", dept != null ? dept.getDeptName() : "未分配");
        info.put("role", user.getRole());
        info.put("roleName", user.getRoleName());
        info.put("token", token);

        SysLog log = new SysLog();
        log.setUserId(user.getId());
        log.setOperator(user.getRealName());
        log.setModule("系统登录");
        log.setAction("用户登录");
        log.setDetail("账号 " + user.getUsername() + " 登录系统");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);

        return Result.ok("登录成功", info);
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest req) {
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        tokenStore.removeToken(token);
        return Result.ok("已安全退出", null);
    }

    @GetMapping("/users")
    public Result<List<SysUser>> listUsers(HttpServletRequest req) {
        if (!isAdmin(req)) {
            return Result.error(403, "无权限查看用户列表");
        }
        List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
        // 返回给前端时不暴露密码哈希
        list.forEach(u -> u.setPassword(null));
        return Result.ok(list);
    }

    @PostMapping("/users")
    public Result<String> addUser(HttpServletRequest req, @RequestBody SysUser user) {
        if (!isAdmin(req)) {
            return Result.error(403, "无权限新增用户");
        }
        String rawPwd = (user.getPassword() == null || user.getPassword().isEmpty()) ? "123456" : user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPwd));
        userMapper.insert(user);
        return Result.ok("新增用户成功", null);
    }

    @PutMapping("/users")
    public Result<String> updateUser(HttpServletRequest req, @RequestBody SysUser user) {
        if (!isAdmin(req)) {
            return Result.error(403, "无权限修改用户");
        }
        user.setPassword(null); // 防止误覆盖密码
        userMapper.updateById(user);
        return Result.ok("修改用户成功", null);
    }

    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(HttpServletRequest req, @PathVariable Long id) {
        if (!isAdmin(req)) {
            return Result.error(403, "无权限删除用户");
        }
        if (currentUser(req).getId().equals(id)) {
            return Result.error("不能删除当前正在登录的账号");
        }
        userMapper.deleteById(id);
        return Result.ok("删除用户成功", null);
    }

    @PutMapping("/users/{id}/reset-pwd")
    public Result<String> resetPwd(HttpServletRequest req, @PathVariable Long id) {
        if (!isAdmin(req)) {
            return Result.error(403, "无权限重置密码");
        }
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(user);
        }
        return Result.ok("密码已重置为 123456", null);
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        SysUser me = currentUser(req);
        SysUser user = userMapper.selectById(me.getId());
        // 安全规范：个人中心仅允许修改本人姓名，所属部门与角色只能由管理员在用户管理中调整
        user.setRealName(String.valueOf(body.get("realName")));
        userMapper.updateById(user);
        SysDept dept = user.getDeptId() != null ? deptMapper.selectById(user.getDeptId()) : null;

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("deptId", user.getDeptId());
        info.put("deptName", dept != null ? dept.getDeptName() : "未分配");
        info.put("role", user.getRole());
        info.put("roleName", user.getRoleName());
        return Result.ok("个人资料已更新", info);
    }

    /**
     * 忘记密码申请（生成待审批单，由直系主管或资产管理员审批后重置）
     */
    @PostMapping("/forgot-pwd")
    public Result<String> forgotPwd(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            return Result.error("该账号不存在，请核对后重试");
        }
        SysPwdReset reqRecord = new SysPwdReset();
        reqRecord.setUserId(user.getId());
        reqRecord.setUsername(user.getUsername());
        reqRecord.setRealName(user.getRealName());
        reqRecord.setDeptId(user.getDeptId());
        reqRecord.setStatus("PENDING");
        reqRecord.setCreateTime(LocalDateTime.now());
        pwdResetMapper.insert(reqRecord);

        SysLog log = new SysLog();
        log.setUserId(user.getId());
        log.setOperator(user.getRealName());
        log.setModule("账号安全");
        log.setAction("忘记密码申请");
        log.setDetail("账号 " + username + " 提交密码重置申请，等待直系上级审批");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
        return Result.ok("重置申请已发送给您的直系主管（部门主管）及资产管理员审批", null);
    }

    /**
     * 查询待审批的密码重置申请（逐级审批：部门主管只审本部门员工；资产管理员只审部门主管/财务主管）
     */
    @GetMapping("/pwd-resets")
    public Result<List<SysPwdReset>> listPwdResets(HttpServletRequest req) {
        SysUser me = currentUser(req);
        if (me == null || (!"DEPT_MANAGER".equals(me.getRole()) && !"ASSET_ADMIN".equals(me.getRole()))) {
            return Result.ok(List.of());
        }
        List<SysPwdReset> allPending = pwdResetMapper.selectList(
                new LambdaQueryWrapper<SysPwdReset>()
                        .eq(SysPwdReset::getStatus, "PENDING")
                        .orderByDesc(SysPwdReset::getId)
        );
        List<SysPwdReset> filtered = new java.util.ArrayList<>();
        for (SysPwdReset r : allPending) {
            if (canApprovePwdReset(me, r)) {
                filtered.add(r);
            }
        }
        return Result.ok(filtered);
    }

    private boolean canApprovePwdReset(SysUser reviewer, SysPwdReset record) {
        SysUser applicant = userMapper.selectById(record.getUserId());
        if (applicant == null) return false;
        if ("DEPT_MANAGER".equals(reviewer.getRole())) {
            return "EMPLOYEE".equals(applicant.getRole())
                    && reviewer.getDeptId() != null
                    && reviewer.getDeptId().equals(record.getDeptId());
        }
        if ("ASSET_ADMIN".equals(reviewer.getRole())) {
            return "DEPT_MANAGER".equals(applicant.getRole()) || "FINANCE_MANAGER".equals(applicant.getRole());
        }
        return false;
    }

    /**
     * 直系上级同意重置密码
     */
    @PostMapping("/pwd-resets/{id}/approve")
    public Result<String> approvePwdReset(HttpServletRequest req, @PathVariable Long id) {
        SysUser me = currentUser(req);
        SysPwdReset record = pwdResetMapper.selectById(id);
        if (record == null || !"PENDING".equals(record.getStatus())) {
            return Result.error("申请不存在或已处理");
        }
        if (!canApprovePwdReset(me, record)) {
            return Result.error(403, "只能由直系上一级审批该密码重置申请");
        }
        SysUser target = userMapper.selectById(record.getUserId());
        if (target != null) {
            target.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(target);
        }
        record.setStatus("APPROVED");
        record.setReviewerName(me.getRealName());
        pwdResetMapper.updateById(record);

        SysLog log = new SysLog();
        log.setUserId(me.getId());
        log.setOperator(me.getRealName());
        log.setModule("账号安全");
        log.setAction("同意重置密码");
        log.setDetail("批准重置账号 " + record.getUsername() + " 的密码为 123456");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
        return Result.ok("已同意，该账号密码已重置为 123456", null);
    }

    /**
     * 直系上级驳回密码重置
     */
    @PostMapping("/pwd-resets/{id}/reject")
    public Result<String> rejectPwdReset(HttpServletRequest req, @PathVariable Long id) {
        SysUser me = currentUser(req);
        SysPwdReset record = pwdResetMapper.selectById(id);
        if (record == null || !"PENDING".equals(record.getStatus())) {
            return Result.error("申请不存在或已处理");
        }
        if (!canApprovePwdReset(me, record)) {
            return Result.error(403, "只能由直系上一级驳回该密码重置申请");
        }
        record.setStatus("REJECTED");
        record.setReviewerName(me.getRealName());
        pwdResetMapper.updateById(record);
        return Result.ok("已驳回该密码重置申请", null);
    }

    @PutMapping("/password")
    public Result<String> updatePassword(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        SysUser me = currentUser(req);
        String oldPwd = String.valueOf(body.get("oldPassword"));
        String newPwd = String.valueOf(body.get("newPassword"));

        SysUser user = userMapper.selectById(me.getId());
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            return Result.error("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        return Result.ok("密码修改成功，请牢记新密码", null);
    }

    @GetMapping("/depts")
    public Result<List<SysDept>> listDepts() {
        return Result.ok(deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getId)));
    }

    @PostMapping("/depts")
    public Result<String> addDept(HttpServletRequest req, @RequestBody SysDept dept) {
        if (!isAdmin(req)) return Result.error(403, "无权限操作部门");
        deptMapper.insert(dept);
        return Result.ok("新增部门成功", null);
    }

    @PutMapping("/depts")
    public Result<String> updateDept(HttpServletRequest req, @RequestBody SysDept dept) {
        if (!isAdmin(req)) return Result.error(403, "无权限操作部门");
        deptMapper.updateById(dept);
        return Result.ok("修改部门成功", null);
    }

    @DeleteMapping("/depts/{id}")
    public Result<String> deleteDept(HttpServletRequest req, @PathVariable Long id) {
        if (!isAdmin(req)) return Result.error(403, "无权限操作部门");
        deptMapper.deleteById(id);
        return Result.ok("删除部门成功", null);
    }
}
