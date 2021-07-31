package com.caychen.msg.bank1.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caychen.msg.bank1.config.TopicProperties;
import com.caychen.msg.bank1.dao.IAccountMapper;
import com.caychen.msg.bank1.dao.ITxDuplicationMapper;
import com.caychen.msg.bank1.dto.TransferRequest;
import com.caychen.msg.bank1.entity.Account;
import com.caychen.msg.bank1.entity.TxDuplication;
import com.caychen.msg.bank1.service.ITransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
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
@EnableConfigurationProperties(TopicProperties.class)
public class TransferServiceImpl implements ITransferService {

    @Autowired
    private IAccountMapper accountMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private TopicProperties topicProperties;

    @Autowired
    private ITxDuplicationMapper txDuplicationMapper;

    /**
     * 转账前的准备，即发送mq消息
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    @Override
    public Boolean transfer(TransferRequest transferRequest) {
        //发送mq的事务消息
        Message message = MessageBuilder.withPayload(JSON.toJSONString(transferRequest)).build();

        /**
         * @param destination destination formats: `topicName:tags`
         * @param message message {@link org.springframework.messaging.Message}
         * @param arg ext arg
         *
         */
        String destination = topicProperties.getTopic();
        rocketMQTemplate.sendMessageInTransaction(destination, message, null);
        return Boolean.TRUE;
    }

    /**
     * 执行本地事务：更新账户，扣减金额
     *
     * @param transferRequest
     * @return
     * @throws Exception
     */
    @Override
    @Transactional
    public Boolean doTransfer(TransferRequest transferRequest) throws Exception {
        log.info("开始更新bank1的账户信息...");
        String txNo = transferRequest.getTxNo();
        //判断幂等性
        Integer count = txDuplicationMapper.selectCount(
                new LambdaQueryWrapper<TxDuplication>()
                        .eq(TxDuplication::getTxNo, txNo)
        );

        //如果count==0，则执行本地事务
        if (count == 0) {
            Account account = accountMapper.selectById(transferRequest.getFromId());
            BigDecimal balance = account.getBalance();

            //检查余额
            if (balance.compareTo(transferRequest.getMoney()) >= 0) {
                account.setBalance(balance.subtract(transferRequest.getMoney()));
                accountMapper.updateById(account);

                //保存事务id
                TxDuplication txDuplication = new TxDuplication();
                txDuplication.setTxNo(txNo);
                txDuplicationMapper.insert(txDuplication);

                log.info("完成bank1账户更新操作...");
                return Boolean.TRUE;
            } else {
                log.error("余额不足，无法转账");
                throw new Exception("余额不足，无法转账");
            }
        } else {
            //否则直接返回
            log.warn("无须操作...");
            return Boolean.FALSE;
        }
    }

}
