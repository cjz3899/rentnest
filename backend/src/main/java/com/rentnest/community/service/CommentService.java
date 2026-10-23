package com.rentnest.community.service;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.community.dto.CommentCreateReq;
import com.rentnest.community.dto.CommentQuery;
import com.rentnest.community.dto.CommentResp;
import com.rentnest.community.entity.PostComment;
import com.rentnest.community.enums.ContentStatus;
import com.rentnest.community.mapper.PostCommentMapper;
import com.rentnest.community.mapper.PostMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {
    @Resource
    private PostCommentMapper postCommentMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private PostService postService;

    @Transactional
    public PostComment create(Long postId, CommentCreateReq req, CurrentUser current) {
        postService.mustGetNormal(postId);

        if (req.getParentId() != null) {
            PostComment parent = postCommentMapper.selectById(req.getParentId());
            if (parent == null || !ContentStatus.NORMAL.name().equals(parent.getStatus())) {
                throw new BizException(ErrorCode.NOT_FOUND, "回复的评论不存在或已下架");
            }
            if (!parent.getPostId().equals(postId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "评论与帖子不匹配");
            }
            if (parent.getParentId() != null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "仅支持回复一级评论");
            }
        }

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(current.getId());
        comment.setParentId(req.getParentId());
        comment.setContent(req.getContent());
        comment.setStatus(ContentStatus.NORMAL.name());
        postCommentMapper.insert(comment);
        postMapper.increaseCommentCount(postId);
        return comment;
    }

    /** 一级评论分页，回复随一级评论一起返回 */
    public PageResult<CommentResp> page(Long postId, CommentQuery query) {
        postService.mustGetNormal(postId);
        query.setPostId(postId);

        long total = postCommentMapper.countTopByQuery(query);
        List<CommentResp> records = total == 0
                ? List.of() : postCommentMapper.selectTopPage(query);
        if (!records.isEmpty()) {
            List<Long> parentIds = records.stream().map(CommentResp::getId).toList();
            Map<Long, List<CommentResp>> replyMap = new HashMap<>();
            for (CommentResp reply : postCommentMapper.selectRepliesByParentIds(parentIds)) {
                replyMap.computeIfAbsent(reply.getParentId(), k -> new ArrayList<>()).add(reply);
            }
            records.forEach(c -> c.setReplies(
                    replyMap.getOrDefault(c.getId(), List.of())));
        }
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    public PageResult<CommentResp> adminPage(CommentQuery query) {
        long total = postCommentMapper.countAdminByQuery(query);
        List<CommentResp> records = total == 0
                ? List.of() : postCommentMapper.selectAdminPage(query);
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    /** 管理端下架/恢复评论，并同步帖子评论数 */
    @Transactional
    public PostComment changeStatus(Long id, String status) {
        ContentStatus target = ContentStatus.parse(status);
        PostComment comment = mustGet(id);
        if (comment.getStatus().equals(target.name())) {
            return comment;
        }
        comment.setStatus(target.name());
        postCommentMapper.updateById(comment);
        if (target == ContentStatus.REMOVED) {
            postMapper.decreaseCommentCount(comment.getPostId());
        } else {
            postMapper.increaseCommentCount(comment.getPostId());
        }
        return comment;
    }

    private PostComment mustGet(Long id) {
        PostComment comment = postCommentMapper.selectById(id);
        if (comment == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        return comment;
    }
}
