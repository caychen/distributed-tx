package com.caychen.notify.account.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 10:34
 * @Description:
 */
@Data
public class DepositRequest {

    private Long accountId;

    private BigDecimal money;

}
