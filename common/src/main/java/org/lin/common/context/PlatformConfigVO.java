package org.lin.common.context;

import lombok.Data;

import java.util.Map;

/**
 * 平台整体配置视图对象
 * 聚合系统图片（Logo/Favicon）和文本配置
 */
@Data
public class PlatformConfigVO {
    private String logoUrl;
    private String faviconUrl;
    private String defaultLogoUrl;
    private String defaultFaviconUrl;
    private Map<String, String> configs;
}
