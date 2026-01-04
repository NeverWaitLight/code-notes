package org.waitlight.codememo.seata.tcc.order.action;

import io.seata.rm.tcc.TwoPhaseResult;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.example.xa.commons.Ordering;

@LocalTCC
public interface OrderingAction {
    @TwoPhaseBusinessAction(name = "OrderingAction", commitMethod = "commit", rollbackMethod = "rollback")
    void ordering(BusinessActionContext actionContext, Ordering ordering);

    TwoPhaseResult commit(BusinessActionContext actionContext);

    TwoPhaseResult rollback(BusinessActionContext actionContext);
}
