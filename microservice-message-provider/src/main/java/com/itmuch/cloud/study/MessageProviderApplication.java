package com.itmuch.cloud.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

/**
 * 使用@EnableFeignClients开启Feign
 * @author eacdy
 */

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages= {"com.coolmq.amqp.config","com.itmuch.cloud.study"})
public class MessageProviderApplication {
  public static void main(String[] args) {
    SpringApplication.run(MessageProviderApplication.class, args);
  }
}
