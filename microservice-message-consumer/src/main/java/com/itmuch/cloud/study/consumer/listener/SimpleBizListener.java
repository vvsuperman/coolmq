package com.itmuch.cloud.study.consumer.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues="hello")
public class SimpleBizListener {
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	@RabbitHandler
	public void process(String hello) {
		logger.info("get msg.............." + hello);
	}
}
