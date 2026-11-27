package com.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asset_info")
public class AssetInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetCode;
    private String assetName;
    private String category;
    private String spec;
    private BigDecimal price;
    /** 1-在库闲置，2-在用，3-借出，4-维修中，5-已报废 */
    private Integer status;
    private Long userId;
    private Long deptId;
    private LocalDateTime inboundTime;
    private String remark;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String deptName;
}
