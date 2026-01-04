package org.waitlight.codememo.seata.tcc.wallet.action.impl;

import io.seata.rm.tcc.TwoPhaseResult;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.waitlight.codememo.seata.tcc.wallet.action.BalancePayAction;
import org.waitlight.codememo.seata.tcc.wallet.model.Balance;
import org.waitlight.codememo.seata.tcc.wallet.model.WalletFreezeLog;
import org.waitlight.codememo.seata.tcc.wallet.repository.BalanceFreezeLogRepository;
import org.waitlight.codememo.seata.tcc.wallet.repository.BalanceRepository;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalancePayActionImpl implements BalancePayAction {
    private final BalanceRepository balanceRepository;
    private final BalanceFreezeLogRepository balanceFreezeLogRepository;

    @Override
    public Balance pay(BusinessActionContext actionContext, String username, BigDecimal amount) {
        Balance balance = balanceRepository.findByUsername(username).map(b -> {
            b.setBalance(b.getBalance().subtract(amount));
            b.setFreeze(b.getFreeze().add(amount));
            return b;
        }).orElseThrow(() -> new RuntimeException("用户不存在"));

        WalletFreezeLog walletFreezeLog = new WalletFreezeLog();
        walletFreezeLog.setXid(actionContext.getXid());
        walletFreezeLog.setBalanceId(balance.getId());
        walletFreezeLog.setAmount(amount);
        walletFreezeLog.setCreateTime(new Date());
        balanceFreezeLogRepository.save(walletFreezeLog);

        return balanceRepository.save(balance);
    }

    @Override
    public TwoPhaseResult commit(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        WalletFreezeLog l = balanceFreezeLogRepository.findByXid(xid).orElseThrow(() -> new RuntimeException("Not found freeze log"));
        Long balanceId = l.getBalanceId();
        Balance balance = balanceRepository.findById(balanceId).orElseThrow(() -> new RuntimeException("Not found balance"));
        balance.setFreeze(balance.getFreeze().subtract(l.getAmount()));
        balanceRepository.save(balance);
        return new TwoPhaseResult(true, "commit success");
    }

    @Override
    public TwoPhaseResult rollback(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        WalletFreezeLog l = balanceFreezeLogRepository.findByXid(xid).orElseThrow(() -> new RuntimeException("Not found freeze log"));
        Long balanceId = l.getBalanceId();
        Balance balance = balanceRepository.findById(balanceId).orElseThrow(() -> new RuntimeException("Not found balance"));
        balance.setBalance(balance.getBalance().add(l.getAmount()));
        balance.setFreeze(balance.getFreeze().subtract(l.getAmount()));
        balanceRepository.save(balance);
        balanceFreezeLogRepository.deleteById(l.getId());
        return new TwoPhaseResult(true, "rollback success");
    }
}
