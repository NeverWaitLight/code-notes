package org.waitlight.codememo.seata.tcc.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class TCCOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TCCOrderApplication.class, args);
    }

}
