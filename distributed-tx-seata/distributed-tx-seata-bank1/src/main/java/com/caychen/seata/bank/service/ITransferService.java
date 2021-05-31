package com.caychen.seata.bank.service;

import com.caychen.seata.bank.dto.TransferRequest;

/**
 * @Author: Caychen
 * @Date: 2021/5/29 18:39
 * @Description:
 */
public interface ITransferService {

    Boolean transfer(TransferRequest transferRequest) throws Exception;
}
