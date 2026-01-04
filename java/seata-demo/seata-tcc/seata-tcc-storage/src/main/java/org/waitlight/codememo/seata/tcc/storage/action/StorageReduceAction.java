package org.waitlight.codememo.seata.tcc.storage.action;

import io.seata.rm.tcc.TwoPhaseResult;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface StorageReduceAction {

    @TwoPhaseBusinessAction(name = "StorageReduceAction", commitMethod = "commit", rollbackMethod = "rollback")
    void reduce(BusinessActionContext actionContext, Long id, Integer amount);

    TwoPhaseResult commit(BusinessActionContext actionContext);

    TwoPhaseResult rollback(BusinessActionContext actionContext);

}
