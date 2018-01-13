package com.itmuch.cloud.study.consumer.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import com.coolmq.amqp.listener.AbstractMessageListener;

@Component
public class BizMessageListener extends AbstractMessageListener  {
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void receiveMessage(Message message, MessageConverter messageConverter) {
		Object bizObj = messageConverter.fromMessage(message);
		logger.info("get message success:"+bizObj.toString());
	}
}
