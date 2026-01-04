package org.waitlight.codememo.seata.at.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ATOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ATOrderApplication.class, args);
    }

}
