package com.caychen.seata.bank.feign;

import com.caychen.seata.bank.dto.TransferRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: Caychen
 * @Date: 2021/5/30 13:11
 * @Description:
 */
@FeignClient(name = "distributed-tx-seata-bank2")
public interface Bank2FeignClient {

    @PostMapping("/v1/bank2/transfer")
    Boolean transferMoney(@RequestBody TransferRequest transferRequest);
}
