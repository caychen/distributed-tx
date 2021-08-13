package com.caychen.notify.pay.controller;

import com.caychen.notify.pay.dto.DepositClientRequest;
import com.caychen.notify.pay.service.IPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 11:12
 * @Description:
 */
@Slf4j
@RestController
@RequestMapping("/v1/pay")
public class PayController {

    @Autowired
    private IPayService payService;

    /**
     * 处理支付请求
     *
     * @param depositClientRequest
     * @return
     */
    @PostMapping("/deposit")
    public String payAccount(@RequestBody @Validated DepositClientRequest depositClientRequest) {
        log.info("接收到支付请求...");

        new Thread(() -> {
            payService.doPay(depositClientRequest);
        }, "async-thread").start();

        return "success";
    }

    /**
     * mq消息处理后的结果通知
     *
     * @param isOk
     * @param txNo
     */
    @PostMapping("/notify/callback")
    public void notifyMqResult(@RequestParam Boolean isOk, @RequestParam String txNo) {
        payService.doNotifyMqResult(isOk, txNo);
    }

    /**
     * 查询事务处理结果
     *
     * @param txNo
     */
    @PostMapping("/query/result")
    public Boolean queryPayResult(@RequestParam String txNo) {
        return payService.queryPayResult(txNo);
    }
}
