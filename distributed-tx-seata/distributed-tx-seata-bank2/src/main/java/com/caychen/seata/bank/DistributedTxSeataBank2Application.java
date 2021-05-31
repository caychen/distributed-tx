package com.caychen.seata.bank;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.caychen.seata.bank.dao")
public class DistributedTxSeataBank2Application {

    public static void main(String[] args) {
        SpringApplication.run(DistributedTxSeataBank2Application.class, args);
    }

}
