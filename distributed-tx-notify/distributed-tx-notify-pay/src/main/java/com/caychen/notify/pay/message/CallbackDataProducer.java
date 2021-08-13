package com.caychen.notify.pay.message;

import com.caychen.notify.pay.entity.PayRecord;
import com.caychen.notify.pay.service.IPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * @Author: Caychen
 * @Date: 2021/8/8 17:24
 * @Description:
 */
@Slf4j
@Component
public class CallbackDataProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${producer.notify.callback.topic}")
    private String notifyCallbackTopic;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IPayService payService;


    public void sendCallbackData(CallbackData callbackData) {
        //如果callbackUrl为空，则使用mq发送回调
        rocketMQTemplate.convertAndSend(notifyCallbackTopic, callbackData);
    }

    @Transactional(rollbackFor = Exception.class)
    public void callUrlInterface(PayRecord payRecord, String callbackUrl, CallbackData callbackData) {
        HttpEntity httpEntity = new HttpEntity(callbackData);
        Boolean returnValue = false;
        try {
            returnValue = restTemplate.postForObject(callbackUrl, httpEntity, Boolean.class);
        } catch (Exception e) {
            log.error("捕获异常,", e);
        }
        payRecord.setCallbackResult(returnValue);
        payService.updateById(payRecord);
    }
}
