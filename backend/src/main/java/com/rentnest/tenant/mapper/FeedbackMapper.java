package com.rentnest.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.tenant.dto.FeedbackQuery;
import com.rentnest.tenant.dto.FeedbackResp;
import com.rentnest.tenant.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
    List<FeedbackResp> selectPage(FeedbackQuery query);

    long countByQuery(FeedbackQuery query);
}
