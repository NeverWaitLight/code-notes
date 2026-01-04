package org.waitlight.codememo.seata.tcc.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class TCCWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(TCCWalletApplication.class, args);
    }

}
