package org.lin.campususer.service;

import org.lin.common.entity.Favorite;

import java.util.List;

public interface FavoriteService {
    List<Favorite> getFavoritesByUserId(Long userId);
    void addFavorite(Long userId, Long productId);
    void removeFavorite(Long userId, Long productId);
    boolean isFavorite(Long userId, Long productId);
}