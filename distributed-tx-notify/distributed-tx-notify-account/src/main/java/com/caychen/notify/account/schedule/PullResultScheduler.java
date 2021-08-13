package com.caychen.notify.account.schedule;

import com.alibaba.fastjson.JSON;
import com.caychen.notify.account.common.Constant;
import com.caychen.notify.account.entity.Account;
import com.caychen.notify.account.entity.DepositRecord;
import com.caychen.notify.account.feign.IPayServiceClient;
import com.caychen.notify.account.service.IAccountService;
import com.caychen.notify.account.service.IDepositRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Caychen
 * @Date: 2021/8/8 17:07
 * @Description:
 */
@Slf4j
@Component
public class PullResultScheduler {

    @Autowired
    private IDepositRecordService depositRecordService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IPayServiceClient payServiceClient;

    @Autowired
    private IAccountService accountService;

    @Value("${server.port}")
    private int port;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void pullResult() {
        Boolean isGet = false;
        RLock lock = redissonClient.getLock(Constant.QUERY_PAY_RESULT_LOCK_NAME);
        try {
            /**
             * watiTime: 等待锁的最大时间，超过时间不再尝试获取所。
             * leaseTime:如果没有手动调用unlock，超过时间自动释放
             * TimeUnit :释放时间单位。
             */
            //尝试加锁，最多等待0秒，上锁以后15秒自动解锁
            if (isGet = lock.tryLock(0, 15000, TimeUnit.MILLISECONDS)) {
                log.info("Redisson获取到分布式锁：[{}], 端口为: [{}]", Thread.currentThread().getName(), port);
                List<DepositRecord> depositRecordList = depositRecordService.listDepositRecordWithoutResult();
                if (CollectionUtils.isNotEmpty(depositRecordList)) {
                    depositRecordList.stream().forEach(depositRecord -> {
                        log.info("查询参数：[{}]", JSON.toJSONString(depositRecord));

                        String txNo = depositRecord.getTxNo();
                        Boolean result = payServiceClient.queryPayResult(txNo);

                        if(result == null){
                            return;
                        }

                        Long accountId = depositRecord.getAccountId();
                        BigDecimal payAmount = depositRecord.getPayAmount();

                        Account account = accountService.getById(accountId);
                        account.setBalance(account.getBalance().add(payAmount));
                        accountService.updateById(account);

                    });
                }
                Thread.sleep(5000);
            } else {
                log.info("Redisson未获取到锁: [{}]", Thread.currentThread().getName());
            }
        } catch (Exception e) {

        } finally {
            if (isGet) {
                lock.unlock();
            }
        }
    }
}
