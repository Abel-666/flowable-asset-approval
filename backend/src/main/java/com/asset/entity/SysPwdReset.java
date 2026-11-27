package com.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_pwd_reset")
public class SysPwdReset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long deptId;
    private String status;
    private String reviewerName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
