package org.lin.campususer.service.impl;

import org.lin.common.exception.BusinessException;
import org.lin.common.entity.Favorite;
import org.lin.campususer.mapper.FavoriteMapper;
import org.lin.campususer.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Override
    public List<Favorite> getFavoritesByUserId(Long userId) {
        return favoriteMapper.findByUserId(userId);
    }

    @Override
    public void addFavorite(Long userId, Long productId) {
        Favorite existing = favoriteMapper.findByUserIdAndProductIdIncludingDeleted(userId, productId);
        if (existing != null) {
            if (existing.getIsDeleted() == 0) {
                throw new BusinessException("已收藏该商品");
            }
            favoriteMapper.restoreById(existing.getFavoriteId(), new Date());
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreateTime(new Date());
        favorite.setIsDeleted(0);
        favoriteMapper.insert(favorite);
    }

    @Override
    public void removeFavorite(Long userId, Long productId) {
        Favorite favorite = favoriteMapper.findByUserIdAndProductId(userId, productId);
        if (favorite == null) {
            throw new BusinessException("未收藏该商品");
        }
        favoriteMapper.deleteById(favorite.getFavoriteId());
    }

    @Override
    public boolean isFavorite(Long userId, Long productId) {
        return favoriteMapper.findByUserIdAndProductId(userId, productId) != null;
    }
}