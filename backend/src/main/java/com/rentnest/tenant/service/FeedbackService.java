package com.rentnest.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.file.entity.FileRecord;
import com.rentnest.file.mapper.FileRecordMapper;
import com.rentnest.tenant.dto.FeedbackCreateReq;
import com.rentnest.tenant.dto.FeedbackQuery;
import com.rentnest.tenant.dto.FeedbackResp;
import com.rentnest.tenant.entity.Feedback;
import com.rentnest.tenant.entity.FeedbackMedia;
import com.rentnest.tenant.enums.FeedbackStatus;
import com.rentnest.tenant.mapper.FeedbackMapper;
import com.rentnest.tenant.mapper.FeedbackMediaMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedbackService {
    @Resource
    private FeedbackMapper feedbackMapper;
    @Resource
    private FeedbackMediaMapper feedbackMediaMapper;
    @Resource
    private FileRecordMapper fileRecordMapper;

    @Transactional
    public Feedback create(FeedbackCreateReq req, CurrentUser current) {
        if (req.getFileIds() != null && !req.getFileIds().isEmpty()) {
            Long owned = fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecord>()
                    .in(FileRecord::getId, req.getFileIds())
                    .eq(FileRecord::getOwnerId, current.getId()));
            if (owned != req.getFileIds().size()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "存在无效或不属于自己的图片文件");
            }
        }

        Feedback feedback = new Feedback();
        feedback.setUserId(current.getId());
        feedback.setContent(req.getContent());
        feedback.setProblemDesc(req.getProblemDesc());
        feedback.setContactPhone(req.getContactPhone());
        feedback.setStatus(FeedbackStatus.PENDING.name());
        feedbackMapper.insert(feedback);

        if (req.getFileIds() != null) {
            int order = 0;
            for (Long fileId : req.getFileIds().stream().distinct().toList()) {
                FeedbackMedia media = new FeedbackMedia();
                media.setFeedbackId(feedback.getId());
                media.setFileId(fileId);
                media.setSortOrder(order++);
                feedbackMediaMapper.insert(media);
            }
        }
        return feedback;
    }

    /** 分页结果二次装配图片 URL（一次 IN 查询，避免 N+1） */
    public PageResult<FeedbackResp> page(FeedbackQuery query) {
        long total = feedbackMapper.countByQuery(query);
        List<FeedbackResp> records = total == 0 ? List.of() : feedbackMapper.selectPage(query);
        if (!records.isEmpty()) {
            List<Long> ids = records.stream().map(FeedbackResp::getId).toList();
            Map<Long, List<String>> mediaMap = new HashMap<>();
            for (Map<String, Object> row : feedbackMediaMapper.selectUrlsByFeedbackIds(ids)) {
                Long feedbackId = ((Number) row.get("feedbackId")).longValue();
                mediaMap.computeIfAbsent(feedbackId, k -> new ArrayList<>())
                        .add((String) row.get("url"));
            }
            records.forEach(r -> r.setMediaUrls(
                    mediaMap.getOrDefault(r.getId(), List.of())));
        }
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    /** 管理端回复；未关闭的反馈可多次回复更新意见 */
    @Transactional
    public Feedback reply(Long id, String reply, CurrentUser current) {
        Feedback feedback = mustGet(id);
        FeedbackStatus status = FeedbackStatus.valueOf(feedback.getStatus());
        if (!status.canReply()) {
            throw new BizException(ErrorCode.CONFLICT, "反馈已关闭，无法回复");
        }
        feedback.setReply(reply);
        feedback.setReplierId(current.getId());
        feedback.setRepliedAt(LocalDateTime.now());
        feedback.setStatus(FeedbackStatus.REPLIED.name());
        feedbackMapper.updateById(feedback);
        return feedback;
    }

    @Transactional
    public Feedback close(Long id, CurrentUser current) {
        Feedback feedback = mustGet(id);
        FeedbackStatus status = FeedbackStatus.valueOf(feedback.getStatus());
        if (!status.canClose()) {
            throw new BizException(ErrorCode.CONFLICT, "反馈已是关闭状态");
        }
        feedback.setStatus(FeedbackStatus.CLOSED.name());
        feedbackMapper.updateById(feedback);
        return feedback;
    }

    public Feedback mustGet(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "反馈不存在");
        }
        return feedback;
    }
}
