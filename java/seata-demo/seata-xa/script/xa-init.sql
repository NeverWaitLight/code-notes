-- -----------------------
-- xa_wallet
-- -----------------------
DROP DATABASE IF EXISTS `xa_wallet`;
CREATE DATABASE `xa_wallet` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `xa_wallet`.`t_balance`;
CREATE TABLE `xa_wallet`.`t_balance`
(
    `id`       BIGINT      NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `balance`  DECIMAL(20, 2) DEFAULT '0.00',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- -----------------------
-- xa_storage
-- -----------------------
DROP DATABASE IF EXISTS `xa_storage`;
CREATE DATABASE `xa_storage` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `xa_storage`.`t_product`;
CREATE TABLE `xa_storage`.`t_product`
(
    `id`    BIGINT      NOT NULL AUTO_INCREMENT,
    `name`  VARCHAR(50) NOT NULL,
    `stock` INT DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- -----------------------
-- xa_order
-- -----------------------
DROP DATABASE IF EXISTS `xa_order`;
CREATE DATABASE `xa_order` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
DROP TABLE IF EXISTS `xa_order`.`t_order`;
CREATE TABLE `xa_order`.`t_order`
(
    `id`         BIGINT         NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT         NOT NULL,
    `bill`       DECIMAL(20, 2) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;
-- -----------------------
-- business
-- -----------------------
DROP DATABASE IF EXISTS `xa_business`;
CREATE DATABASE `xa_business` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- -----------------------
-- init
-- -----------------------
INSERT INTO `xa_wallet`.`t_balance` (id, username, balance)
VALUES (1, 'tom', 100.00);
INSERT INTO `xa_storage`.`t_product` (id, name, stock)
VALUES (1, 'hat', 20);