package com.caychen.msg.bank1;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.caychen.msg.bank1.dao")
public class DistributedTxMsgBank1Application {

    public static void main(String[] args) {
        SpringApplication.run(DistributedTxMsgBank1Application.class, args);
    }

}
