package com.itmuch.cloud.study.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coolmq.amqp.annotation.MqSender;
import com.coolmq.amqp.sender.RabbitSender;
import com.coolmq.amqp.util.MQConstants;
import com.coolmq.amqp.util.RabbitMetaMessage;
import com.fasterxml.jackson.core.JsonProcessingException;


@RestController
public class MessageProviderController {
 
  /** test if can get value from config center **/
  @Value("${spring.rabbitmq.host}")
  private String rabbitmqAddress;
  
  @Autowired
  SimpleSender simpleSender;
  
  
//  @Autowired
//  private RabbitSender rabbitSender;
  
  Logger logger = LoggerFactory.getLogger(getClass());
  
  
  @GetMapping("testconfig")
  public String testconfig() throws Exception {
	  return rabbitmqAddress;
  }
  
//  @GetMapping("testmq")
//  @Transactional
//  public String testMessage() throws Exception {
//	    /** 生成一个发送对象 */
//		RabbitMetaMessage  rabbitMetaMessage = new RabbitMetaMessage();
//		/**设置交换机 */
//		rabbitMetaMessage.setExchange(MQConstants.BUSINESS_EXCHANGE);
//		/**指定routing key */
//		rabbitMetaMessage.setRoutingKey(MQConstants.BUSINESS_KEY);
//		/** 设置需要传递的消息体,可以是任意对象 */
//		rabbitMetaMessage.setPayload("the message you want to send");	
//		
//		//do some biz
//		
//		/** 发送消息 */
//		rabbitSender.send(rabbitMetaMessage);
//		
//		return "sucess";
//  }
  
  @GetMapping("testmqanno")
  @Transactional
  @MqSender(exchange = MQConstants.BUSINESS_EXCHANGE, routingkey = MQConstants.BUSINESS_KEY)
  public String testMessageWithAnnotation() throws Exception {
	    /** 生成一个发送对象 */
		RabbitMetaMessage  rabbitMetaMessage = new RabbitMetaMessage();
		
		//do some biz
		
		return "sucess";
  }
  
  @GetMapping("testsimplemq")
  @Transactional
  public String testSimpleSender() throws Exception {
	    /** 生成一个发送对象 */
		simpleSender.send();
		//do some biz
		return "sucess";
  }

}
