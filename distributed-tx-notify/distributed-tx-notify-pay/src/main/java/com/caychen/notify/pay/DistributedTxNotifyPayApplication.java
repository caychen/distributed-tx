package com.caychen.notify.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@MapperScan("com.caychen.notify.pay.dao")
@EnableScheduling
public class DistributedTxNotifyPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedTxNotifyPayApplication.class, args);
    }

}
