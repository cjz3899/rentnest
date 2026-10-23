package com.rentnest.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.community.dto.PostQuery;
import com.rentnest.community.dto.PostResp;
import com.rentnest.community.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    List<PostResp> selectPage(PostQuery query);

    long countByQuery(PostQuery query);

    PostResp selectDetailById(@Param("id") Long id, @Param("viewerId") Long viewerId);

    /** 冗余计数用原子 SQL 维护，避免并发覆盖 */
    int increaseLikeCount(@Param("id") Long id);

    int decreaseLikeCount(@Param("id") Long id);

    int increaseCommentCount(@Param("id") Long id);

    int decreaseCommentCount(@Param("id") Long id);
}
