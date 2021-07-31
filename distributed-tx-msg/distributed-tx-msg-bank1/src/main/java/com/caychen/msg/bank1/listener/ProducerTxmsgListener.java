package com.caychen.msg.bank1.listener;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caychen.msg.bank1.dao.ITxDuplicationMapper;
import com.caychen.msg.bank1.dto.TransferRequest;
import com.caychen.msg.bank1.entity.TxDuplication;
import com.caychen.msg.bank1.service.ITransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @Author: Caychen
 * @Date: 2021/6/30 16:14
 * @Description:
 */
@Slf4j
@Component
@RocketMQTransactionListener
public class ProducerTxmsgListener implements RocketMQLocalTransactionListener {

    @Autowired
    private ITransferService transferService;

    @Autowired
    private ITxDuplicationMapper txDuplicationMapper;

    /**
     * RocketMQ发送者发送事务消息之后的回调
     *
     * @param msg
     * @param arg
     * @return
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        log.info("开始事务消息回调");
        try {
            String messageString = new String((byte[]) msg.getPayload());
            TransferRequest transferRequest = JSON.parseObject(messageString, TransferRequest.class);
            log.info("消息回调txNo: [{}]", transferRequest.getTxNo());

            transferService.doTransfer(transferRequest);

            BigDecimal money = transferRequest.getMoney();
            if (money.compareTo(new BigDecimal("5")) == 0) {
                //提交commit
                //正常返回，则进行commit，自动向mq发送commit消息，mq消息的状态则变为可消费
                return RocketMQLocalTransactionState.COMMIT;
            } else if (money.compareTo(new BigDecimal("10")) == 0) {
                //提交unknown
                return RocketMQLocalTransactionState.UNKNOWN;
            } else if (money.compareTo(new BigDecimal("20")) == 0) {
                //提交rollback
                return RocketMQLocalTransactionState.ROLLBACK;
            } else {
                //其他提交commit
                return RocketMQLocalTransactionState.COMMIT;
            }

        } catch (Exception e) {
            log.error("发生异常：", e);
            //异常返回，进行rollback，自动向mq发送rollback消息，mq消息则被删除
            return RocketMQLocalTransactionState.ROLLBACK;
        }

    }

    /**
     * 事务回查
     *
     * @param msg
     * @return
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String messageString = new String((byte[]) msg.getPayload());
        TransferRequest transferRequest = JSON.parseObject(messageString, TransferRequest.class);
        log.info("回查txNo: [{}]", transferRequest.getTxNo());

        //判断幂等性
        Integer count = txDuplicationMapper.selectCount(
                new LambdaQueryWrapper<TxDuplication>()
                        .eq(TxDuplication::getTxNo, transferRequest.getTxNo())
        );
        if (count > 0) {
            //如果查询到，则发送commit
            return RocketMQLocalTransactionState.COMMIT;
        }

        //如果未查询到，则发送unknown
        return RocketMQLocalTransactionState.UNKNOWN;
    }
}
