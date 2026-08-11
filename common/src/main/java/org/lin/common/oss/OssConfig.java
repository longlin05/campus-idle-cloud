package org.lin.common.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {
    /**
     * OSS endpoint，例如 oss-cn-beijing.aliyuncs.com
     */
    private String endpoint;

    /**
     * Bucket 名称
     */
    private String bucketName;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 访问域名前缀，例如 https://campus-idle.oss-cn-beijing.aliyuncs.com/
     * 如果为空，会自动拼接：https://{bucketName}.{endpoint}/
     */
    private String urlPrefix;
}