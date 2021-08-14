package com.caychen.notify.account.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.caychen.notify.account.consumer.CallbackData;
import com.caychen.notify.account.dto.DepositRequest;
import com.caychen.notify.account.entity.DepositRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author: Caychen
 * @Date: 2021/8/1 12:39
 * @Description:
 */
public interface IDepositRecordService extends IService<DepositRecord> {

    /**
     * 充值
     *
     * @param depositRequest
     * @return
     */
    String deposit(DepositRequest depositRequest);

    Boolean dealDepositResult(CallbackData message) throws Exception;

    List<DepositRecord> listDepositRecordWithoutResult();

}
