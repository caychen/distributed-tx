package com.caychen.seata.bank;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.caychen.seata.bank.dao")
@EnableFeignClients("com.caychen.seata.bank.feign")
public class DistributedTxSeataBank1Application {

    public static void main(String[] args) {
        SpringApplication.run(DistributedTxSeataBank1Application.class, args);
    }

}
