package org.lin.campusadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.lin.common.result.Result;
import org.lin.common.entity.SystemConfig;
import org.lin.campusadmin.mapper.SystemConfigMapper;
import org.lin.campusadmin.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public Result<Map<String, String>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return Result.success(result);
    }

    @Override
    public Result<?> updateConfigs(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()) {
            return Result.error(400, "配置不能为空");
        }
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemConfig::getConfigKey, key);
            SystemConfig existing = systemConfigMapper.selectOne(wrapper);

            if (existing != null) {
                existing.setConfigValue(value);
                existing.setUpdateTime(new Date());
                systemConfigMapper.updateById(existing);
            } else {
                SystemConfig newConfig = new SystemConfig();
                newConfig.setConfigKey(key);
                newConfig.setConfigValue(value);
                newConfig.setDescription("");
                newConfig.setCreateTime(new Date());
                newConfig.setUpdateTime(new Date());
                systemConfigMapper.insert(newConfig);
            }
        }
        return Result.success("配置更新成功");
    }
}
