package org.lin.common.dto;

import lombok.Data;

@Data
public class SendChatRequest {

    private Long receiverId;

    private Long productId;

    private String content;
}
