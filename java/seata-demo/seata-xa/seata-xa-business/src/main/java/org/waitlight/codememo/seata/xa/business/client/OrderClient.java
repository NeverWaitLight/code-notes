package org.waitlight.codememo.seata.xa.business.client;

import org.example.xa.commons.Ordering;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient("order")
public interface OrderClient {
    @PostMapping("/orders")
    void create(Ordering ordering);
}
