create database distributed-tx-notify-pay character set utf8mb4;

use distributed-tx-notify-pay;

DROP TABLE IF EXISTS `pay_record`;
CREATE TABLE `pay_record` (
  `id` bigint NOT NULL,
  `account_id` bigint DEFAULT NULL COMMENT '账号id',
  `pay_amount` double DEFAULT NULL COMMENT '充值金额',
  `tx_no` varchar(20) DEFAULT NULL,
  `pay_result` tinyint(1) DEFAULT NULL COMMENT '充值结果:success，fail',
  `callback_result` tinyint(1) DEFAULT '0' COMMENT '回调结果：0-失败，1-成功',
  `callback_count` int DEFAULT '0' COMMENT '回调次数',
  `callback_url` varchar(200) DEFAULT NULL COMMENT '回调地址',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_tx_no` (`tx_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='支付记录';