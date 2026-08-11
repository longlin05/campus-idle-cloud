package org.lin.campususer.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 收货地址 VO（视图对象），兼容两种前端提交格式：
 * <ul>
 *   <li>简化版：{@code receiverAddress} 为完整地址字符串（订单确认页使用）</li>
 *   <li>完整版：{@code province}/{@code city}/{@code detailAddress} 分字段（地址管理页使用）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressVO {

    private Long id;

    private String receiverName;

    private String receiverPhone;

    /** 完整收货地址（简化版，订单确认页使用） */
    private String receiverAddress;

    /** 省份（完整版，地址管理页使用） */
    @JsonAlias({"province"})
    private String province;

    /** 城市（完整版，地址管理页使用） */
    @JsonAlias({"city"})
    private String city;

    /** 区/县（完整版，地址管理页使用） */
    @JsonAlias({"district"})
    private String district;

    /** 详细地址（完整版，地址管理页使用） */
    @JsonAlias({"detailAddress", "detail"})
    private String detailAddress;

    private Integer isDefault;

    /**
     * 获取合并后的地址字符串（兼容两种格式）。
     */
    public String getFullAddress() {
        if (receiverAddress != null && !receiverAddress.isEmpty()) {
            return receiverAddress;
        }
        StringBuilder sb = new StringBuilder();
        if (province != null) sb.append(province);
        if (city != null) sb.append(city);
        if (district != null) sb.append(district);
        if (detailAddress != null) sb.append(detailAddress);
        return sb.toString();
    }
}
