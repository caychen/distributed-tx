package com.caychen.notify.account.consumer;

import com.alibaba.fastjson.JSON;
import com.caychen.notify.account.feign.IPayServiceClient;
import com.caychen.notify.account.service.IDepositRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 20:41
 * @Description:
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${consumer.callback.account.topic}", consumerGroup = "${rocketmq.consumer.group}")
public class CallbackMessageMessageListener implements RocketMQListener<CallbackData> {

    @Autowired
    private IDepositRecordService depositRecordService;

    @Autowired
    private IPayServiceClient payServiceClient;

    @Override
    public void onMessage(CallbackData message) {
        log.info("接收到消息：[{}]", JSON.toJSONString(message));

        String txNo = message.getTxNo();

        //处理本地的转账结果
        Boolean isOk = false;
        try {
            isOk = depositRecordService.dealDepositResult(message);
        } catch (Exception e) {
            log.error("捕获异常：", e);
        }

        //通知pay服务结果
        payServiceClient.notifyMqResult(isOk, txNo);

    }
}
