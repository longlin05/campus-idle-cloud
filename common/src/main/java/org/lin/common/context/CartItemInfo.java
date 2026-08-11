package org.lin.common.context;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CartItemInfo implements Serializable {

    private Long itemId;

    private Long productId;

    private String name;

    private Double price;

    private Integer quantity;

    private Boolean selected;

    private List<String> images;

    private String categoryName;

    private Integer stock;
}
