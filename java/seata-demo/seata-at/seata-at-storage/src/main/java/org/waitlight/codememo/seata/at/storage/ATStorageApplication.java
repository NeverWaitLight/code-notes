package org.waitlight.codememo.seata.at.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ATStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ATStorageApplication.class, args);
    }

}
