package com.caychen.notify.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 10:44
 * @Description:
 */
@Data
@TableName(value = "t_account")
public class Account {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String account;

    private BigDecimal balance;
}

