package com.caychen.notify.account.feign;

import com.caychen.notify.account.dto.DepositClientRequest;
import com.caychen.notify.account.feign.fallback.PayAccountFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 10:48
 * @Description:
 */
@FeignClient(name = "distributed-tx-notify-pay", fallbackFactory = PayAccountFallbackFactory.class)
public interface IPayServiceClient {

    @PostMapping("/v1/pay/deposit")
    String payAccount(@RequestBody DepositClientRequest depositClientRequest);

    @PostMapping("/v1/pay/notify/callback")
    void notifyMqResult(@RequestParam Boolean isOk, @RequestParam String txNo);

    @GetMapping("/v1/pay/query/result")
    Boolean queryPayResult(@RequestParam String txNo);
}
