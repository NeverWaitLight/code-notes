package org.waitlight.codememo.seata.tcc.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class TCCStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(TCCStorageApplication.class, args);
    }

}
