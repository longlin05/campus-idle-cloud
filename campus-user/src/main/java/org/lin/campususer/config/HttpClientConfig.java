package org.lin.campususer.config;

import org.lin.common.jwt.InternalAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class HttpClientConfig {

    @Autowired
    private InternalAuthRequestInterceptor internalAuthRequestInterceptor;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 出站请求统一携带内部令牌头，通过下游 /internal/** 鉴权
        restTemplate.setInterceptors(List.of(internalAuthRequestInterceptor));
        return restTemplate;
    }
}
