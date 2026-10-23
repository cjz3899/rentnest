package com.rentnest.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.community.dto.LikeResp;
import com.rentnest.community.dto.PostCreateReq;
import com.rentnest.community.dto.PostQuery;
import com.rentnest.community.dto.PostResp;
import com.rentnest.community.entity.Post;
import com.rentnest.community.entity.PostLike;
import com.rentnest.community.entity.PostMedia;
import com.rentnest.community.enums.ContentStatus;
import com.rentnest.community.mapper.PostLikeMapper;
import com.rentnest.community.mapper.PostMapper;
import com.rentnest.community.mapper.PostMediaMapper;
import com.rentnest.file.entity.FileRecord;
import com.rentnest.file.mapper.FileRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostService {
    @Resource
    private PostMapper postMapper;
    @Resource
    private PostMediaMapper postMediaMapper;
    @Resource
    private PostLikeMapper postLikeMapper;
    @Resource
    private FileRecordMapper fileRecordMapper;

    @Transactional
    public PostResp create(PostCreateReq req, CurrentUser current) {
        validateOwnedFiles(req.getFileIds(), current);

        Post post = new Post();
        post.setUserId(current.getId());
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setStatus(ContentStatus.NORMAL.name());
        postMapper.insert(post);

        saveMedia(post.getId(), req.getFileIds());
        return detail(post.getId(), current);
    }

    /** 用户端列表只展示正常帖子 */
    public PageResult<PostResp> page(PostQuery query, CurrentUser current) {
        query.setStatus(ContentStatus.NORMAL.name());
        query.setViewerId(current.getId());
        return doPage(query);
    }

    /** 我的帖子：包含被下架内容，便于作者知晓处理结果 */
    public PageResult<PostResp> myPage(PostQuery query, CurrentUser current) {
        query.setUserId(current.getId());
        query.setViewerId(current.getId());
        return doPage(query);
    }

    public PageResult<PostResp> adminPage(PostQuery query, CurrentUser current) {
        query.setViewerId(current.getId());
        return doPage(query);
    }

    public PostResp detail(Long id, CurrentUser current) {
        PostResp resp = postMapper.selectDetailById(id, current.getId());
        if (resp == null || !ContentStatus.NORMAL.name().equals(resp.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在或已下架");
        }
        resp.setMediaUrls(postMediaMapper.selectUrlsByPostId(id));
        return resp;
    }

    /** 管理端可查看已下架帖子全文 */
    public PostResp adminDetail(Long id, CurrentUser current) {
        PostResp resp = postMapper.selectDetailById(id, current.getId());
        if (resp == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        resp.setMediaUrls(postMediaMapper.selectUrlsByPostId(id));
        return resp;
    }

    @Transactional
    public LikeResp like(Long postId, CurrentUser current) {
        mustGetNormal(postId);
        Long exists = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, current.getId()));
        if (exists == 0) {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(current.getId());
            try {
                postLikeMapper.insert(like);
                postMapper.increaseLikeCount(postId);
            } catch (DuplicateKeyException ignored) {
                // 并发重复点赞由唯一索引兜底，计数不再增加
            }
        }
        return new LikeResp(true, postMapper.selectById(postId).getLikeCount());
    }

    @Transactional
    public LikeResp unlike(Long postId, CurrentUser current) {
        mustGetNormal(postId);
        int deleted = postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, current.getId()));
        if (deleted > 0) {
            postMapper.decreaseLikeCount(postId);
        }
        return new LikeResp(false, postMapper.selectById(postId).getLikeCount());
    }

    /** 管理端下架/恢复帖子 */
    @Transactional
    public Post changeStatus(Long id, String status) {
        ContentStatus target = ContentStatus.parse(status);
        Post post = mustGet(id);
        if (!post.getStatus().equals(target.name())) {
            post.setStatus(target.name());
            postMapper.updateById(post);
        }
        return post;
    }

    public Post mustGetNormal(Long id) {
        Post post = mustGet(id);
        if (!ContentStatus.NORMAL.name().equals(post.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在或已下架");
        }
        return post;
    }

    private Post mustGet(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        return post;
    }

    private PageResult<PostResp> doPage(PostQuery query) {
        long total = postMapper.countByQuery(query);
        List<PostResp> records = total == 0 ? List.of() : postMapper.selectPage(query);
        if (!records.isEmpty()) {
            List<Long> postIds = records.stream().map(PostResp::getId).toList();
            Map<Long, List<String>> mediaMap = new HashMap<>();
            for (Map<String, Object> row : postMediaMapper.selectUrlsByPostIds(postIds)) {
                Long postId = ((Number) row.get("postId")).longValue();
                mediaMap.computeIfAbsent(postId, k -> new ArrayList<>())
                        .add((String) row.get("url"));
            }
            records.forEach(r -> r.setMediaUrls(
                    mediaMap.getOrDefault(r.getId(), List.of())));
        }
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    /** 只能引用自己上传的文件，防止把他人文件挂进自己的帖子 */
    private void validateOwnedFiles(List<Long> fileIds, CurrentUser current) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Long owned = fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecord>()
                .in(FileRecord::getId, fileIds)
                .eq(FileRecord::getOwnerId, current.getId()));
        if (owned != fileIds.size()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "存在无效或不属于自己的图片文件");
        }
    }

    private void saveMedia(Long postId, List<Long> fileIds) {
        if (fileIds == null) {
            return;
        }
        int order = 0;
        for (Long fileId : fileIds.stream().distinct().toList()) {
            PostMedia media = new PostMedia();
            media.setPostId(postId);
            media.setFileId(fileId);
            media.setSortOrder(order++);
            postMediaMapper.insert(media);
        }
    }
}
