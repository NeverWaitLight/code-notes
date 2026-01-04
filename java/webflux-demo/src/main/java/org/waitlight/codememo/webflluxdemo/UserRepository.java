package org.waitlight.codememo.webflluxdemo;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    @Query("{'name': {$regex: ?0,$options: 'i'}}")
    Flux<User> findByName(String name);
}