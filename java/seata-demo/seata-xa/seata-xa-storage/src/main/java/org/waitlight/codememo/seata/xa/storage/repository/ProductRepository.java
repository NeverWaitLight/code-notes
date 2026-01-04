package org.waitlight.codememo.seata.xa.storage.repository;

import org.springframework.data.repository.CrudRepository;
import org.waitlight.codememo.seata.xa.storage.model.Product;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
