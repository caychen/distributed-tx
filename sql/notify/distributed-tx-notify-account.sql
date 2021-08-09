create database distributed-tx-notify-account character set utf8mb4;

use distributed-tx-notify-account;

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
  `id` bigint(0) NOT NULL,
  `account` varchar(255) NOT NULL comment '账户名',
  `balance` decimal(5, 2) NOT NULL comment '账户余额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB  comment='账户表';

INSERT INTO `t_account`(`id`, `account`, `balance`) VALUES (1, 'zhangsan', 100.00);

DROP TABLE IF EXISTS `tx_duplication`;
CREATE TABLE `tx_duplication`  (
   `tx_no` varchar(64)  NOT NULL COMMENT '事务id',
   `create_time` datetime(0) NULL DEFAULT NULL,
   PRIMARY KEY (`tx_no`) USING BTREE
) ENGINE = InnoDB  COMMENT = '事务记录表（去重表），用于幂等控制' ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `t_deposit_record`;
CREATE TABLE `t_deposit_record` (
    `id` bigint NOT NULL,
    `tx_no` varchar(20) NOT NULL COMMENT '事务号',
    `account_id` bigint NOT NULL COMMENT '账户id',
    `pay_amount` double DEFAULT NULL COMMENT '充值金额',
    `result` varchar(10)  DEFAULT NULL COMMENT '充值结果:success，fail',
    `create_time` datetime DEFAULT NULL,
    `update_time` datetime DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_tx_no` (`tx_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='充值记录';