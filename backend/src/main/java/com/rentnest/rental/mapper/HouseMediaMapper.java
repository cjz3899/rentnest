package com.rentnest.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.rental.dto.HouseMediaResp;
import com.rentnest.rental.entity.HouseMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HouseMediaMapper extends BaseMapper<HouseMedia> {
    List<HouseMediaResp> selectByHouseId(@Param("houseId") Long houseId);
}