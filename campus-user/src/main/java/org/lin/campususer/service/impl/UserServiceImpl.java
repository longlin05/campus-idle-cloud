package org.lin.campususer.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.UserHomeVO;
import org.lin.common.context.UserInfo;
import org.lin.common.exception.BusinessException;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.common.entity.Follow;
import org.lin.common.entity.User;
import org.lin.campususer.mapper.FollowMapper;
import org.lin.campususer.mapper.UserMapper;
import org.lin.campususer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${campus.auth.base-url:http://localhost:8081}")
    private String authBaseUrl;

    @Override
    public User getUserById(Long userId) {
        return userMapper.findByUserId(userId);
    }

    @Override
    public UserInfo getUserInfoById(Long userId) {
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            user = syncUserFromJwt(userId);
        }
        if (user == null) {
            // 最后兜底：调用 campus-auth 的内部接口获取用户信息
            user = syncUserFromAuth(userId);
        }
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToUserInfo(user);
    }

    @Override
    public UserInfo updateUserAvatar(Long userId, String avatar) {
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            user = syncUserFromJwt(userId);
        }
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setAvatar(avatar);
        userMapper.updateById(user);
        syncToAuth(userId, user);
        return convertToUserInfo(user);
    }

    @Override
    public UserInfo updateUserInfo(Long userId, String nickname, String email, String avatar) {
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            user = syncUserFromJwt(userId);
        }
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        userMapper.updateById(user);
        syncToAuth(userId, user);
        return convertToUserInfo(user);
    }

    private User syncUserFromJwt(Long userId) {
        try {
            UserInfo jwtUser = UserThreadLocal.get();
            if (jwtUser != null && jwtUser.getId() != null) {
                User user = new User();
                user.setUserId(jwtUser.getId());
                user.setPassword("sync_placeholder_" + jwtUser.getId());
                user.setNickname(jwtUser.getNickname());
                user.setPhone(jwtUser.getPhone());
                user.setEmail(jwtUser.getEmail());
                user.setAvatar(jwtUser.getAvatar());
                user.setRole(jwtUser.getRole() != null ? jwtUser.getRole() : 1);
                user.setStatus(jwtUser.getStatus() != null ? jwtUser.getStatus() : 1);
                user.setIsDeleted(0);
                userMapper.insert(user);
                log.info("从JWT同步用户到campus_user数据库: userId={}", userId);
                return user;
            }
        } catch (Exception e) {
            log.warn("同步用户失败: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * 从 campus-auth 服务获取用户信息并同步到本地（用于跨服务查询的兜底）
     */
    private User syncUserFromAuth(Long userId) {
        try {
            String url = authBaseUrl + "/auth/user/" + userId;
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && Integer.valueOf(200).equals(resp.get("code"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                if (data != null) {
                    User user = new User();
                    user.setUserId(((Number) data.get("userId")).longValue());
                    user.setNickname((String) data.getOrDefault("nickname", ""));
                    user.setAvatar((String) data.getOrDefault("avatar", ""));
                    user.setPhone((String) data.getOrDefault("phone", ""));
                    user.setEmail((String) data.getOrDefault("email", ""));
                    user.setRole(data.get("role") != null ? ((Number) data.get("role")).intValue() : 1);
                    user.setStatus(data.get("status") != null ? ((Number) data.get("status")).intValue() : 1);
                    user.setIsDeleted(0);
                    userMapper.insert(user);
                    log.info("从campus-auth同步用户到campus_user数据库: userId={}", userId);
                    return user;
                }
            }
        } catch (Exception e) {
            log.warn("从campus-auth同步用户失败: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    private UserInfo convertToUserInfo(User user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getUserId());
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRole(user.getRole());
        userInfo.setStatus(user.getStatus());
        userInfo.setCreateTime(user.getCreateTime());
        userInfo.setLastLoginTime(user.getLastLoginTime());
        return userInfo;
    }

    @Override
    public UserHomeVO getUserHome(Long targetUserId, Long currentUserId) {
        // 复用 getUserInfoById 的完整同步链路（本地 → JWT → campus-auth）
        UserInfo info = getUserInfoById(targetUserId);

        UserHomeVO vo = new UserHomeVO();
        vo.setId(info.getId());
        vo.setNickname(info.getNickname());
        vo.setPhone(maskPhone(info.getPhone()));
        vo.setEmail(info.getEmail());
        vo.setAvatar(info.getAvatar());
        vo.setStatus(info.getStatus());
        vo.setCreateTime(info.getCreateTime());
        vo.setLastLoginTime(info.getLastLoginTime());

        // 粉丝/关注统计
        try {
            vo.setFollowCount(followMapper.countFollowing(targetUserId));
            vo.setFansCount(followMapper.countFollowers(targetUserId));
        } catch (Exception e) {
            log.warn("查询用户关注/粉丝统计失败 userId={}, error={}", targetUserId, e.getMessage());
            vo.setFollowCount(0);
            vo.setFansCount(0);
        }

        // 当前登录用户是否关注了该用户
        if (currentUserId != null) {
            try {
                Follow f = followMapper.findByUserIdAndFollowUserId(currentUserId, targetUserId);
                vo.setFollowedByMe(f != null);
            } catch (Exception e) {
                vo.setFollowedByMe(false);
            }
        } else {
            vo.setFollowedByMe(false);
        }

        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 将 campus_user 库的最新资料同步回 campus_auth 库，
     * 保证重新登录时 JWT 携带的是最新头像/昵称。
     * 异步失败不影响主流程（仅记录日志），避免因 auth 服务抖动阻塞用户更新。
     */
    private void syncToAuth(Long userId, User user) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("avatar", user.getAvatar());
            payload.put("nickname", user.getNickname());
            payload.put("email", user.getEmail());

            String url = authBaseUrl + "/auth/internal/sync-profile";
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(payload), String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("同步用户资料到 campus-auth 成功: userId={}", userId);
            } else {
                log.warn("同步用户资料到 campus-auth 返回非2xx: userId={}, status={}", userId, resp.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("同步用户资料到 campus-auth 失败(不影响主流程): userId={}, error={}", userId, e.getMessage());
        }
    }
}