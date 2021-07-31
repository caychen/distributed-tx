create database distributed-tx-msg-bank1 character set utf8mb4;

use distributed-tx-msg-bank1;

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
  `id` bigint(0) NOT NULL,
  `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL comment '账户名',
  `balance` decimal(5, 2) NOT NULL comment '账户余额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic comment='账户表';

INSERT INTO `t_account`(`id`, `account`, `balance`) VALUES (1, 'zhangsan', 100.00);

DROP TABLE IF EXISTS `tx_duplication`;
CREATE TABLE `tx_duplication`  (
   `tx_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '事务id',
   `create_time` datetime(0) NULL DEFAULT NULL,
   PRIMARY KEY (`tx_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '事务记录表（去重表），用于幂等控制' ROW_FORMAT = Dynamic;
