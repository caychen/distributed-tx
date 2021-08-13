package com.caychen.notify.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caychen.notify.pay.common.Constant;
import com.caychen.notify.pay.dao.IPayRecordMapper;
import com.caychen.notify.pay.dto.DepositClientRequest;
import com.caychen.notify.pay.entity.PayRecord;
import com.caychen.notify.pay.message.CallbackData;
import com.caychen.notify.pay.message.CallbackDataProducer;
import com.caychen.notify.pay.service.IPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 11:23
 * @Description:
 */
@Slf4j
@Service
public class PayServiceImpl extends ServiceImpl<IPayRecordMapper, PayRecord> implements IPayService {

    @Autowired
    private IPayRecordMapper payRecordMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CallbackDataProducer callbackDataProducer;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doPay(DepositClientRequest depositClientRequest) {
        try {
            //模拟10秒之后操作完成
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.error("捕获异常:", e);
        }

        //支付记录
        PayRecord payRecord = new PayRecord();
        payRecord.setAccountId(depositClientRequest.getAccountId());
        payRecord.setPayAmount(depositClientRequest.getMoney());
        payRecord.setTxNo(depositClientRequest.getTxNo());
        payRecord.setPayResult(Boolean.TRUE);
        String callbackUrl = depositClientRequest.getCallbackUrl();
        payRecord.setCallbackUrl(callbackUrl);

        int count = payRecordMapper.insert(payRecord);

        if (count > 0) {
            CallbackData callbackData = new CallbackData();
            callbackData.setPayResult(Boolean.TRUE);
            callbackData.setTxNo(depositClientRequest.getTxNo());

            if (StringUtils.isNotBlank(callbackUrl)) {
                log.info("回调地址callbackUrl: [{}]", callbackUrl);
                callbackDataProducer.callUrlInterface(payRecord, callbackUrl, callbackData);
            } else {
                callbackDataProducer.sendCallbackData(callbackData);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doNotifyMqResult(Boolean isOk, String txNo) {
        PayRecord payRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<PayRecord>().eq(PayRecord::getTxNo, txNo));
        payRecord.setCallbackResult(isOk);
        if (!isOk) {
            payRecord.setCallbackCount(payRecord.getCallbackCount() + 1);
        }
        payRecordMapper.updateById(payRecord);
    }

    @Override
    public List<PayRecord> listFailPayRecord() {
        List<PayRecord> payRecords = payRecordMapper.selectList(
                new LambdaQueryWrapper<PayRecord>()
                        .eq(PayRecord::getCallbackResult, false)
                        .lt(PayRecord::getCallbackCount, Constant.MAX_COUNT)
        );
        return payRecords;
    }

    @Override
    public Boolean queryPayResult(String txNo) {
        PayRecord payRecord = payRecordMapper.selectOne(
                new LambdaQueryWrapper<PayRecord>()
                        .eq(PayRecord::getTxNo, txNo)
        );
        return payRecord.getPayResult();
    }
}
