package org.lin.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CodeUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 验证码长度
    private static final int CODE_LENGTH = 6;
    // 验证码过期时间（分钟）
    private static final int EXPIRE_TIME = 5;
    // 验证码前缀
    private static final String CODE_PREFIX = "sms:code:";
    // 用户信息前缀
    private static final String USER_PREFIX = "user:info:";

    /**
     * 生成6位随机验证码
     * @return 6位数字验证码
     */
    public String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 发送验证码（模拟）
     * @param phone 手机号
     * @return 生成的验证码
     */
    public String sendCode(String phone) {
        // 生成验证码
        String code = generateCode();
        
        // 存储验证码到Redis
        String key = CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(key, code, EXPIRE_TIME, TimeUnit.MINUTES);
        
        // 模拟发送短信
        System.out.println("向手机号 " + phone + " 发送验证码: " + code);
        
        return code;
    }

    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证通过
     */
    public boolean verifyCode(String phone, String code) {
        String key = CODE_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            return false;
        }
        
        boolean result = storedCode.equals(code);
        
        // 验证成功后删除验证码
        if (result) {
            redisTemplate.delete(key);
        }
        
        return result;
    }

    /**
     * 验证手机号格式
     * @param phone 手机号
     * @return 是否格式正确
     */
    public boolean validatePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 保存用户信息到Redis Hash
     * @param phone 手机号
     * @param userInfo 用户信息
     * @return 生成的用户token
     */
    public String saveUserInfo(String phone, Map<String, Object> userInfo) {
        // 生成随机token
        String token = UUID.randomUUID().toString();
        String key = USER_PREFIX + token;
        
        // 存储用户信息到Redis Hash
        redisTemplate.opsForHash().putAll(key, userInfo);
        
        // 设置过期时间（例如24小时）
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
        
        return token;
    }

    /**
     * 根据token获取用户信息
     * @param token 用户token
     * @return 用户信息
     */
    public Map<Object, Object> getUserInfo(String token) {
        String key = USER_PREFIX + token;
        return redisTemplate.opsForHash().entries(key);
    }
}