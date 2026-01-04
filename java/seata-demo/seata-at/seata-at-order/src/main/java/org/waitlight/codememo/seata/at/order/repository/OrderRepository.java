package org.waitlight.codememo.seata.at.order.repository;


import org.springframework.data.repository.CrudRepository;
import org.waitlight.codememo.seata.at.order.model.Order;

public interface OrderRepository extends CrudRepository<Order, Long> {
}
