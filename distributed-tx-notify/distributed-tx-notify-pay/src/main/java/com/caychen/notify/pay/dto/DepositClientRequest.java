package com.caychen.notify.pay.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 10:34
 * @Description:
 */
@Data
public class DepositClientRequest {

    @NotNull
    private Long accountId;

    @NotNull
    private BigDecimal money;

    /**
     * 如果callbackUrl为空，则不使用接口回调；如果callbackUrl不为空，同时使用接口回调和mq回调
     */
    private String callbackUrl;

    @NotBlank
    private String txNo;

}
