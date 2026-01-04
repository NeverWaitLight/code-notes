package org.waitlight.codememo.seata.xa.storage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.waitlight.codememo.seata.xa.storage.model.Product;
import org.waitlight.codememo.seata.xa.storage.repository.ProductRepository;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    public void submit(@RequestBody Product product) {
        productRepository.save(product);
    }

    @GetMapping
    public Iterable<Product> list() {
        return productRepository.findAll();
    }

    @Transactional
    @PostMapping("/reduce")
    public void reduce(@RequestParam("id") Long id, @RequestParam("amount") Integer amount) {
        productRepository.findById(id).map(p -> {
            p.setStock(p.getStock() - amount);
            return p;
        }).ifPresent(p -> productRepository.save(p));
    }
}
