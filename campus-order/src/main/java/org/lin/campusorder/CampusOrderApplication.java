package org.lin.campusorder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = {"org.lin.campusorder", "org.lin.common"})
@MapperScan("org.lin.campusorder.mapper")
public class CampusOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusOrderApplication.class, args);
    }
}