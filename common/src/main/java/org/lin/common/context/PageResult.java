package org.lin.common.context;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private Long current;
    private Long size;
    private Long total;
    private Long pages;
    private List<T> records;
}