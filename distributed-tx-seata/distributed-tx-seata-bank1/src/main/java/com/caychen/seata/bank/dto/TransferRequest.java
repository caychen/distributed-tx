package com.caychen.seata.bank.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @Author: Caychen
 * @Date: 2021/5/29 18:34
 * @Description:
 */
@Data
public class TransferRequest {

    @NotNull
    private Long fromId;

    @NotNull
    private Long toId;

    private String account;

    @NotNull
    private BigDecimal money;
}
