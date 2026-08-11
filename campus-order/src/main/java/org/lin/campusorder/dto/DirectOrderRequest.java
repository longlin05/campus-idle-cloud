package org.lin.campusorder.dto;

import lombok.Data;

@Data
public class DirectOrderRequest {
    private Long productId;
    private Integer quantity;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}
