package org.waitlight.codememo.seata.xa.order.repository;


import org.springframework.data.repository.CrudRepository;
import org.waitlight.codememo.seata.xa.order.model.Order;

public interface OrderRepository extends CrudRepository<Order, Long> {
}
