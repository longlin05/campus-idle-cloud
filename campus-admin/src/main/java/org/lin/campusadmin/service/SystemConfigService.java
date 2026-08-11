package org.lin.campusadmin.service;

import org.lin.common.result.Result;

import java.util.Map;

public interface SystemConfigService {
    Result<Map<String, String>> getAllConfigs();
    Result<?> updateConfigs(Map<String, String> configs);
}
