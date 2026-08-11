package org.lin.campusitem.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.kafka.KafkaMessage;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.consumer.AbstractKafkaConsumer;
import org.lin.campusitem.kafka.dto.ViewCountPayload;
import org.lin.campusitem.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品浏览量计数消费者。
 * <p>
 * 接收 {@link ViewCountPayload} 事件，本地聚合后批量落库到 DB。
 * <p>
 * 聚合策略：在内存中按 productId 累加 count，触发条件（任一满足）即批量落库：
 * <ol>
 *   <li>缓冲区商品数达到 {@link #FLUSH_THRESHOLD}（100）</li>
 *   <li>定时任务每 10 秒触发一次（避免低频浏览长期不落库）</li>
 * </ol>
 * <p>
 * 相比"每次浏览一条 UPDATE SQL"，本方案将 DB 写入压力降低 1-2 个数量级。
 * <p>
 * 线程安全：消费线程（Kafka 监听，concurrency=1）与定时落库线程（调度器）
 * 通过 {@link #lock} 串行化「合并」与「缓冲区换出」。锁只保护 O(1) 的换出操作，
 * 不包含 DB 落库。换出后的旧缓冲区不再有任何写入，可安全读取快照，
 * 杜绝「快照后、清空前」并发写入被清掉导致的浏览量丢失；也避免换出竞态下的重复计数。
 */
@Slf4j
@Component
public class ViewCountConsumer extends AbstractKafkaConsumer<ViewCountPayload> {

    @Autowired
    private ProductMapper productMapper;

    /** 串行化「合并」与「换出」的锁 */
    private final Object lock = new Object();

    /** 本地聚合缓冲区：productId -> 累计浏览增量（由 lock 保护） */
    private Map<Long, Integer> buffer = new HashMap<>();

    /** 触发批量落库的阈值（不同商品数） */
    private static final int FLUSH_THRESHOLD = 100;

    @KafkaListener(
            topics = KafkaTopicConstants.PRODUCT_VIEW_COUNT,
            groupId = "campus-item-view-group",
            // 浏览量统计需单线程聚合避免计数错乱，显式指定 concurrency=1
            concurrency = "1",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(KafkaMessage message) {
        super.handle(message, ViewCountPayload.class);
    }

    @Override
    protected void doConsume(String eventId, ViewCountPayload payload) {
        if (payload == null || payload.getProductId() == null || payload.getCount() == null) {
            log.warn("[浏览量消费] payload 字段缺失，跳过 eventId={}", eventId);
            return;
        }

        Map<Long, Integer> toFlush = null;
        synchronized (lock) {
            // 聚合到缓冲区（锁内合并，与换出互斥）
            Long productId = payload.getProductId();
            buffer.merge(productId, payload.getCount(), Integer::sum);

            // 达到阈值触发批量落库（锁内换出，避免与正在进行的落库冲突）
            if (buffer.size() >= FLUSH_THRESHOLD) {
                toFlush = buffer;
                buffer = new HashMap<>();
            }
        }

        if (toFlush != null) {
            persist(toFlush);
        }
    }

    /**
     * 定时兜底：每 10 秒刷新一次缓冲区。
     * <p>
     * 防止低频浏览场景下缓冲区长期不满，导致浏览量延迟落库。
     */
    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void scheduledFlush() {
        Map<Long, Integer> toFlush;
        synchronized (lock) {
            if (buffer.isEmpty()) {
                return;
            }
            toFlush = buffer;
            buffer = new HashMap<>();
        }
        persist(toFlush);
    }

    /**
     * 将换出缓冲区的累计增量批量落库到 DB。
     * <p>
     * 使用 {@code incrementViewCountByDelta} 将 N 次浏览合并为 1 条 UPDATE SQL，
     * 显著降低 DB 写入压力。落库不持有锁，不影响消费线程继续聚合。
     */
    private void persist(Map<Long, Integer> snapshot) {
        log.info("[浏览量消费] 批量落库开始 products={} totalDelta={}",
                snapshot.size(), snapshot.values().stream().mapToInt(Integer::intValue).sum());

        for (Map.Entry<Long, Integer> entry : snapshot.entrySet()) {
            try {
                productMapper.incrementViewCountByDelta(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("[浏览量消费] 落库失败 productId={} delta={}",
                        entry.getKey(), entry.getValue(), e);
                // 失败的增量放回缓冲区（锁内合并），等待下次重试
                synchronized (lock) {
                    buffer.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        log.info("[浏览量消费] 批量落库完成");
    }
}
