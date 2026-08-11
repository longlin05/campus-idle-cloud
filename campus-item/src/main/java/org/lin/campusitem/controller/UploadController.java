package org.lin.campusitem.controller;

import org.lin.common.jwt.JwtAuth;
import org.lin.common.oss.OssUtil;
import org.lin.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传接口（使用阿里云 OSS）
 */
@RestController
@RequestMapping("/item")
public class UploadController {

    @Autowired
    private OssUtil ossUtil;

    /**
     * 单张图片上传（返回图片URL，供前端先上传再提交表单）
     *
     * @param file     上传的图片文件
     * @param director 上传目录，默认 products，可选 avatars/system
     * @return 上传后的访问 URL
     */
    @JwtAuth
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "directory", required = false, defaultValue = "products") String directory) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }
        // 校验文件大小 10MB
        if (file.getSize() > 10 * 1024 * 1024L) {
            return Result.error(400, "图片大小不能超过10MB");
        }
        String url = ossUtil.upload(file, directory);
        return Result.success(url);
    }

    /**
     * 批量图片上传
     */
    @JwtAuth
    @PostMapping(value = "/upload/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<String>> uploadImages(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(value = "directory", required = false, defaultValue = "products") String directory) {
        if (files == null || files.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            urls.add(ossUtil.upload(file, directory));
        }
        return Result.success(urls);
    }
}