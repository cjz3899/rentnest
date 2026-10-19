package com.rentnest.tenant.service;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.AnnouncementQuery;
import com.rentnest.tenant.dto.AnnouncementResp;
import com.rentnest.tenant.dto.AnnouncementSaveReq;
import com.rentnest.tenant.entity.Announcement;
import com.rentnest.tenant.enums.AnnouncementStatus;
import com.rentnest.tenant.enums.AnnouncementType;
import com.rentnest.tenant.mapper.AnnouncementMapper;
import com.rentnest.tenant.redis.AnnouncementCache;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementService {
    @Resource
    private AnnouncementMapper announcementMapper;
    @Resource
    private AnnouncementCache announcementCache;

    /**
     * 用户端/游客：仅已发布，走缓存
     */
    public PageResult<AnnouncementResp> publicPage(AnnouncementQuery query) {
        query.setIncludeUnpublished(false);
        query.setStatus(null);
        PageResult<AnnouncementResp> cached = announcementCache.get(query);
        if (cached != null) {
            return cached;
        }
        PageResult<AnnouncementResp> result = doPage(query);
        announcementCache.put(query, result);
        return result;
    }

    /**
     * 用户端/游客详情：未发布的一律 404
     */
    public AnnouncementResp publicDetail(Long id) {
        Announcement announcement = mustGet(id);
        if (!AnnouncementStatus.PUBLISHED.name().equals(announcement.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "公告不存在或未发布");
        }
        return toResp(announcement);
    }

    public PageResult<AnnouncementResp> adminPage(AnnouncementQuery query) {
        query.setIncludeUnpublished(true);
        return doPage(query);
    }

    @Transactional
    public Announcement create(AnnouncementSaveReq req, CurrentUser current) {
        AnnouncementType.parse(req.getType());
        AnnouncementStatus.parse(req.getStatus());

        Announcement announcement = new Announcement();
        BeanUtils.copyProperties(req, announcement);
        announcement.setPublisherId(current.getId());
        if (AnnouncementStatus.PUBLISHED.name().equals(req.getStatus())) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
        announcementMapper.insert(announcement);
        announcementCache.evictAll();
        return announcement;
    }

    @Transactional
    public Announcement update(Long id, AnnouncementSaveReq req) {
        AnnouncementType.parse(req.getType());
        AnnouncementStatus.parse(req.getStatus());

        Announcement announcement = mustGet(id);
        String oldStatus = announcement.getStatus();
        BeanUtils.copyProperties(req, announcement, "id", "publisherId", "publishedAt");
        // 未发布 → 发布时补发布时间；已发布→未发布→再发布则刷新
        if (AnnouncementStatus.PUBLISHED.name().equals(req.getStatus())
                && !AnnouncementStatus.PUBLISHED.name().equals(oldStatus)) {
            announcement.setPublishedAt(LocalDateTime.now());
        }
        announcementMapper.updateById(announcement);
        announcementCache.evictAll();
        return announcement;
    }

    @Transactional
    public Announcement changeStatus(Long id, String status) {
        AnnouncementStatus target = AnnouncementStatus.parse(status);
        Announcement announcement = mustGet(id);
        if (!announcement.getStatus().equals(target.name())) {
            announcement.setStatus(target.name());
            if (target == AnnouncementStatus.PUBLISHED && announcement.getPublishedAt() == null) {
                announcement.setPublishedAt(LocalDateTime.now());
            }
            announcementMapper.updateById(announcement);
            announcementCache.evictAll();
        }
        return announcement;
    }

    /**
     * 供 AI FAQ 工具与首页聚合复用
     */
    public List<AnnouncementResp> latestPublished(String type, int limit) {
        AnnouncementQuery query = new AnnouncementQuery();
        query.setType(type);
        query.setSize(limit);
        return publicPage(query).getRecords();
    }

    public Announcement mustGet(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return announcement;
    }

    private PageResult<AnnouncementResp> doPage(AnnouncementQuery query) {
        long total = announcementMapper.countByQuery(query);
        List<AnnouncementResp> records = total == 0
                ? List.of() : announcementMapper.selectPage(query);
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    private AnnouncementResp toResp(Announcement announcement) {
        AnnouncementResp resp = new AnnouncementResp();
        BeanUtils.copyProperties(announcement, resp);
        return resp;
    }
}
