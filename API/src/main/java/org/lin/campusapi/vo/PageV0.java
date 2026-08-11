package org.lin.campusapi.vo;

import lombok.Data;
import java.util.List;

@Data
public class PageV0<T> {
    // 当前页码
    private Long current;
    // 每页大小
    private Long size;
    // 总记录数
    private Long total;
    // 总页数
    private Long pages;
    // 数据列表
    private List<T> records;
}