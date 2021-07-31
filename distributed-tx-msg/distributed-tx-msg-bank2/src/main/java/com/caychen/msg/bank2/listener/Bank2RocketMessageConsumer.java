package com.caychen.msg.bank2.listener;

import com.alibaba.fastjson.JSON;
import com.caychen.msg.bank2.dto.TransferRequest;
import com.caychen.msg.bank2.service.IAccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: Caychen
 * @Date: 2021/7/6 11:32
 * @Description:
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${bank2.consumer.topic}", consumerGroup = "${rocketmq.consumer.group}")
public class Bank2RocketMessageConsumer implements RocketMQListener<String> {

    @Autowired
    private IAccountService accountService;

    @Override
    public void onMessage(String message) {
        log.info("bank2接收到消息：[{}]", message);

        TransferRequest transferRequest = JSON.parseObject(message, TransferRequest.class);
        log.info("消费txNo: [{}]", transferRequest.getTxNo());

        try {
            accountService.addAccountInfoBalance(transferRequest);
            log.info("bank2消费成功...");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("bank2消费失败...");
        }
    }
}
