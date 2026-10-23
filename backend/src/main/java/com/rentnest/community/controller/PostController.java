package com.rentnest.community.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.community.dto.LikeResp;
import com.rentnest.community.dto.PostCreateReq;
import com.rentnest.community.dto.PostQuery;
import com.rentnest.community.dto.PostResp;
import com.rentnest.community.service.PostService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PostController {
    @Resource
    private PostService postService;

    @PostMapping("/posts")
    public Result<PostResp> create(@Valid @RequestBody PostCreateReq req) {
        return Result.ok(postService.create(req, UserContextHolder.require()));
    }

    @GetMapping("/posts/page")
    public Result<PageResult<PostResp>> page(PostQuery query) {
        return Result.ok(postService.page(query, UserContextHolder.require()));
    }

    @GetMapping("/posts/{id}")
    public Result<PostResp> detail(@PathVariable Long id) {
        return Result.ok(postService.detail(id, UserContextHolder.require()));
    }

    @GetMapping("/my/posts/page")
    public Result<PageResult<PostResp>> myPage(PostQuery query) {
        return Result.ok(postService.myPage(query, UserContextHolder.require()));
    }

    @PostMapping("/posts/{id}/like")
    public Result<LikeResp> like(@PathVariable Long id) {
        return Result.ok(postService.like(id, UserContextHolder.require()));
    }

    @DeleteMapping("/posts/{id}/like")
    public Result<LikeResp> unlike(@PathVariable Long id) {
        return Result.ok(postService.unlike(id, UserContextHolder.require()));
    }
}
