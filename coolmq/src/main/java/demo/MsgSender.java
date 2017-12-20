package demo;

import org.slf4j.Logger;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.coolmq.amqp.sender.RabbitSender;
import com.coolmq.amqp.util.RabbitMetaMessage;

public class MsgSender {
	
	@Autowired
	RedisTemplate redisTemplate;
	
	@Autowired
	RabbitTemplate rabbitTemplate;
	
	@Autowired
	Logger logger;
	
	public  void produce() {
		/** 生成一个发送对象 */
		RabbitMetaMessage  rabbitMetaMessage = new RabbitMetaMessage();
		/**设置交换机 */
		rabbitMetaMessage.setExchange("biz_exchange");
		/**是指routing key */
		rabbitMetaMessage.setRoutingKey("biz_key");
		/** 设置需要传递的消息体 */
		rabbitMetaMessage.setPayload("the message you want to send");
		/** 发送消息 */
		RabbitSender.send(rabbitMetaMessage, redisTemplate, rabbitTemplate, logger);
	}
}
