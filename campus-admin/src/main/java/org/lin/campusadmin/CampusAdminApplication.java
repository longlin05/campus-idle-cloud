package org.lin.campusadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"org.lin.campusadmin", "org.lin.common"})
@MapperScan("org.lin.campusadmin.mapper")
public class CampusAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusAdminApplication.class, args);
    }
}