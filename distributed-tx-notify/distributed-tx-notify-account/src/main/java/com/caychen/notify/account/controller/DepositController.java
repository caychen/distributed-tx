package com.caychen.notify.account.controller;

import com.caychen.notify.account.consumer.CallbackData;
import com.caychen.notify.account.dto.DepositRequest;
import com.caychen.notify.account.service.IDepositRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 10:32
 * @Description: 充值业务
 */
@RestController
@RequestMapping("/v1/deposit")
public class DepositController {

    @Autowired
    private IDepositRecordService depositRecordService;

    /**
     * 转账请求
     *
     * @param depositRequest
     * @return
     */
    @PostMapping
    public String deposit(@RequestBody @Validated DepositRequest depositRequest) {
        return depositRecordService.deposit(depositRequest);
    }

    /**
     * http接口回调
     *
     * @param callbackData
     * @return
     */
    @PostMapping("/result/callback")
    public Boolean resultCallback(@RequestBody CallbackData callbackData) throws Exception {
        return depositRecordService.dealDepositResult(callbackData);
    }
}
