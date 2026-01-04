package org.waitlight.codememo.seata.tcc.wallet.action;

import io.seata.rm.tcc.TwoPhaseResult;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.waitlight.codememo.seata.tcc.wallet.model.Balance;

import java.math.BigDecimal;

@LocalTCC
public interface BalancePayAction {
    @TwoPhaseBusinessAction(name = "BalancePayAction", commitMethod = "commit", rollbackMethod = "rollback")
    Balance pay(BusinessActionContext actionContext, String username, BigDecimal amount);

    TwoPhaseResult commit(BusinessActionContext actionContext);

    TwoPhaseResult rollback(BusinessActionContext actionContext);
}