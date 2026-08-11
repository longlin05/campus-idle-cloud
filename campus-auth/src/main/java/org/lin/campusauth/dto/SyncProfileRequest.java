package org.lin.campusauth.dto;

import lombok.Data;

@Data
public class SyncProfileRequest {
    private Long userId;
    private String avatar;
    private String nickname;
    private String email;
}
