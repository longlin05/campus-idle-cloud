package org.lin.campususer.service;

import org.lin.common.context.UserHomeVO;
import org.lin.common.context.UserInfo;
import org.lin.common.entity.User;

public interface UserService {
    User getUserById(Long userId);
    UserInfo getUserInfoById(Long userId);
    UserInfo updateUserAvatar(Long userId, String avatar);
    UserInfo updateUserInfo(Long userId, String nickname, String email, String avatar);

    /**
     * 获取用户主页完整信息（含粉丝/关注统计）
     * @param targetUserId 目标用户ID
     * @param currentUserId 当前登录用户ID（未登录为null）
     */
    UserHomeVO getUserHome(Long targetUserId, Long currentUserId);
}