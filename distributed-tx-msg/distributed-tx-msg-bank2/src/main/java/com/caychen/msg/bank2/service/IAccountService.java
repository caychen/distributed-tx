package com.caychen.msg.bank2.service;

import com.caychen.msg.bank2.dto.TransferRequest;

/**
 * @Author: Caychen
 * @Date: 2021/7/31 21:33
 * @Description:
 */
public interface IAccountService {

    /**
     * 新增账户金额
     *
     * @param transferRequest
     */
    void addAccountInfoBalance(TransferRequest transferRequest) throws Exception;
}
