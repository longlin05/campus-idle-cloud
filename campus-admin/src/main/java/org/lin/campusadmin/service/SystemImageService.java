package org.lin.campusadmin.service;

import org.lin.common.context.PageResult;
import org.lin.common.context.SystemImageVO;
import org.lin.common.result.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SystemImageService {
    Result<PageResult<SystemImageVO>> getImageList(Integer type, Long current, Long size);
    Result<SystemImageVO> getImageById(Long imageId);
    Result<?> uploadAndAddImage(MultipartFile file, String imageName, Integer type, String description, Integer sortOrder);
    Result<?> updateImage(Long imageId, String imageName, Integer type, String description, Integer sortOrder);
    Result<?> deleteImage(Long imageId);
    Result<?> updateImageStatus(Long imageId, Integer status);
    Result<List<SystemImageVO>> getBannerList();
    Result<String> getLogoUrl();
    Result<String> getFaviconUrl();
}
