package org.lin.campususer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 用户资料更新请求。
 * 使用 @JsonIgnoreProperties(ignoreUnknown = true) 忽略前端传来的
 * 尚未在后端实现的字段（如 school/major/gender/intro），避免 400 Bad Request。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoRequest {
    private String nickname;
    private String email;
    /** 头像 URL（OSS 上传成功后由前端传入） */
    private String avatar;
}
