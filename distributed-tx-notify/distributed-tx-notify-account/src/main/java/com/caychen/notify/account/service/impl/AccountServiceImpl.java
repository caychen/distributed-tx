package com.caychen.notify.account.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caychen.notify.account.dao.IAccountMapper;
import com.caychen.notify.account.entity.Account;
import com.caychen.notify.account.service.IAccountService;
import org.springframework.stereotype.Service;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 23:24
 * @Description:
 */
@Service
public class AccountServiceImpl extends ServiceImpl<IAccountMapper, Account> implements IAccountService {
}
