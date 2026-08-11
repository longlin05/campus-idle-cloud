package org.lin.campusorder.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.lin.common.jwt.InternalAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Autowired
    private InternalAuthRequestInterceptor internalAuthRequestInterceptor;

    /**
     * 池化的 HttpClient：复用 TCP 连接，避免每个请求都新建连接（SimpleClientHttpRequestFactory 的缺点）。
     * 跨服务调用（商品库存扣减/恢复等）在高并发下依赖连接池提升吞吐。
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

    /**
     * 带负载均衡的 RestTemplate：用于通过 Nacos 服务名调用其他微服务。
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(HttpClient pooledHttpClient) {
        RestTemplate restTemplate = new RestTemplate(buildFactory(pooledHttpClient));
        restTemplate.setInterceptors(List.of(internalAuthRequestInterceptor));
        return restTemplate;
    }

    /**
     * 普通 RestTemplate：用于直连固定地址（localhost / 服务容器地址）。
     */
    @Bean
    public RestTemplate directRestTemplate(HttpClient pooledHttpClient) {
        RestTemplate restTemplate = new RestTemplate(buildFactory(pooledHttpClient));
        restTemplate.setInterceptors(List.of(internalAuthRequestInterceptor));
        return restTemplate;
    }

    private HttpComponentsClientHttpRequestFactory buildFactory(HttpClient client) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        factory.setConnectTimeout(3000);
        factory.setConnectionRequestTimeout(3000);
        // 读超时(responseTimeout)已在 pooledHttpClient 的 RequestConfig 中配置
        return factory;
    }
}
