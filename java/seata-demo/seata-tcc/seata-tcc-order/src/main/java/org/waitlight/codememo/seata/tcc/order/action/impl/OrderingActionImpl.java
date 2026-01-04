package org.waitlight.codememo.seata.tcc.order.action.impl;

import io.seata.rm.tcc.TwoPhaseResult;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.xa.commons.Ordering;
import org.springframework.stereotype.Component;
import org.waitlight.codememo.seata.tcc.order.action.OrderingAction;
import org.waitlight.codememo.seata.tcc.order.model.Order;
import org.waitlight.codememo.seata.tcc.order.repository.OrderRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderingActionImpl implements OrderingAction {

    private final OrderRepository orderRepository;

    @Override
    public void ordering(BusinessActionContext actionContext, Ordering ordering) {
        Order order = new Order();
        order.setXid(actionContext.getXid());
        order.setProductId(ordering.getProductId());
        order.setBill(ordering.getBill());
        order.setStatus("pending");
        orderRepository.save(order);
    }

    @Override
    public TwoPhaseResult commit(BusinessActionContext actionContext) {
        orderRepository.findByXid(actionContext.getXid()).ifPresent(o -> {
            o.setStatus("ok");
            orderRepository.save(o);
        });
        return new TwoPhaseResult(true, "commit success");
    }

    @Override
    public TwoPhaseResult rollback(BusinessActionContext actionContext) {
        orderRepository.findByXid(actionContext.getXid()).ifPresent(o -> {
            o.setStatus("fail");
            orderRepository.save(o);
        });
        return new TwoPhaseResult(true, "rollback success");
    }
}
