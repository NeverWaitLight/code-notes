package org.waitlight.codememo.seata.tcc.wallet.model;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name = "t_wallet_freeze_log")
public class WalletFreezeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String xid;
    private Long balanceId;
    private BigDecimal amount;
    private Date createTime;
}
