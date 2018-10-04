package com.itmuch.cloud.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author fw
 */

@SpringBootApplication(scanBasePackages = {"com.coolmq.amqp","com.itmuch.cloud.study"})
public class MessageDemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(MessageDemoApplication.class, args);
  }
}
