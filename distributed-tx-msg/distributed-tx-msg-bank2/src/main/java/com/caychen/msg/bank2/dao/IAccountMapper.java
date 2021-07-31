package com.caychen.msg.bank2.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caychen.msg.bank2.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: Caychen
 * @Date: 2021/5/30 11:25
 * @Description:
 */
@Mapper
public interface IAccountMapper extends BaseMapper<Account> {
}
