package org.lin.common.context;

import lombok.Data;

/**
 * 用户主页聚合 VO，包含：
 * - 用户基本资料（头像/昵称/注册时间等）
 * - 统计信息（粉丝/关注/在售商品数/已售商品数）
 * <p>
 * 由 campus-user 返回给前端 UserHomeView 使用。
 */
@Data
public class UserHomeVO {
    /** 用户 ID */
    private Long id;
    /** 昵称 */
    private String nickname;
    /** 手机（脱敏显示） */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 头像 URL */
    private String avatar;
    /** 账号状态：1-正常 0-禁用 */
    private Integer status;
    /** 注册时间 */
    private java.util.Date createTime;
    /** 最后登录时间 */
    private java.util.Date lastLoginTime;

    /** 关注数（TA 关注了多少人） */
    private Integer followCount;
    /** 粉丝数（多少人关注了 TA） */
    private Integer fansCount;

    /** 是否已经是当前登录用户关注的（仅登录时返回） */
    private Boolean followedByMe;
}
