package org.lin.campusorder.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartOrderRequest {
    private List<Long> productIds;
    /** 每个商品对应的购买数量，与productIds一一对应；若为null则默认每件1 */
    private List<Integer> quantities;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}
