package com.rentnest.user.service;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.exception.BizException;
import com.rentnest.user.dto.ProfileResp;
import com.rentnest.user.dto.UpdateProfileReq;
import com.rentnest.user.entity.User;
import com.rentnest.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

    public ProfileResp myProfile() {
        User user = mustGet(UserContextHolder.require().getId());
        return toProfileResp(user);
    }

    @Transactional
    public ProfileResp updateMyProfile(UpdateProfileReq req) {
        User user = mustGet(UserContextHolder.require().getId());
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getRealName() != null) {
            user.setRealName(req.getRealName());
        }
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getAvatarFileId() != null) {
            user.setAvatarFileId(req.getAvatarFileId());
        }
        userMapper.updateById(user);
        return toProfileResp(user);
    }

    public ProfileResp adminGetContact(Long userId) {
        User user = mustGet(userId);
        return toProfileResp(user);
    }

    private User mustGet(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private ProfileResp toProfileResp(User user) {
        return new ProfileResp(user.getId(), user.getPhone(), user.getNickname(),
                user.getAvatarFileId(), user.getRole(), user.getRealName(),
                user.getEmail(), user.getCreatedAt());
    }
}
