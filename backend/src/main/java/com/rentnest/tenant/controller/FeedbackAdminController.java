package com.rentnest.tenant.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.FeedbackQuery;
import com.rentnest.tenant.dto.FeedbackReplyReq;
import com.rentnest.tenant.dto.FeedbackResp;
import com.rentnest.tenant.entity.Feedback;
import com.rentnest.tenant.service.FeedbackService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈处理归房东与管理员（需求 6.3.2 权限拆分）。
 */
@RestController
@RequestMapping("/api/admin/feedbacks")
@RequireRole(Roles.LANDLORD)
public class FeedbackAdminController {
    @Resource
    private FeedbackService feedbackService;

    @GetMapping("/page")
    public Result<PageResult<FeedbackResp>> page(FeedbackQuery query) {
        return Result.ok(feedbackService.page(query));
    }

    @PutMapping("/{id}/reply")
    @AuditLog(action = "REPLY_FEEDBACK", targetType = "FEEDBACK", targetId = "#id")
    public Result<Feedback> reply(@PathVariable Long id,
                                  @Valid @RequestBody FeedbackReplyReq req) {
        return Result.ok(feedbackService.reply(id, req.getReply(), UserContextHolder.require()));
    }

    @PutMapping("/{id}/close")
    @AuditLog(action = "CLOSE_FEEDBACK", targetType = "FEEDBACK", targetId = "#id")
    public Result<Feedback> close(@PathVariable Long id) {
        return Result.ok(feedbackService.close(id, UserContextHolder.require()));
    }
}
