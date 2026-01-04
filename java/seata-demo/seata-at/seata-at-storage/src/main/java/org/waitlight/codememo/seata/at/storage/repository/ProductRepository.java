package org.waitlight.codememo.seata.at.storage.repository;

import org.springframework.data.repository.CrudRepository;
import org.waitlight.codememo.seata.at.storage.model.Product;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
