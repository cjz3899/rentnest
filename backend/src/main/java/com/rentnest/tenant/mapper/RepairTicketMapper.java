package com.rentnest.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.tenant.dto.RepairQuery;
import com.rentnest.tenant.dto.RepairResp;
import com.rentnest.tenant.entity.RepairTicket;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepairTicketMapper extends BaseMapper<RepairTicket> {
    List<RepairResp> selectPage(RepairQuery query);

    long countByQuery(RepairQuery query);
}
