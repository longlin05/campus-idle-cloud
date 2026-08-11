package org.lin.common.context;

import lombok.Data;

import java.util.Date;

@Data
public class ConversationVO {

    private Long userId;

    private String nickname;

    private String avatar;

    private String lastMessage;

    private Date lastMessageTime;

    private Integer unreadCount;

    private Long productId;

    private String productTitle;

    private String productImage;
}
