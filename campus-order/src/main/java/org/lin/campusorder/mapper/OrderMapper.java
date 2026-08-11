package org.lin.campusorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lin.common.entity.OrderInfo;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<OrderInfo> {
    @Select("SELECT * FROM order_info WHERE order_id = #{orderId} AND is_deleted = 0")
    OrderInfo findByOrderId(Long orderId);

    /**
     * 原子订单状态流转（乐观锁式条件更新）。
     * <p>
     * 仅在订单当前状态等于 {@code oldStatus} 时才更新为 {@code newStatus}，
     * 并同时维护各状态对应的时间戳列。返回受影响行数（0 表示并发下状态已变更）。
     * 用于解决「支付/发货/收货/取消/退款」与「超时自动取消」之间的竞态：
     * 例如调度器读到待付款后，用户先完成支付并提交，调度器的取消更新将因 WHERE status=0
     * 匹配不到行而被跳过，避免已支付订单被误取消、库存被双重恢复。
     */
    @Update("UPDATE order_info SET status = #{newStatus}, update_time = NOW(), " +
            "pay_time = CASE WHEN #{newStatus} = 1 THEN NOW() ELSE pay_time END, " +
            "ship_time = CASE WHEN #{newStatus} = 2 THEN NOW() ELSE ship_time END, " +
            "confirm_time = CASE WHEN #{newStatus} = 3 THEN NOW() ELSE confirm_time END, " +
            "cancel_time = CASE WHEN #{newStatus} = 4 THEN NOW() ELSE cancel_time END " +
            "WHERE order_id = #{orderId} AND status = #{oldStatus} AND is_deleted = 0")
    int compareAndSetStatus(@Param("orderId") Long orderId,
                            @Param("oldStatus") Integer oldStatus,
                            @Param("newStatus") Integer newStatus);

    @Select("SELECT * FROM order_info WHERE buyer_id = #{buyerId} AND is_deleted = 0")
    IPage<OrderInfo> selectPageByBuyerId(Page<OrderInfo> page, @Param("buyerId") Long buyerId);

    @Select("SELECT * FROM order_info WHERE seller_id = #{sellerId} AND is_deleted = 0")
    IPage<OrderInfo> selectPageBySellerId(Page<OrderInfo> page, @Param("sellerId") Long sellerId);

    @Select("SELECT * FROM order_info WHERE buyer_id = #{buyerId} AND status = #{status} AND is_deleted = 0")
    IPage<OrderInfo> selectPageByBuyerIdAndStatus(Page<OrderInfo> page, @Param("buyerId") Long buyerId, @Param("status") Integer status);

    @Select("SELECT * FROM order_info WHERE seller_id = #{sellerId} AND status = #{status} AND is_deleted = 0")
    IPage<OrderInfo> selectPageBySellerIdAndStatus(Page<OrderInfo> page, @Param("sellerId") Long sellerId, @Param("status") Integer status);

    /**
     * 按订单号查询订单（前端轮询订单状态用）。
     * <p>
     * 购物车场景下多个订单可能共享同一 orderNo，这里返回第一条即可。
     */
    @Select("SELECT * FROM order_info WHERE order_no = #{orderNo} AND is_deleted = 0 LIMIT 1")
    OrderInfo findByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT COUNT(*) FROM order_info WHERE is_deleted = 0")
    long countAll();

    @Select("SELECT COUNT(*) FROM order_info WHERE is_deleted = 0 AND DATE(create_time) = #{date}")
    long countTodayOrders(@Param("date") String date);

    @Select("SELECT COALESCE(SUM(order_amount), 0) FROM order_info WHERE is_deleted = 0 AND DATE(pay_time) = #{date} AND status IN (1, 2, 3)")
    java.math.BigDecimal sumTodayAmount(@Param("date") String date);

    @Select("SELECT COALESCE(SUM(order_amount), 0) FROM order_info WHERE is_deleted = 0 AND status IN (1, 2, 3)")
    java.math.BigDecimal sumTotalAmount();

    /**
     * 查询超时未支付的订单（status=0 且创建时间早于指定分钟数）。
     */
    @Select("SELECT * FROM order_info WHERE is_deleted = 0 AND status = 0 " +
            "AND create_time <= DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<OrderInfo> selectTimeoutUnpaidOrders(@Param("timeoutMinutes") int timeoutMinutes);
}