package com.rentnest.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.tenant.entity.RepairMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RepairMediaMapper extends BaseMapper<RepairMedia> {
    List<String> selectUrlsByTicketId(@Param("ticketId") Long ticketId);
}
