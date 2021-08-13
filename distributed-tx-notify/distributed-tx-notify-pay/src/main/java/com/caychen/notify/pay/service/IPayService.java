package com.caychen.notify.pay.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caychen.notify.pay.dto.DepositClientRequest;
import com.caychen.notify.pay.entity.PayRecord;

import java.util.List;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 11:22
 * @Description:
 */
public interface IPayService extends IService<PayRecord> {

    void doPay(DepositClientRequest depositClientRequest);

    void doNotifyMqResult(Boolean isOk, String txNo);

    List<PayRecord> listFailPayRecord();

    Boolean queryPayResult(String txNo);
}
