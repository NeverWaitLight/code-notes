-- -----------------------
-- tcc_wallet
-- -----------------------
DROP DATABASE IF EXISTS `tcc_wallet`;
CREATE DATABASE `tcc_wallet` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `tcc_wallet`.`t_balance`;
CREATE TABLE `tcc_wallet`.`t_balance`
(
    `id`       BIGINT      NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `balance`  DECIMAL(20, 2) DEFAULT '0.00',
    `freeze`   DECIMAL(20, 2) DEFAULT '0.00',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
DROP TABLE IF EXISTS `tcc_wallet`.`t_wallet_freeze_log`;
CREATE TABLE `tcc_wallet`.`t_wallet_freeze_log`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `xid`         VARCHAR(255) NOT NULL,
    `balance_id`  BIGINT       NOT NULL,
    `amount`      DECIMAL(20, 2) DEFAULT '0.00',
    `create_time` DATETIME       DEFAULT NOW(),
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- -----------------------
-- tcc_storage
-- -----------------------
DROP DATABASE IF EXISTS `tcc_storage`;
CREATE DATABASE `tcc_storage` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `tcc_storage`.`t_product`;
CREATE TABLE `tcc_storage`.`t_product`
(
    `id`     BIGINT      NOT NULL AUTO_INCREMENT,
    `name`   VARCHAR(50) NOT NULL,
    `stock`  INT DEFAULT 0,
    `freeze` INT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
DROP TABLE IF EXISTS `tcc_storage`.`t_product_freeze_log`;
CREATE TABLE `tcc_storage`.`t_product_freeze_log`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `xid`         VARCHAR(255) NOT NULL,
    `product_id`  BIGINT       NOT NULL,
    `amount`      INT      DEFAULT 0,
    `create_time` DATETIME DEFAULT NOW(),
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- -----------------------
-- tcc_order
-- -----------------------
DROP DATABASE IF EXISTS `tcc_order`;
CREATE DATABASE `tcc_order` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `tcc_order`.`t_order`;
CREATE TABLE `tcc_order`.`t_order`
(
    `id`         BIGINT         NOT NULL AUTO_INCREMENT,
    `xid`        VARCHAR(100)   NOT NULL,
    `product_id` BIGINT         NOT NULL,
    `bill`       DECIMAL(20, 2) NOT NULL,
    `status`     VARCHAR(10)    NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- -----------------------
-- business
-- -----------------------
DROP DATABASE IF EXISTS `tcc_business`;
CREATE DATABASE `tcc_business` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- -----------------------
-- init
-- -----------------------
INSERT INTO `tcc_wallet`.`t_balance` (id, username, balance)
VALUES (1, 'tom', 100.00);
INSERT INTO `tcc_storage`.`t_product` (id, name, stock)
VALUES (1, 'hat', 20);