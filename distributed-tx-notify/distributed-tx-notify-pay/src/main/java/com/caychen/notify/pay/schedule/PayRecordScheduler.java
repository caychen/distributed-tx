package com.caychen.notify.pay.schedule;

import com.alibaba.fastjson.JSON;
import com.caychen.notify.pay.common.Constant;
import com.caychen.notify.pay.entity.PayRecord;
import com.caychen.notify.pay.message.CallbackData;
import com.caychen.notify.pay.message.CallbackDataProducer;
import com.caychen.notify.pay.service.IPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Caychen
 * @Date: 2021/8/8 17:07
 * @Description:
 */
@Slf4j
@Component
public class PayRecordScheduler {

    @Autowired
    private IPayService payService;

    @Autowired
    private CallbackDataProducer callbackDataProducer;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${server.port}")
    private int port;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void callback() {
        Boolean isGet = false;
        RLock lock = redissonClient.getLock(Constant.PAY_RECORD_RESULT_CALLBACK_LOCK_NAME);
        try {
            /**
             * watiTime: 等待锁的最大时间，超过时间不再尝试获取所。
             * leaseTime:如果没有手动调用unlock，超过时间自动释放
             * TimeUnit :释放时间单位。
             */
            //尝试加锁，最多等待0.5秒，上锁以后15秒自动解锁
            if (isGet = lock.tryLock(0, 15000, TimeUnit.MILLISECONDS)) {
                log.info("Redisson获取到分布式锁：[{}], 端口为: [{}]", Thread.currentThread().getName(), port);
                List<PayRecord> payRecords = payService.listFailPayRecord();
                if (CollectionUtils.isNotEmpty(payRecords)) {
                    payRecords.stream().forEach(payRecord -> {
                        log.info("回调参数：[{}]", JSON.toJSONString(payRecord));

                        CallbackData callbackData = new CallbackData();
                        callbackData.setPayResult(payRecord.getPayResult());
                        callbackData.setTxNo(payRecord.getTxNo());

                        if (StringUtils.isNotBlank(payRecord.getCallbackUrl())) {
                            //http接口
                            callbackDataProducer.callUrlInterface(payRecord, payRecord.getCallbackUrl(), callbackData);
                        } else {
                            //mq
                            callbackDataProducer.sendCallbackData(callbackData);
                        }
                    });
                }
                Thread.sleep(5000);
            } else {
                log.info("Redisson未获取到锁: [{}]", Thread.currentThread().getName());
            }
        } catch (Exception e) {

        } finally {
            if (isGet) {
                lock.unlock();
            }
        }
    }
}
