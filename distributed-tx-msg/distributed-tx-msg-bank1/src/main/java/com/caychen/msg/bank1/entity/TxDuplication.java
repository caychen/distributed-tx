package com.caychen.msg.bank1.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * @Author: Caychen
 * @Date: 2021/6/30 12:48
 * @Description:
 */
@Data
@TableName(value = "tx_duplication")
public class TxDuplication {

    @TableId(type = IdType.INPUT)
    @TableField("tx_no")
    private String txNo;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

}
