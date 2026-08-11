package org.lin.campususer.service;

import org.lin.common.entity.Follow;

import java.util.List;

public interface FollowService {
    List<Follow> getFollowsByUserId(Long userId);
    void follow(Long userId, Long followUserId);
    void unfollow(Long userId, Long followUserId);
    boolean isFollowing(Long userId, Long followUserId);
}