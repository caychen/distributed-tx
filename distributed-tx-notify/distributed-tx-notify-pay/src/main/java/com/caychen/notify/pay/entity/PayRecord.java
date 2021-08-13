package com.caychen.notify.pay.entity;

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
 * @Date: 2021/8/7 11:15
 * @Description:
 */
@Data
@TableName("pay_record")
public class PayRecord {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("account_id")
    private Long accountId;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField(value = "tx_no")
    private String txNo;

    @TableField("pay_result")
    private Boolean payResult;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "callback_count")
    private Integer callbackCount;

    @TableField(value = "callback_result")
    private Boolean callbackResult;

    @TableField("callback_url")
    private String callbackUrl;

}
