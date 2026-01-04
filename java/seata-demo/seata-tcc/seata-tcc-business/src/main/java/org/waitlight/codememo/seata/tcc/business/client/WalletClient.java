package org.waitlight.codememo.seata.tcc.business.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient("wallet")
public interface WalletClient {
    @PostMapping("/balance/pay")
    void pay(@RequestParam("username") String username, @RequestParam("amount") BigDecimal amount);
}
