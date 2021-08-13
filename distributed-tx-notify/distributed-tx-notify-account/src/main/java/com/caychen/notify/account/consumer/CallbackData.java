package com.caychen.notify.account.consumer;

import lombok.Data;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 20:37
 * @Description:
 */
@Data
public class CallbackData {

    private String txNo;

    private Boolean payResult;

}
