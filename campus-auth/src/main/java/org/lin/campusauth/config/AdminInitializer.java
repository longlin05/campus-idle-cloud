package org.lin.campusauth.config;

import org.lin.common.entity.User;
import org.lin.common.util.Md5Util;
import org.lin.campusauth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 管理员初始化器
 * 应用启动时自动创建/修复默认管理员账户（登录走 campus_auth 库，在此兜底保证可登录）：
 * - 旧版单体项目的默认管理员：13800138000 / admin123
 * - init.sql seed 的管理员：13800000000 / 123456
 *
 * 兼容历史数据：早期 init.sql 曾写入 BCrypt 哈希（$2...），而登录校验是 MD5，
 * 启动时若检测到这类残留哈希会重置为 MD5，保证账号能正常登录。
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void run(String... args) {
        ensureAdmin("13800138000", "admin123", "管理员");
        ensureAdmin("13800000000", "123456", "admin");
    }

    private void ensureAdmin(String phone, String rawPassword, String nickname) {
        User admin = userMapper.findByPhone(phone);
        if (admin == null) {
            admin = new User();
            admin.setNickname(nickname);
            admin.setPhone(phone);
            admin.setEmail("admin@campus-idle.com");
            admin.setPassword(Md5Util.encrypt(rawPassword));
            admin.setRole(0);   // 0 表示管理员
            admin.setStatus(1); // 1 表示正常
            userMapper.insert(admin);
            System.out.println("默认管理员账户创建成功：手机号 " + phone + "，密码 " + rawPassword);
            return;
        }

        // 兼容旧版 init.sql：残留 BCrypt 哈希与 MD5 校验不匹配，重置为 MD5
        String stored = admin.getPassword();
        if (stored != null && stored.startsWith("$2")) {
            admin.setPassword(Md5Util.encrypt(rawPassword));
            userMapper.updateById(admin);
            System.out.println("管理员密码已修复为 MD5：手机号 " + phone + "，密码 " + rawPassword);
        }
    }
}
