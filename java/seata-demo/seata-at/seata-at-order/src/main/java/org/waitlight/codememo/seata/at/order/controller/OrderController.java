package org.waitlight.codememo.seata.at.order.controller;

import org.example.xa.commons.Ordering;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.waitlight.codememo.seata.at.order.model.Order;
import org.waitlight.codememo.seata.at.order.repository.OrderRepository;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public void submit(@RequestBody Ordering ordering) {
        Order order = new Order();
        order.setProductId(ordering.getProductId());
        order.setBill(ordering.getBill());
        orderRepository.save(order);
    }

    @GetMapping
    public Iterable<Order> list() {
        return orderRepository.findAll();
    }
}
