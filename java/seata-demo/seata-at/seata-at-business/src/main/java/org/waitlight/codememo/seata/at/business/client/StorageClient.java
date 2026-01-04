package org.waitlight.codememo.seata.at.business.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("storage")
public interface StorageClient {
    @PostMapping("/products/reduce")
    void reduce(@RequestParam("id") Long id, @RequestParam("amount") Integer amount);
}
