package com.rentnest.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.rental.dto.HouseListItemResp;
import com.rentnest.rental.dto.HouseQuery;
import com.rentnest.rental.entity.House;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HouseMapper extends BaseMapper<House> {
    List<HouseListItemResp> selectPage(HouseQuery query);

    long countByQuery(HouseQuery query);
}