package com.rentnest.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.tenant.dto.AnnouncementQuery;
import com.rentnest.tenant.dto.AnnouncementResp;
import com.rentnest.tenant.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
    List<AnnouncementResp> selectPage(AnnouncementQuery query);

    long countByQuery(AnnouncementQuery query);
}
