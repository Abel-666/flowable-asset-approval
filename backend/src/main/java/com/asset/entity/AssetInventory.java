package com.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("asset_inventory")
public class AssetInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String inventoryNo;
    private String title;
    private Long checkerId;
    private String checkerName;
    private Integer expectedCount;
    private Integer actualCount;
    private String result;
    private String remark;
    private LocalDateTime createTime;
}
