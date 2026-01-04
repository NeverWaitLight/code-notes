-- -----------------------
-- at_wallet
-- -----------------------
DROP DATABASE IF EXISTS `at_wallet`;
CREATE DATABASE `at_wallet` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `at_wallet`.`t_balance`;
CREATE TABLE `at_wallet`.`t_balance`
(
    `id`       BIGINT      NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `balance`  DECIMAL(20, 2) DEFAULT '0.00',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `at_wallet`.`undo_log`
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
  DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
-- -----------------------
-- at_storage
-- -----------------------
DROP DATABASE IF EXISTS `at_storage`;
CREATE DATABASE `at_storage` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `at_storage`.`t_product`;
CREATE TABLE `at_storage`.`t_product`
(
    `id`    BIGINT      NOT NULL AUTO_INCREMENT,
    `name`  VARCHAR(50) NOT NULL,
    `stock` INT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `at_storage`.`undo_log`
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
  DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
-- -----------------------
-- at_order
-- -----------------------
DROP DATABASE IF EXISTS `at_order`;
CREATE DATABASE `at_order` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `at_order`.`t_order`;
CREATE TABLE `at_order`.`t_order`
(
    `id`         BIGINT         NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT         NOT NULL,
    `bill`       DECIMAL(20, 2) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `at_order`.`undo_log`
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
  DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
-- -----------------------
-- business
-- -----------------------
DROP DATABASE IF EXISTS `at_business`;
CREATE DATABASE `at_business` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `at_business`.`undo_log`
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
  DEFAULT CHARSET = utf8mb4 COMMENT ='AT transaction mode undo table';
-- -----------------------
-- init
-- -----------------------
INSERT INTO `at_wallet`.`t_balance` (id, username, balance)
VALUES (1, 'tom', 100.00);
INSERT INTO `at_storage`.`t_product` (id, name, stock)
VALUES (1, 'hat', 20);