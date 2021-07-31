create database distributed-tx-seata-bank2 character set utf8mb4;

use distributed-tx-seata-bank2;

-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
    ) ENGINE = InnoDB
    AUTO_INCREMENT = 1
    DEFAULT CHARSET = utf8 COMMENT ='AT transaction mode undo table';

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account`  (
  `id` bigint(0) NOT NULL,
  `account` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL comment '账户名',
  `balance` decimal(5, 2) NOT NULL comment '账户余额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic comment='账户表';

INSERT INTO `t_account`(`id`, `account`, `balance`) VALUES (2, 'lisi', 0.00);
