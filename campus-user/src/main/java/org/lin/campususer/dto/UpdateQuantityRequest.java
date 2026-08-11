package org.lin.campususer.dto;

import lombok.Data;

@Data
public class UpdateQuantityRequest {
    private Long productId;
    private Integer quantity;
}
