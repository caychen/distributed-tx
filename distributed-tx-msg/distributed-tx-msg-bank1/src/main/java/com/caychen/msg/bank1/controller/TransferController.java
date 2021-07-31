package com.caychen.msg.bank1.controller;

import com.caychen.msg.bank1.dto.TransferRequest;
import com.caychen.msg.bank1.service.ITransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @Author: Caychen
 * @Date: 2021/5/29 18:29
 * @Description:
 */
@RestController
@RequestMapping("/v1/bank1/transfer")
public class TransferController {

    @Autowired
    private ITransferService transferService;

    @PostMapping
    public String transferMoney(@RequestBody @Validated TransferRequest transferRequest) throws Exception {
        transferRequest.setTxNo(UUID.randomUUID().toString().replace("-", ""));
        Boolean transferFlag = transferService.transfer(transferRequest);
        return transferFlag ? "success" : "fail";
    }
}
