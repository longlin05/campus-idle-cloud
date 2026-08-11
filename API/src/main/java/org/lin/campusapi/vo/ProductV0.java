package org.lin.campusapi.vo;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class ProductV0 {
    // 商品ID
    private Long id;
    // 商品名称
    private String name;
    // 商品描述
    private String description;
    // 商品价格
    private Double price;
    // 商品库存
    private Integer stock;
    // 商品分类ID
    private Long categoryId;
    // 商品分类名称
    private String categoryName;
    // 商品图片列表
    private List<String> images;
    // 商品状态
    private Integer status;
    // 发布时间
    private Date createTime;
    // 更新时间
    private Date updateTime;
}