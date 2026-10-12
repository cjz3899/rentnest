package com.rentnest.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("house_facility")
public class HouseFacility {
    @TableId(value = "house_id", type = IdType.INPUT)
    private Long houseId;
    private String facility;
}