package com.asset;

import com.asset.entity.SysUser;
import com.asset.mapper.SysUserMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

/**
 * 企业固定资产审批系统启动类
 */
@SpringBootApplication
@MapperScan("com.asset.mapper")
public class AssetApprovalApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetApprovalApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 启动时自动将数据库里的旧明文密码升级为 BCrypt 密文
     */
    @Bean
    public CommandLineRunner encryptLegacyPasswords(SysUserMapper userMapper, BCryptPasswordEncoder encoder) {
        return args -> {
            List<SysUser> users = userMapper.selectList(null);
            for (SysUser u : users) {
                if (u.getPassword() != null && !u.getPassword().startsWith("$2a$")) {
                    u.setPassword(encoder.encode(u.getPassword()));
                    userMapper.updateById(u);
                }
            }
        };
    }
}
