package org.lin.campusadmin.controller;

import org.lin.common.context.PageResult;
import org.lin.common.context.PlatformConfigVO;
import org.lin.common.context.SystemImageVO;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.campusadmin.service.SystemConfigService;
import org.lin.campusadmin.service.SystemImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 系统图片控制器
 * 管理轮播图、广告图、平台Logo/Favicon等系统图片
 * 图片上传使用阿里云OSS，数据存储在 idle_image 表（type=3）
 */
@RestController
@RequestMapping("/admin/system-images")
public class SystemImageController {

    @Autowired
    private SystemImageService systemImageService;

    @Autowired
    private SystemConfigService systemConfigService;

    private static final String DEFAULT_LOGO = "../default-pic/web-image.png";
    private static final String DEFAULT_FAVICON = "../default-pic/web-image.png";

    @JwtAuth(admin = true)
    @GetMapping
    public Result<PageResult<SystemImageVO>> getImageList(
            @RequestParam(required = false) Integer type,
            @RequestParam Long current,
            @RequestParam Long size) {
        return systemImageService.getImageList(type, current, size);
    }

    @JwtAuth(admin = true)
    @GetMapping("/{imageId}")
    public Result<SystemImageVO> getImageById(@PathVariable Long imageId) {
        return systemImageService.getImageById(imageId);
    }

    @JwtAuth(admin = true)
    @PostMapping("/upload")
    public Result<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String imageName,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer sortOrder) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要上传的图片");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return Result.error(400, "只支持jpg、jpeg、png、gif、webp格式的图片");
        }
        return systemImageService.uploadAndAddImage(file, imageName, type, description, sortOrder);
    }

    @JwtAuth(admin = true)
    @PutMapping("/{imageId}")
    public Result<?> updateImage(
            @PathVariable Long imageId,
            @RequestParam(required = false) String imageName,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer sortOrder) {
        return systemImageService.updateImage(imageId, imageName, type, description, sortOrder);
    }

    @JwtAuth(admin = true)
    @DeleteMapping("/{imageId}")
    public Result<?> deleteImage(@PathVariable Long imageId) {
        return systemImageService.deleteImage(imageId);
    }

    @JwtAuth(admin = true)
    @PutMapping("/{imageId}/status")
    public Result<?> updateImageStatus(@PathVariable Long imageId, @RequestParam Integer status) {
        return systemImageService.updateImageStatus(imageId, status);
    }

    /**
     * 获取轮播图列表（公开接口，无需登录）
     */
    @GetMapping("/public/banners")
    public Result<List<SystemImageVO>> getBannerList() {
        return systemImageService.getBannerList();
    }

    /**
     * 获取平台完整配置（公开接口，无需登录）
     */
    @GetMapping("/public/platform-config")
    public Result<PlatformConfigVO> getPlatformConfig() {
        PlatformConfigVO vo = new PlatformConfigVO();
        vo.setLogoUrl(systemImageService.getLogoUrl().getData());
        vo.setFaviconUrl(systemImageService.getFaviconUrl().getData());
        vo.setDefaultLogoUrl(DEFAULT_LOGO);
        vo.setDefaultFaviconUrl(DEFAULT_FAVICON);
        vo.setConfigs(systemConfigService.getAllConfigs().getData());
        return Result.success(vo);
    }

    @JwtAuth(admin = true)
    @GetMapping("/configs")
    public Result<Map<String, String>> getAllConfigs() {
        return systemConfigService.getAllConfigs();
    }

    @JwtAuth(admin = true)
    @PutMapping("/configs")
    public Result<?> updateConfigs(@RequestBody Map<String, String> configs) {
        return systemConfigService.updateConfigs(configs);
    }
}
