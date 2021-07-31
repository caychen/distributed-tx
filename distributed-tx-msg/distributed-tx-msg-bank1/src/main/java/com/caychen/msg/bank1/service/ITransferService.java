package com.caychen.msg.bank1.service;

import com.caychen.msg.bank1.dto.TransferRequest;

/**
 * @Author: Caychen
 * @Date: 2021/5/29 18:39
 * @Description:
 */
public interface ITransferService {

    /**
     * 转账前的准备，即发送mq消息
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    Boolean transfer(TransferRequest transferRequest) throws Exception;

    /**
     * 更新账户，扣减金额
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    Boolean doTransfer(TransferRequest transferRequest) throws Exception;
}
