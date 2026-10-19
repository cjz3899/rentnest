package com.rentnest.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.tenant.entity.FeedbackMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FeedbackMediaMapper extends BaseMapper<FeedbackMedia> {
    /** 按反馈批量取图片 URL（feedback_id, url），供分页结果二次装配 */
    List<Map<String, Object>> selectUrlsByFeedbackIds(@Param("feedbackIds") List<Long> feedbackIds);
}
