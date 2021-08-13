package com.caychen.notify.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caychen.notify.account.consumer.CallbackData;
import com.caychen.notify.account.dao.IAccountMapper;
import com.caychen.notify.account.dao.IDepositRecordMapper;
import com.caychen.notify.account.dto.DepositClientRequest;
import com.caychen.notify.account.dto.DepositRequest;
import com.caychen.notify.account.entity.Account;
import com.caychen.notify.account.entity.DepositRecord;
import com.caychen.notify.account.feign.IPayServiceClient;
import com.caychen.notify.account.service.IDepositRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 10:39
 * @Description:
 */
@Service
@Slf4j
public class DepositRecordServiceImpl extends ServiceImpl<IDepositRecordMapper, DepositRecord> implements IDepositRecordService {

    private static final String INTERFACE_CALLBACK_URL = "/v1/deposit/result/callback";

    @Autowired
    private IDepositRecordMapper depositRecordMapper;

    @Autowired
    private IAccountMapper accountMapper;

    @Autowired
    private IPayServiceClient payServiceClient;

    @Value("${server.protocol:http}")
    private String protocol;

    @Value("${server.port:8080}")
    private Integer port;

    @Value("${server.host:localhost}")
    private String host;

    /**
     * 充值
     *
     * @param depositRequest
     * @return
     */
    @Override
    @Transactional
    public String deposit(DepositRequest depositRequest) {
        DepositClientRequest depositClientRequest = new DepositClientRequest();
        BeanUtils.copyProperties(depositRequest, depositClientRequest);
        String callbackUrl = protocol + "://" + host + ":" + port + INTERFACE_CALLBACK_URL;
        log.info("回调地址：[{}]", callbackUrl);

        if (depositRequest.getMoney().compareTo(new BigDecimal("10")) == 0) {
            //回调地址，可为空，也可不为空
            depositClientRequest.setCallbackUrl(callbackUrl);
        }

        String txNo = RandomStringUtils.random(10, true, true);
        depositClientRequest.setTxNo(txNo);

        String result = payServiceClient.payAccount(depositClientRequest);
        if (StringUtils.equals(result, "success")) {
            DepositRecord depositRecord = new DepositRecord();
            depositRecord.setTxNo(txNo);
            depositRecord.setAccountId(depositRequest.getAccountId());
            depositRecord.setPayAmount(depositRequest.getMoney());

            depositRecordMapper.insert(depositRecord);

        }
        return result;
    }

    @Override
    @Transactional
    public Boolean dealDepositResult(CallbackData callbackData) throws Exception {
        String txNo = callbackData.getTxNo();

        DepositRecord depositRecord = depositRecordMapper.selectOne(
                new LambdaQueryWrapper<DepositRecord>()
                        .eq(DepositRecord::getTxNo, txNo));

        //如果有值，则直接返回true，表示已收到结果
        if (StringUtils.isNotBlank(depositRecord.getResult())) {
            return Boolean.TRUE;
        }

        //模拟转账5元，返回false
        if (depositRecord.getPayAmount().compareTo(new BigDecimal("5")) == 0) {
            log.error("模拟失败场景");
            return Boolean.FALSE;
        } else if (depositRecord.getPayAmount().compareTo(new BigDecimal("50")) == 0) {
            throw new Exception("人造异常");
        }

        if (depositRecord != null) {
            //更新转账记录
            depositRecord.setResult(callbackData.getPayResult() ? "success" : "fail");
            depositRecordMapper.updateById(depositRecord);

            if (callbackData.getPayResult()) {
                //更新账户余额
                Long accountId = depositRecord.getAccountId();
                BigDecimal payAmount = depositRecord.getPayAmount();

                Account account = accountMapper.selectById(accountId);
                account.setBalance(account.getBalance().add(payAmount));
                accountMapper.updateById(account);
            }

            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public List<DepositRecord> listDepositRecordWithoutResult() {
        return depositRecordMapper.selectList(new LambdaQueryWrapper<DepositRecord>().isNull(DepositRecord::getResult));
    }

}
