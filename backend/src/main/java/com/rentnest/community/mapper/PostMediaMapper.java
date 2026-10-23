package com.rentnest.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.community.entity.PostMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMediaMapper extends BaseMapper<PostMedia> {
    List<String> selectUrlsByPostId(@Param("postId") Long postId);

    /** 仅返回页面范围内的帖子图片，避免逐条 N+1 */
    List<Map<String, Object>> selectUrlsByPostIds(@Param("postIds") List<Long> postIds);
}
