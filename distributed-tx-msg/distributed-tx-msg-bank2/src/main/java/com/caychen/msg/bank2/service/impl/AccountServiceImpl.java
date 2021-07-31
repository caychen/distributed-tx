package com.caychen.msg.bank2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caychen.msg.bank2.dao.IAccountMapper;
import com.caychen.msg.bank2.dao.ITxDuplicationMapper;
import com.caychen.msg.bank2.dto.TransferRequest;
import com.caychen.msg.bank2.entity.Account;
import com.caychen.msg.bank2.entity.TxDuplication;
import com.caychen.msg.bank2.service.IAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: Caychen
 * @Date: 2021/7/31 21:34
 * @Description:
 */
@Slf4j
@Service
public class AccountServiceImpl implements IAccountService {

    @Autowired
    private IAccountMapper accountMapper;

    @Autowired
    private ITxDuplicationMapper txDuplicationMapper;

    /**
     * 增加账户金额
     *
     * @param transferRequest
     * @throws Exception
     */
    @Override
    @Transactional
    public void addAccountInfoBalance(TransferRequest transferRequest) throws Exception {
        log.info("开始更新bank2的账户信息...");
        String txNo = transferRequest.getTxNo();

        //判断幂等性
        Integer count = txDuplicationMapper.selectCount(
                new LambdaQueryWrapper<TxDuplication>()
                        .eq(TxDuplication::getTxNo, txNo)
        );

        if (count > 0) {
            log.warn("无须操作...");
            return;
        }

        Account account = accountMapper.selectById(transferRequest.getToId());
        if (account == null) {
            log.error("账户信息不存在...");
            throw new Exception("账户信息不存在...");
        }

        account.setBalance(account.getBalance().add(transferRequest.getMoney()));
        accountMapper.updateById(account);

        TxDuplication txDuplication = new TxDuplication();
        txDuplication.setTxNo(txNo);
        txDuplicationMapper.insert(txDuplication);
        log.info("完成bank2账户更新操作...");
    }
}
