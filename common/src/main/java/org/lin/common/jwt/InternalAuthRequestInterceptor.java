package org.lin.common.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 出站请求携带内部令牌的 RestTemplate 拦截器。
 * <p>
 * 为调用方服务（admin/order/user 等）的 RestTemplate 统一附加 {@code X-Internal-Token} 头，
 * 使对下游 /internal/** 端点的直连调用通过 {@link InternalAuthInterceptor} 的校验。
 * 对公开端点的请求多带一个头无副作用。
 */
@Slf4j
@Component
public class InternalAuthRequestInterceptor implements ClientHttpRequestInterceptor {

    @Value("${campus.internal-token:campus-internal-token}")
    private String internalToken;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().set(InternalAuthInterceptor.INTERNAL_TOKEN_HEADER, internalToken);
        return execution.execute(request, body);
    }
}
