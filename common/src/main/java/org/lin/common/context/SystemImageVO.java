package org.lin.common.context;

import lombok.Data;

import java.util.Date;

/**
 * 系统图片视图对象
 */
@Data
public class SystemImageVO {
    private Long imageId;
    private String imageName;
    private String imageUrl;
    private String description;
    private Integer type;
    private Integer status;
    private Integer sortOrder;
    private Date createTime;
}
