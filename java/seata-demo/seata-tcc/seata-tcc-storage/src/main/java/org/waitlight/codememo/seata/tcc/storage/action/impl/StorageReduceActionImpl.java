package org.waitlight.codememo.seata.tcc.storage.action.impl;

import io.seata.rm.tcc.TwoPhaseResult;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.waitlight.codememo.seata.tcc.storage.action.StorageReduceAction;
import org.waitlight.codememo.seata.tcc.storage.model.Product;
import org.waitlight.codememo.seata.tcc.storage.model.ProductFreezeLog;
import org.waitlight.codememo.seata.tcc.storage.repository.ProductFreezeLogRepository;
import org.waitlight.codememo.seata.tcc.storage.repository.ProductRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageReduceActionImpl implements StorageReduceAction {

    private final ProductRepository productRepository;
    private final ProductFreezeLogRepository freezeLogRepository;

    @Override
    public void reduce(BusinessActionContext actionContext, Long id, Integer amount) {
        productRepository.findById(id).map(p -> {
                    p.setStock(p.getStock() - amount);
                    p.setFreeze(p.getFreeze() + amount);
                    return p;
                })
                .ifPresent(p -> {
                    productRepository.save(p);
                    ProductFreezeLog l = new ProductFreezeLog();
                    l.setXid(actionContext.getXid());
                    l.setProductId(p.getId());
                    l.setAmount(amount);
                    freezeLogRepository.save(l);
                });
    }

    @Override
    public TwoPhaseResult commit(BusinessActionContext actionContext) {
        freezeLogRepository.findByXid(actionContext.getXid()).ifPresent(l -> {
            Product p = productRepository.findById(l.getProductId()).orElseThrow(() -> new RuntimeException("Not found product"));
            p.setFreeze(p.getFreeze() - l.getAmount());
            productRepository.save(p);
        });
        return new TwoPhaseResult(true, "commit success");
    }

    @Override
    public TwoPhaseResult rollback(BusinessActionContext actionContext) {
        freezeLogRepository.findByXid(actionContext.getXid()).ifPresent(l -> {
            Product p = productRepository.findById(l.getProductId()).orElseThrow(() -> new RuntimeException("Not found product"));
            p.setFreeze(p.getFreeze() - l.getAmount());
            p.setStock(p.getStock() + l.getAmount());
            productRepository.save(p);
            freezeLogRepository.deleteById(l.getId());
        });
        return new TwoPhaseResult(true, "rollback success");
    }
}
