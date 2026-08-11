package org.lin.campususer.service.impl;

import org.lin.common.exception.BusinessException;
import org.lin.common.entity.Follow;
import org.lin.campususer.mapper.FollowMapper;
import org.lin.campususer.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    @Override
    public List<Follow> getFollowsByUserId(Long userId) {
        return followMapper.findByUserId(userId);
    }

    @Override
    public void follow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new BusinessException("不能关注自己");
        }

        Follow existing = followMapper.findByUserIdAndFollowUserId(userId, followUserId);
        if (existing != null) {
            throw new BusinessException("已关注该用户");
        }

        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        follow.setCreateTime(new Date());
        follow.setIsDeleted(0);
        followMapper.insert(follow);
    }

    @Override
    public void unfollow(Long userId, Long followUserId) {
        Follow follow = followMapper.findByUserIdAndFollowUserId(userId, followUserId);
        if (follow == null) {
            throw new BusinessException("未关注该用户");
        }
        followMapper.deleteById(follow.getFollowId());
    }

    @Override
    public boolean isFollowing(Long userId, Long followUserId) {
        return followMapper.findByUserIdAndFollowUserId(userId, followUserId) != null;
    }
}