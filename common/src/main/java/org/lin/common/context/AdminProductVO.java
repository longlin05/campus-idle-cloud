package org.lin.common.context;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class AdminProductVO {
    private Long id;
    private Long productId;
    private String productName;
    private String title;
    private String description;
    private List<String> images;
    private String productImage;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer quantity;
    private Integer status;
    private Date createTime;
    private Long sellerId;
    private String sellerName;
    private Integer viewCount;
    private Integer tradeType;
}
