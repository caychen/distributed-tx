package com.caychen.msg.bank1.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author: Caychen
 * @Date: 2021/7/31 18:16
 * @Description:
 */
@Data
@ConfigurationProperties("bank1.producer")
public class TopicProperties {

    private String topic;

    private String tag;
}
