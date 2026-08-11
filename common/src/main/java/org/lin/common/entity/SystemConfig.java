package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("idle_system_config")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long configId;
    private String configKey;
    private String configValue;
    private String description;
    private Date createTime;
    private Date updateTime;
}
