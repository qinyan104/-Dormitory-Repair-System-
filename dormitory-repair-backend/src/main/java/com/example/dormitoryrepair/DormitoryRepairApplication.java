package com.example.dormitoryrepair;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.dormitoryrepair.mapper")
@EnableAsync
@EnableScheduling
public class DormitoryRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(DormitoryRepairApplication.class, args);
    }
}
