package com.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asset_apply")
public class AssetApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    /** PURCHASE-采购申购，USE-领用，BORROW-借用，SCRAP-报废 */
    private String applyType;
    private Long applicantId;
    private String applicantName;
    private Long deptId;
    private Long assetId;
    private String assetName;
    private String category;
    private String spec;
    private Integer quantity;
    private BigDecimal amount;
    private String reason;
    /** PENDING-审批中，APPROVED-已通过，REJECTED-已驳回，FINISHED-已办结 */
    private String status;
    private String processInstanceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
