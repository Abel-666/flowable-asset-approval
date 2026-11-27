package com.asset.controller;

import com.asset.common.Result;
import com.asset.entity.AssetInfo;
import com.asset.entity.AssetInventory;
import com.asset.mapper.AssetInfoMapper;
import com.asset.mapper.AssetInventoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 资产盘点记录接口（支持增删改查与筛选）
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final AssetInventoryMapper inventoryMapper;
    private final AssetInfoMapper assetMapper;

    @GetMapping("/list")
    public Result<List<AssetInventory>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String result) {
        LambdaQueryWrapper<AssetInventory> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(AssetInventory::getTitle, keyword)
                    .or().like(AssetInventory::getInventoryNo, keyword);
        }
        if (result != null && !result.trim().isEmpty()) {
            wrapper.eq(AssetInventory::getResult, result);
        }
        wrapper.orderByDesc(AssetInventory::getId);
        return Result.ok(inventoryMapper.selectList(wrapper));
    }

    @PostMapping("/add")
    public Result<AssetInventory> add(@RequestBody AssetInventory inventory) {
        long expected = assetMapper.selectCount(
                new LambdaQueryWrapper<AssetInfo>().ne(AssetInfo::getStatus, 5)
        );
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = ThreadLocalRandom.current().nextInt(100, 999);
        inventory.setInventoryNo("PD-" + datePart + "-" + rand);
        inventory.setExpectedCount((int) expected);
        inventory.setResult(inventory.getActualCount() == expected ? "NORMAL" : "ABNORMAL");
        inventory.setCreateTime(LocalDateTime.now());
        inventoryMapper.insert(inventory);
        return Result.ok("盘点记录已保存", inventory);
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody AssetInventory inventory) {
        if (inventory.getExpectedCount() != null && inventory.getActualCount() != null) {
            inventory.setResult(inventory.getActualCount().equals(inventory.getExpectedCount()) ? "NORMAL" : "ABNORMAL");
        }
        inventoryMapper.updateById(inventory);
        return Result.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        inventoryMapper.deleteById(id);
        return Result.ok("删除成功", null);
    }
}
