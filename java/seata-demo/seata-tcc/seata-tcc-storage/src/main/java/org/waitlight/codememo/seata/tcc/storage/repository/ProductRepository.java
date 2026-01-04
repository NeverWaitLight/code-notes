package org.waitlight.codememo.seata.tcc.storage.repository;

import org.springframework.data.repository.CrudRepository;
import org.waitlight.codememo.seata.tcc.storage.model.Product;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
