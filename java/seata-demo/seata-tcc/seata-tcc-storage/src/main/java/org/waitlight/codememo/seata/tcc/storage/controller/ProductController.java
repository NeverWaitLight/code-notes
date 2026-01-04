package org.waitlight.codememo.seata.tcc.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.waitlight.codememo.seata.tcc.storage.action.StorageReduceAction;
import org.waitlight.codememo.seata.tcc.storage.model.Product;
import org.waitlight.codememo.seata.tcc.storage.repository.ProductRepository;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository productRepository;
    private final StorageReduceAction storageReduceAction;

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public void submit(@RequestBody Product product) {
        productRepository.save(product);
    }

    @GetMapping
    public Iterable<Product> list() {
        return productRepository.findAll();
    }

    @PostMapping("/reduce")
    public void reduce(@RequestParam("id") Long id, @RequestParam("amount") Integer amount) {
        storageReduceAction.reduce(null, id, amount);
    }
}
