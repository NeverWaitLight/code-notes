package org.waitlight.codememo.seata.tcc.order.repository;


import org.springframework.data.repository.CrudRepository;
import org.waitlight.codememo.seata.tcc.order.model.Order;

import java.util.Optional;

public interface OrderRepository extends CrudRepository<Order, Long> {
    Optional<Order> findByXid(String xid);
}
