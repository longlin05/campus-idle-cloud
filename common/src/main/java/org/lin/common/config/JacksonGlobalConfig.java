package org.lin.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置
 * 关闭反序列化时对未知字段的严格校验，避免前端多传字段导致 400 错误。
 * 各微服务通过 @ComponentScan("org.lin.common") 共享此配置。
 */
@Configuration
public class JacksonGlobalConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.featuresToDisable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        );
    }
}
