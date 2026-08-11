package org.lin.common.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类
 * <p>
 * OSSClient 线程安全、内部维护连接池，官方建议复用单个实例。
 * 这里懒加载一个单例 client 供所有上传/删除复用，避免每次操作都 build/shutdown（每次都会重建连接池）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    private final OssConfig ossConfig;

    /** 懒加载的单例 OSSClient（volatile + 双重检查，保证只初始化一次） */
    private volatile OSS ossClient;

    private OSS getOssClient() {
        OSS client = ossClient;
        if (client == null) {
            synchronized (this) {
                client = ossClient;
                if (client == null) {
                    client = new OSSClientBuilder().build(
                            ossConfig.getEndpoint(),
                            ossConfig.getAccessKeyId(),
                            ossConfig.getAccessKeySecret());
                    ossClient = client;
                }
            }
        }
        return client;
    }

    @PreDestroy
    public void destroy() {
        OSS client = ossClient;
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * 上传文件到 OSS
     *
     * @param file     上传的文件
     * @param director 目录，例如 "products"、"avatars"
     * @return 文件的完整访问 URL
     */
    public String upload(MultipartFile file, String director) {
        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 2. 生成文件名：目录/日期/UUID.扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = director + "/" + datePath + "/" + UUID.randomUUID() + extension;

        // 3. 上传到 OSS（复用单例 client，不再每次新建/关闭）
        try {
            InputStream inputStream = file.getInputStream();
            PutObjectResult result = getOssClient().putObject(
                    ossConfig.getBucketName(),
                    fileName,
                    inputStream
            );

            log.info("OSS 上传成功: bucket={}, key={}, etag={}",
                    ossConfig.getBucketName(), fileName, result.getETag());

        } catch (IOException e) {
            log.error("OSS 上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        } catch (Exception e) {
            log.error("OSS 上传异常: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }

        // 4. 返回访问 URL
        return getUrlPrefix() + fileName;
    }

    /**
     * 上传字节数组到 OSS（用于处理头像等场景）
     *
     * @param bytes    文件字节数组
     * @param director 目录
     * @param fileName 文件名（包含扩展名）
     * @return 文件的完整访问 URL
     */
    public String upload(byte[] bytes, String director, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String key = director + "/" + datePath + "/" + fileName;

        try {
            getOssClient().putObject(ossConfig.getBucketName(), key, new java.io.ByteArrayInputStream(bytes));

            log.info("OSS 上传成功: bucket={}, key={}", ossConfig.getBucketName(), key);

        } catch (Exception e) {
            log.error("OSS 上传异常: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }

        return getUrlPrefix() + key;
    }

    /**
     * 删除 OSS 上的文件
     *
     * @param fileUrl 文件完整 URL
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // 从 URL 提取 key
        String urlPrefix = getUrlPrefix();
        if (!fileUrl.startsWith(urlPrefix)) {
            log.warn("文件 URL 不属于当前 OSS Bucket: {}", fileUrl);
            return;
        }

        String key = fileUrl.substring(urlPrefix.length());

        try {
            getOssClient().deleteObject(ossConfig.getBucketName(), key);
            log.info("OSS 删除成功: bucket={}, key={}", ossConfig.getBucketName(), key);

        } catch (Exception e) {
            log.error("OSS 删除失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取 URL 前缀
     */
    private String getUrlPrefix() {
        if (ossConfig.getUrlPrefix() != null && !ossConfig.getUrlPrefix().isEmpty()) {
            return ossConfig.getUrlPrefix();
        }
        // 默认拼接：https://{bucketName}.{endpoint}/
        return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/";
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ".jpg"; // 默认扩展名
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot).toLowerCase();
    }
}