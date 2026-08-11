package org.lin.campusorder.client;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.ProductInfo;
import org.lin.common.result.Result;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

/**
 * 商品服务客户端：调用 campus-item 的内部 API 获取商品信息和管理库存。
 * 根据 base-url 自动选择 RestTemplate：
 *   - URL 包含 localhost 时使用 directRestTemplate（直连）
 *   - URL 为服务名（如 campus-item）时使用 loadBalancedRestTemplate（Nacos发现）
 *
 * <p>重试策略：
 * <ul>
 *   <li>GET（查商品信息）：对全部瞬时异常（连接、超时、5xx）重试 3 次，安全无副作用；</li>
 *   <li>PUT（扣减/恢复库存）：仅对「连接级失败」（连接被拒/对端关闭/域名解析失败）重试——
 *       这类失败请求确定未到达服务端，重试不会重复扣减；
 *       响应超时等「请求可能已生效」的失败不重试，交由上层回滚/提示，避免库存被重复扣减或恢复。</li>
 *   <li>4xx 业务/客户端错误不重试。</li>
 * </ul>
 */
@Slf4j
@Component
public class ProductClient {

    /** 最大重试次数（含首次调用共 MAX_ATTEMPTS 次） */
    private static final int MAX_ATTEMPTS = 3;
    /** 重试退避基数（毫秒），第 n 次重试等待 BACKOFF_MS * n */
    private static final long BACKOFF_MS = 200;

    @Value("${campus.item.base-url:http://localhost:8083}")
    private String itemBaseUrl;

    private final RestTemplate loadBalancedRestTemplate;
    private final RestTemplate directRestTemplate;

    public ProductClient(
            @Qualifier("loadBalancedRestTemplate") RestTemplate loadBalancedRestTemplate,
            @Qualifier("directRestTemplate") RestTemplate directRestTemplate) {
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
        this.directRestTemplate = directRestTemplate;
    }

    private RestTemplate chooseRestTemplate() {
        // localhost 或 127.0.0.1 走直连，其他走Nacos服务发现
        if (itemBaseUrl.contains("localhost") || itemBaseUrl.contains("127.0.0.1")) {
            return directRestTemplate;
        }
        return loadBalancedRestTemplate;
    }

    /**
     * 获取商品信息（含库存、价格、卖家等）。GET 安全，对全部瞬时异常重试。
     */
    public ProductInfo getProduct(Long productId) {
        String url = itemBaseUrl + "/item/internal/product/" + productId;
        RestTemplate rt = chooseRestTemplate();
        return executeWithRetry("getProduct", url, rt, true, () -> {
            ParameterizedTypeReference<Result<ProductInfo>> typeRef =
                    new ParameterizedTypeReference<Result<ProductInfo>>() {};
            ResponseEntity<Result<ProductInfo>> response = rt.exchange(
                    url, HttpMethod.GET, null, typeRef);
            Result<ProductInfo> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return result.getData();
            }
            log.warn("[ProductClient-getProduct] 业务失败 productId={} result={}", productId, result);
            return null;
        }, null);
    }

    /**
     * 扣减库存（下单时调用）。
     * @return true 扣减成功，false 库存不足或商品不存在
     */
    public boolean reduceStock(Long productId, Integer quantity) {
        String url = itemBaseUrl + "/item/internal/product/" + productId + "/stock/reduce?quantity=" + quantity;
        RestTemplate rt = chooseRestTemplate();
        return executeWithRetry("reduceStock", url, rt, false, () -> {
            ParameterizedTypeReference<Result<String>> typeRef =
                    new ParameterizedTypeReference<Result<String>>() {};
            ResponseEntity<Result<String>> response = rt.exchange(
                    url, HttpMethod.PUT, null, typeRef);
            Result<String> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return true;
            }
            log.warn("[ProductClient-reduceStock] 业务失败 productId={} quantity={} result={}", productId, quantity, result);
            return false;
        }, false);
    }

    /**
     * 恢复库存（订单取消/退款时调用）。
     */
    public boolean restoreStock(Long productId, Integer quantity) {
        String url = itemBaseUrl + "/item/internal/product/" + productId + "/stock/restore?quantity=" + quantity;
        RestTemplate rt = chooseRestTemplate();
        return executeWithRetry("restoreStock", url, rt, false, () -> {
            ParameterizedTypeReference<Result<String>> typeRef =
                    new ParameterizedTypeReference<Result<String>>() {};
            ResponseEntity<Result<String>> response = rt.exchange(
                    url, HttpMethod.PUT, null, typeRef);
            Result<String> result = response.getBody();
            if (result != null && result.getCode() == 200) {
                return true;
            }
            log.warn("[ProductClient-restoreStock] 业务失败 productId={} quantity={} result={}", productId, quantity, result);
            return false;
        }, false);
    }

    /**
     * 带重试的执行器。
     *
     * @param action            日志标识（如 getProduct）
     * @param url               目标 URL（仅用于日志）
     * @param rt                RestTemplate
     * @param retryAllTransient true 表示对全部瞬时异常重试（GET 等幂等操作）；
     *                          false 表示仅对连接级失败重试（避免重复扣减库存）
     * @param operation         实际调用
     * @param onFailure         重试耗尽后的兜底返回值
     */
    private <T> T executeWithRetry(String action, String url, RestTemplate rt, boolean retryAllTransient,
                                   Supplier<T> operation, T onFailure) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (HttpClientErrorException e) {
                // 4xx 客户端错误（如商品不存在），重试无意义
                log.warn("[ProductClient-{}] HTTP 4xx，不重试 url={} status={}", action, url, e.getStatusCode());
                return onFailure;
            } catch (RestClientException e) {
                last = e;
                if (!retryAllTransient && !isConnectLevel(e)) {
                    // 请求可能已到达服务端（如响应读超时），为避免库存重复扣减/恢复，不重试
                    log.error("[ProductClient-{}] 非连接级异常，不重试 url={} err={}", action, url, e.getMessage());
                    return onFailure;
                }
                if (attempt < MAX_ATTEMPTS) {
                    long wait = BACKOFF_MS * attempt;
                    log.warn("[ProductClient-{}] 第{}次调用失败，{}ms后重试 url={} err={}",
                            action, attempt, wait, url, e.getMessage());
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("[ProductClient-{}] 重试{}次后仍失败 url={}", action, MAX_ATTEMPTS, url, last);
        return onFailure;
    }

    /**
     * 判断异常是否为「连接级失败」：请求确定未到达服务端，重试安全无副作用。
     */
    private boolean isConnectLevel(RestClientException e) {
        Throwable c = e;
        while (c != null) {
            String name = c.getClass().getName();
            if (name.endsWith("ConnectException")
                    || name.endsWith("ConnectTimeoutException")
                    || name.endsWith("ConnectionClosedException")
                    || name.endsWith("UnknownHostException")) {
                return true;
            }
            c = c.getCause();
        }
        return false;
    }
}
