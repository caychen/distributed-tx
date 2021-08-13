package com.caychen.notify.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author: Caychen
 * @Date: 2021/8/7 13:39
 * @Description:
 */
@Data
@ConfigurationProperties(prefix = "httpclient")
public class HttpClientProperties {

    /**
     * 请求获取数据的超时时间，单位毫秒
     */
    private int socketTimeout;

    /**
     * 设置连接超时时间，单位毫秒
     */
    private int connectTimeout;

    /**
     * #http clilent中从connetcion pool中获得一个connection的超时时间,单位毫秒
     */
    private int connectionRequestTimeout;

}
