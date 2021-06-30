package com.caychen.seata.bank.service.impl;

import com.caychen.seata.bank.dao.IAccountMapper;
import com.caychen.seata.bank.dto.TransferRequest;
import com.caychen.seata.bank.entity.Account;
import com.caychen.seata.bank.feign.Bank2FeignClient;
import com.caychen.seata.bank.service.ITransferService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * @Author: Caychen
 * @Date: 2021/5/29 18:39
 * @Description:
 */
@Slf4j
@Service
public class TransferServiceImpl implements ITransferService {

    @Autowired
    private IAccountMapper accountMapper;

    @Autowired
    private Bank2FeignClient bank2FeignClient;

    @Override
    @Transactional
    @GlobalTransactional(rollbackFor = Exception.class)
    public Boolean transfer(TransferRequest transferRequest) throws Exception {
        String xid = RootContext.getXID();
        log.info("全局事务ID: [{}]", xid);

        BigDecimal transferRequestMoney = transferRequest.getMoney();

        Account account = accountMapper.selectById(transferRequest.getFromId());

        if (account.getBalance().compareTo(transferRequestMoney) == -1) {
            throw new Exception("余额不足, 无法转账");
        }

        //计算余额
        BigDecimal newBalance = account.getBalance().subtract(transferRequestMoney);
        account.setBalance(newBalance);
        int affectCount = accountMapper.updateById(account);
        Boolean isOk = affectCount == 1 ? true : false;

        //远程调用
        isOk = isOk && bank2FeignClient.transferMoney(transferRequest);

        //人为制造异常
        if (transferRequestMoney.compareTo(new BigDecimal("4")) == 0) {
            throw new Exception("人为制造异常");
        }

        return isOk;
    }
}
