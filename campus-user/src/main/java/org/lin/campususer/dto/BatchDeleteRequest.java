package org.lin.campususer.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchDeleteRequest {
    private List<Long> productIds;
}
