package org.lin.common.dto;

import lombok.Data;

@Data
public class MarkReadRequest {

    private Long userId;

    private Long productId;
}
