package org.lin.common.context;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class ProductInfo {
    private Long id;
    private String name;
    private String title;
    private String description;
    private Double price;
    private Double originalPrice;
    private Integer stock;
    private Integer quantity;
    private Integer tradeType;
    private Integer categoryId;
    private String categoryName;
    private List<String> images;
    private String imageUrl;
    private Integer status;
    private Integer viewCount;
    private Date createTime;
    private Date updateTime;
    private Long sellerId;
    private String sellerName;
    private String sellerNickname;
    private String sellerPhone;
    private String sellerAvatar;
    private Integer sellerProductCount;
}