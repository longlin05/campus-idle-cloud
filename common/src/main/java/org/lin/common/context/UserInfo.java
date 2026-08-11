package org.lin.common.context;

import lombok.Data;
import java.util.Date;

@Data
public class UserInfo {
    private Long id;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer role;
    private Integer status;
    private Date createTime;
    private Date lastLoginTime;
}