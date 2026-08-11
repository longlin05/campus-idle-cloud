package org.lin.campusauth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"org.lin.campusauth", "org.lin.common"})
@MapperScan("org.lin.campusauth.mapper")
public class CampusAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusAuthApplication.class, args);
    }

}