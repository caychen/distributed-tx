package com.caychen.msg.bank2.dto;

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

    @NotNull
    private BigDecimal money;

    private String txNo;
}
