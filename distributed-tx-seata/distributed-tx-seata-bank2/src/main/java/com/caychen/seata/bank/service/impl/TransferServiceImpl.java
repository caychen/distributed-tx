package com.caychen.seata.bank.service.impl;

import com.caychen.seata.bank.dao.IAccountMapper;
import com.caychen.seata.bank.dto.TransferRequest;
import com.caychen.seata.bank.entity.Account;
import com.caychen.seata.bank.service.ITransferService;
import io.seata.core.context.RootContext;
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
@Service
@Slf4j
public class TransferServiceImpl implements ITransferService {

    @Autowired
    private IAccountMapper accountMapper;

    @Override
    @Transactional
    public Boolean transfer(TransferRequest transferRequest) {
        String xid = RootContext.getXID();
        log.info("全局事务ID: [{}]", xid);

        Account account = accountMapper.selectById(transferRequest.getToId());

        BigDecimal newBalance = account.getBalance().add(transferRequest.getMoney());
        account.setBalance(newBalance);
        int affectCount = accountMapper.updateById(account);
        Boolean isOk = affectCount == 1 ? true : false;
        return isOk;
    }
}
