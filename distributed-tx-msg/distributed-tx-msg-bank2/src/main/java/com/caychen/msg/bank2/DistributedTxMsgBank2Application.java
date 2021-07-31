package com.caychen.msg.bank2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.caychen.msg.bank2.dao")
public class DistributedTxMsgBank2Application {

    public static void main(String[] args) {
        SpringApplication.run(DistributedTxMsgBank2Application.class, args);
    }

}
