package com.caychen.notify.account.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 13:52
 * @Description:
 */
@Data
@TableName("t_deposit_record")
public class DepositRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tx_no")
    private String txNo;

    @TableField("account_id")
    private Long accountId;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField("result")
    private String result;

}
