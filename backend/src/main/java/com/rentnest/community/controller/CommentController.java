package com.rentnest.community.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.community.dto.CommentCreateReq;
import com.rentnest.community.dto.CommentQuery;
import com.rentnest.community.dto.CommentResp;
import com.rentnest.community.entity.PostComment;
import com.rentnest.community.service.CommentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommentController {
    @Resource
    private CommentService commentService;

    @GetMapping("/posts/{postId}/comments/page")
    public Result<PageResult<CommentResp>> page(@PathVariable Long postId, CommentQuery query) {
        return Result.ok(commentService.page(postId, query));
    }

    @PostMapping("/posts/{postId}/comments")
    public Result<PostComment> create(@PathVariable Long postId,
                                      @Valid @RequestBody CommentCreateReq req) {
        return Result.ok(commentService.create(postId, req, UserContextHolder.require()));
    }
}
