package org.lin.campusapi.vo;

import lombok.Data;
import java.util.Date;

@Data
public class UserV0 {
    // 用户ID
    private Long id;
    // 用户名
    private String username;
    // 用户昵称
    private String nickname;
    // 手机号
    private String phone;
    // 邮箱
    private String email;
    // 头像
    private String avatar;
    // 用户状态
    private Integer status;
    // 用户角色 0-管理员 1-普通用户
    private Integer role;
    // 注册时间
    private Date createTime;
    // 最后登录时间
    private Date lastLoginTime;
}