package org.lin.campusadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.PageResult;
import org.lin.common.context.SystemImageVO;
import org.lin.common.oss.OssUtil;
import org.lin.common.result.Result;
import org.lin.common.entity.Image;
import org.lin.campusadmin.mapper.ImageMapper;
import org.lin.campusadmin.service.SystemImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统图片服务实现
 * 复用 idle_image 表（type=3）管理系统图片，图片上传使用阿里云OSS
 */
@Slf4j
@Service
public class SystemImageServiceImpl implements SystemImageService {

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private OssUtil ossUtil;

    @Override
    public Result<PageResult<SystemImageVO>> getImageList(Integer type, Long current, Long size) {
        Page<Image> page = new Page<>(current, size);
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Image::getType, Image.TYPE_SYSTEM);
        if (type != null) {
            wrapper.eq(Image::getRelationId, type.longValue());
        }
        wrapper.orderByDesc(Image::getSortOrder).orderByDesc(Image::getCreateTime);
        Page<Image> resultPage = imageMapper.selectPage(page, wrapper);

        List<SystemImageVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        PageResult<SystemImageVO> pageResult = new PageResult<>();
        pageResult.setCurrent(resultPage.getCurrent());
        pageResult.setSize(resultPage.getSize());
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setPages(resultPage.getPages());
        pageResult.setRecords(voList);
        return Result.success(pageResult);
    }

    @Override
    public Result<SystemImageVO> getImageById(Long imageId) {
        Image image = imageMapper.selectById(imageId);
        if (image == null || !Image.TYPE_SYSTEM.equals(image.getType())) {
            return Result.error(404, "图片不存在");
        }
        return Result.success(convertToVO(image));
    }

    @Override
    public Result<?> uploadAndAddImage(MultipartFile file, String imageName, Integer type,
                                       String description, Integer sortOrder) {
        // 上传到OSS
        String imageUrl;
        try {
            imageUrl = ossUtil.upload(file, "system");
        } catch (Exception e) {
            log.error("系统图片上传到OSS失败: {}", e.getMessage(), e);
            return Result.error(500, "图片上传失败：" + e.getMessage()
                    + "（请检查 campus-admin 的 aliyun.oss 配置是否有效、网络是否能访问 OSS）");
        }

        Image image = new Image();
        image.setType(Image.TYPE_SYSTEM);
        image.setRelationId(type != null ? type.longValue() : 0L);
        image.setImageUrl(imageUrl);
        image.setSortOrder(sortOrder != null ? sortOrder : 0);
        image.setCreateTime(new Date());
        imageMapper.insert(image);
        return Result.success("图片上传成功");
    }

    @Override
    public Result<?> updateImage(Long imageId, String imageName, Integer type,
                                 String description, Integer sortOrder) {
        Image image = imageMapper.selectById(imageId);
        if (image == null || !Image.TYPE_SYSTEM.equals(image.getType())) {
            return Result.error(404, "图片不存在");
        }
        if (type != null) {
            image.setRelationId(type.longValue());
        }
        if (sortOrder != null) {
            image.setSortOrder(sortOrder);
        }
        imageMapper.updateById(image);
        return Result.success("图片更新成功");
    }

    @Override
    public Result<?> deleteImage(Long imageId) {
        Image image = imageMapper.selectById(imageId);
        if (image == null || !Image.TYPE_SYSTEM.equals(image.getType())) {
            return Result.error(404, "图片不存在");
        }
        // 删除OSS上的文件
        try {
            ossUtil.delete(image.getImageUrl());
        } catch (Exception e) {
            log.warn("OSS文件删除失败: {}", e.getMessage());
        }
        imageMapper.deleteById(imageId);
        return Result.success("图片删除成功");
    }

    @Override
    public Result<?> updateImageStatus(Long imageId, Integer status) {
        Image image = imageMapper.selectById(imageId);
        if (image == null || !Image.TYPE_SYSTEM.equals(image.getType())) {
            return Result.error(404, "图片不存在");
        }
        // sortOrder<0 表示禁用，通过正负号切换模拟启用/禁用
        if (status == 0 && image.getSortOrder() >= 0) {
            image.setSortOrder(-image.getSortOrder() - 1);
        } else if (status == 1 && image.getSortOrder() < 0) {
            image.setSortOrder(-image.getSortOrder() - 1);
        }
        imageMapper.updateById(image);
        return Result.success(status == 1 ? "图片启用成功" : "图片禁用成功");
    }

    @Override
    public Result<List<SystemImageVO>> getBannerList() {
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Image::getType, Image.TYPE_SYSTEM)
                .eq(Image::getRelationId, 0L)
                .ge(Image::getSortOrder, 0)
                .orderByDesc(Image::getSortOrder)
                .orderByDesc(Image::getCreateTime);
        List<Image> images = imageMapper.selectList(wrapper);
        List<SystemImageVO> voList = images.stream().map(this::convertToVO).collect(Collectors.toList());
        return Result.success(voList);
    }

    @Override
    public Result<String> getLogoUrl() {
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Image::getType, Image.TYPE_SYSTEM)
                .eq(Image::getRelationId, 3L)
                .ge(Image::getSortOrder, 0)
                .orderByDesc(Image::getSortOrder)
                .orderByDesc(Image::getCreateTime)
                .last("LIMIT 1");
        Image image = imageMapper.selectOne(wrapper);
        return Result.success(image != null ? image.getImageUrl() : null);
    }

    @Override
    public Result<String> getFaviconUrl() {
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Image::getType, Image.TYPE_SYSTEM)
                .eq(Image::getRelationId, 4L)
                .ge(Image::getSortOrder, 0)
                .orderByDesc(Image::getSortOrder)
                .orderByDesc(Image::getCreateTime)
                .last("LIMIT 1");
        Image image = imageMapper.selectOne(wrapper);
        return Result.success(image != null ? image.getImageUrl() : null);
    }

    private SystemImageVO convertToVO(Image image) {
        SystemImageVO vo = new SystemImageVO();
        vo.setImageId(image.getImageId());
        String url = image.getImageUrl();
        String name = url;
        if (url != null && url.contains("/")) {
            name = url.substring(url.lastIndexOf("/") + 1);
        }
        vo.setImageName(name);
        vo.setImageUrl(url);
        vo.setDescription("系统图片");
        vo.setType(image.getRelationId() != null ? image.getRelationId().intValue() : 0);
        vo.setStatus(image.getSortOrder() != null && image.getSortOrder() >= 0 ? 1 : 0);
        vo.setSortOrder(image.getSortOrder() != null ? Math.abs(image.getSortOrder()) : 0);
        vo.setCreateTime(image.getCreateTime());
        return vo;
    }
}
