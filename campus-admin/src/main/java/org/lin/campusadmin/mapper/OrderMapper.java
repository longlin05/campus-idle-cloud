package org.lin.campusadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lin.common.entity.OrderInfo;

import java.math.BigDecimal;
import java.util.Date;

@Mapper
public interface OrderMapper extends BaseMapper<OrderInfo> {
    @Select("SELECT COUNT(*) FROM order_info WHERE is_deleted = 0 AND DATE(create_time) = #{date}")
    long countTodayOrders(Date date);

    @Select("SELECT COALESCE(SUM(order_amount), 0) FROM order_info WHERE is_deleted = 0 AND DATE(pay_time) = #{date} AND status IN (1, 2, 3)")
    BigDecimal sumTodayAmount(Date date);
}