package org.waitlight.codememo.seata.tcc.wallet.repository;

import org.springframework.data.repository.CrudRepository;
import org.waitlight.codememo.seata.tcc.wallet.model.WalletFreezeLog;

import java.util.Optional;

public interface BalanceFreezeLogRepository extends CrudRepository<WalletFreezeLog, Long> {
    Optional<WalletFreezeLog> findByXid(String xid);
}
