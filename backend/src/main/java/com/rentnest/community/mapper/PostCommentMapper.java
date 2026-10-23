package com.rentnest.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.community.dto.CommentQuery;
import com.rentnest.community.dto.CommentResp;
import com.rentnest.community.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
    List<CommentResp> selectTopPage(CommentQuery query);

    long countTopByQuery(CommentQuery query);

    /** 批量取一级评论的回复 */
    List<CommentResp> selectRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    List<CommentResp> selectAdminPage(CommentQuery query);

    long countAdminByQuery(CommentQuery query);
}
