package org.lin.campusitem.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.lin.common.jwt.InternalAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class HttpClientConfig {

    @Autowired
    private InternalAuthRequestInterceptor internalAuthRequestInterceptor;

    /**
     * 池化的 HttpClient：复用 TCP 连接，避免每个请求新建连接。
     */
    @Bean
    public HttpClient pooledHttpClient() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(50);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(3000))
                .setResponseTimeout(Timeout.ofMilliseconds(5000))
                .build();
        return HttpClientBuilder.create()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }

    @Bean
    public RestTemplate restTemplate(HttpClient pooledHttpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(pooledHttpClient);
        factory.setConnectTimeout(3000);
        factory.setConnectionRequestTimeout(3000);
        // 读超时(responseTimeout)已在 pooledHttpClient 的 RequestConfig 中配置
        RestTemplate restTemplate = new RestTemplate(factory);
        // 出站请求统一携带内部令牌头，通过下游 /internal/** 鉴权（对公开端点无副作用）
        restTemplate.setInterceptors(List.of(internalAuthRequestInterceptor));
        return restTemplate;
    }
}
