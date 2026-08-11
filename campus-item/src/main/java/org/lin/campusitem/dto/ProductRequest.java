package org.lin.campusitem.dto;

import lombok.Data;
import org.lin.common.entity.Product;
import java.util.List;

@Data
public class ProductRequest {
    private Product product;
    private List<String> images;
}
