package com.caychen.seata.bank.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caychen.seata.bank.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: Caychen
 * @Date: 2021/5/30 11:25
 * @Description:
 */
@Mapper
public interface IAccountMapper extends BaseMapper<Account> {
}
