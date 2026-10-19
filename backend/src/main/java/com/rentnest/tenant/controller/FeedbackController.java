package com.rentnest.tenant.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.FeedbackCreateReq;
import com.rentnest.tenant.dto.FeedbackQuery;
import com.rentnest.tenant.dto.FeedbackResp;
import com.rentnest.tenant.entity.Feedback;
import com.rentnest.tenant.service.FeedbackService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FeedbackController {
    @Resource
    private FeedbackService feedbackService;

    @PostMapping("/feedbacks")
    public Result<Feedback> create(@Valid @RequestBody FeedbackCreateReq req) {
        return Result.ok(feedbackService.create(req, UserContextHolder.require()));
    }

    @GetMapping("/my/feedbacks/page")
    public Result<PageResult<FeedbackResp>> myFeedbacks(FeedbackQuery query) {
        query.setUserId(UserContextHolder.require().getId());
        return Result.ok(feedbackService.page(query));
    }
}
