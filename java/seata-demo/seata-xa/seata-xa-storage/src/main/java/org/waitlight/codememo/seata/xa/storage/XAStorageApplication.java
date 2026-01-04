package org.waitlight.codememo.seata.xa.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class XAStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(XAStorageApplication.class, args);
    }

}
