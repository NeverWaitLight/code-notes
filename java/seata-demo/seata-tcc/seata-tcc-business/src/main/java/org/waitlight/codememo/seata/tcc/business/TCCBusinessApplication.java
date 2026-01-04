package org.waitlight.codememo.seata.tcc.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class TCCBusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(TCCBusinessApplication.class, args);
    }

}
