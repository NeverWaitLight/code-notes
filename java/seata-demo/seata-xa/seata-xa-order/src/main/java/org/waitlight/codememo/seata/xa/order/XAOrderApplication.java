package org.waitlight.codememo.seata.xa.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class XAOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(XAOrderApplication.class, args);
    }

}
