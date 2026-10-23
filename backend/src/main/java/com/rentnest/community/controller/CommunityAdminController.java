package com.rentnest.community.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.community.dto.CommentQuery;
import com.rentnest.community.dto.CommentResp;
import com.rentnest.community.dto.ContentStatusReq;
import com.rentnest.community.dto.PostQuery;
import com.rentnest.community.dto.PostResp;
import com.rentnest.community.entity.Post;
import com.rentnest.community.entity.PostComment;
import com.rentnest.community.service.CommentService;
import com.rentnest.community.service.PostService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社区内容管理归 ADMIN（需求 6.3.2 权限拆分）。
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole(Roles.ADMIN)
public class CommunityAdminController {
    @Resource
    private PostService postService;
    @Resource
    private CommentService commentService;

    @GetMapping("/posts/page")
    public Result<PageResult<PostResp>> posts(PostQuery query) {
        return Result.ok(postService.adminPage(query, UserContextHolder.require()));
    }

    @GetMapping("/posts/{id}")
    public Result<PostResp> postDetail(@PathVariable Long id) {
        return Result.ok(postService.adminDetail(id, UserContextHolder.require()));
    }

    @PutMapping("/posts/{id}/status")
    @AuditLog(action = "CHANGE_POST_STATUS", targetType = "POST", targetId = "#id")
    public Result<Post> changePostStatus(@PathVariable Long id,
                                         @Valid @RequestBody ContentStatusReq req) {
        return Result.ok(postService.changeStatus(id, req.getStatus()));
    }

    @GetMapping("/comments/page")
    public Result<PageResult<CommentResp>> comments(CommentQuery query) {
        return Result.ok(commentService.adminPage(query));
    }

    @PutMapping("/comments/{id}/status")
    @AuditLog(action = "CHANGE_COMMENT_STATUS", targetType = "COMMENT", targetId = "#id")
    public Result<PostComment> changeCommentStatus(@PathVariable Long id,
                                                   @Valid @RequestBody ContentStatusReq req) {
        return Result.ok(commentService.changeStatus(id, req.getStatus()));
    }
}
