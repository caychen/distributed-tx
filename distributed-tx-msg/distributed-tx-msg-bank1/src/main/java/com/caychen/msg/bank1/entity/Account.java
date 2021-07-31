package com.caychen.msg.bank1.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: Caychen
 * @Date: 2021/5/30 11:06
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
