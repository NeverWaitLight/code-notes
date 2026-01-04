package org.waitlight.codememo.seata.xa.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class XAWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(XAWalletApplication.class, args);
    }

}
