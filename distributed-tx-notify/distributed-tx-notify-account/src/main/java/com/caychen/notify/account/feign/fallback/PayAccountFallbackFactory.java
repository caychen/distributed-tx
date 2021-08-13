package com.caychen.notify.account.feign.fallback;

import com.caychen.notify.account.dto.DepositClientRequest;
import com.caychen.notify.account.feign.IPayServiceClient;
import feign.FeignException;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 12:08
 * @Description:
 */
@Component
public class PayAccountFallbackFactory implements FallbackFactory<IPayServiceClient> {
    /**
     * Returns an instance of the fallback appropriate for the given cause
     *
     * @param cause corresponds to {@link AbstractCommand#getExecutionException()}
     *              often, but not always an instance of {@link FeignException}.
     */
    @Override
    public IPayServiceClient create(Throwable cause) {
        return new IPayServiceClient() {
            @Override
            public String payAccount(DepositClientRequest depositClientRequest) {
                return "fail";
            }

            @Override
            public void notifyMqResult(Boolean isOk, String txNo) {
                return;
            }

            @Override
            public Boolean queryPayResult(String txNo) {
                return null;
            }
        };
    }
}
