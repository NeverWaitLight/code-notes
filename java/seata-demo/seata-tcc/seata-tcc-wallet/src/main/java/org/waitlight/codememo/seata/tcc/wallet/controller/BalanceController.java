package org.waitlight.codememo.seata.tcc.wallet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.waitlight.codememo.seata.tcc.wallet.action.BalancePayAction;
import org.waitlight.codememo.seata.tcc.wallet.model.Balance;
import org.waitlight.codememo.seata.tcc.wallet.repository.BalanceRepository;

import java.math.BigDecimal;

@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
public class BalanceController {
    private final BalanceRepository balanceRepository;
    private final BalancePayAction balancePayAction;

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public void submit(@RequestBody Balance balance) {
        balanceRepository.save(balance);
    }

    @GetMapping
    public Iterable<Balance> list() {
        return balanceRepository.findAll();
    }

    @PostMapping("/pay")
    public void pay(@RequestParam("username") String username, @RequestParam("amount") BigDecimal amount) {
        balancePayAction.pay(null, username, amount);
    }

}
