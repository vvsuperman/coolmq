package com.coolmq.amqp.config;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.coolmq.amqp.sender.RabbitSender;
import com.coolmq.amqp.util.MQConstants;
import com.coolmq.amqp.util.RabbitMetaMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;



/**
 * <p><b>Description:</b> RabbitTemplate配置工厂类
 * <p><b>Company:</b> 
 *
 * @author created by hongda at 11:33 on 2017-07-05
 * @version V0.1
 */
@Configuration
@ComponentScan
public class RabbitTemplateConfig {
	 private Logger logger = LoggerFactory.getLogger(RabbitTemplateConfig.class);
	 static Boolean SUCESS_SEND = false;
	 
	 @Autowired
	 RabbitSender rabbitSender;

     @Autowired
     private RedisTemplate<String, Object> redisTemplate;	
	 
     @Bean
     public RabbitTemplate customRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        // mandatory 必须设置为true，ReturnCallback才会调用
        rabbitTemplate.setMandatory(true);
        // 消息发送到RabbitMQ交换器后接收ack回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {

            logger.debug("confirm回调，ack={} correlationData={} cause={}", ack, correlationData, cause);

            String cacheKey = correlationData.getId();
            RabbitMetaMessage metaMessage = (RabbitMetaMessage) redisTemplate.opsForHash().get(MQConstants.MQ_PRODUCER_RETRY_KEY, cacheKey);
            // 只要消息能投入正确的交换器中，并持久化，就返回ack为true
            if (ack) {
                logger.info("消息已正确投递到队列，correlationData:{}", correlationData);
                	// 清除重发缓存
                redisTemplate.opsForHash().delete(MQConstants.MQ_PRODUCER_RETRY_KEY, cacheKey);
                SUCESS_SEND = true;
            // 除无Exchange，以及网络中断外的其它异常：重发消息
            } else {
                logger.error("消息投递至交换机失败。correlationData:{}，原因：{}", correlationData, cause);
                //重发消息
                reSendMsg(cacheKey, metaMessage);
            }
        });

        //消息发送到RabbitMQ交换器，但无相应Exchange时触发此回掉：重发消息
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            String cacheKey = message.getMessageProperties().getMessageId();
            message.getClass().getName();
            logger.error("消息投递至交换机失败，没有找到任何匹配的队列！message id:{},replyCode{},replyText:{},"
                    + "exchange:{},routingKey{}", cacheKey, replyCode, replyText, exchange, routingKey);

            RabbitMetaMessage metaMessage = (RabbitMetaMessage) redisTemplate.opsForHash().get(MQConstants.MQ_PRODUCER_RETRY_KEY, cacheKey);
            //重发消息
            reSendMsg(cacheKey, metaMessage);
        });
        
        return rabbitTemplate;
    }
     
    //重发消息
    public void reSendMsg(String msgId, RabbitMetaMessage rabbitMetaMessage) {
    	
    	    class ReSendThread implements Callable{
    	    	    
    	    		String msgId;
    	    		RabbitMetaMessage rabbitMetaMessage;
    	    		
    	    		public ReSendThread(String msgId, RabbitMetaMessage rabbitMetaMessage) {
    	    			this.msgId = msgId;
    	    			this.rabbitMetaMessage = rabbitMetaMessage;
    	    		}

			@Override
			public Boolean call() throws Exception {
				//如果发送成功，重发结束
				if (SUCESS_SEND)
					return true;
				//重发消息
				rabbitSender.sendMsg(this.rabbitMetaMessage, this.msgId);
				return false;
			}
    	    }
    	    
	    	Retryer<Boolean> retryer = RetryerBuilder
	                .<Boolean>newBuilder()
	                //抛出runtime异常、checked异常时都会重试，但是抛出error不会重试。
	                .retryIfException()
	                .retryIfResult(Predicates.equalTo(false))
	                //重试策略  100ms*2^n 最多10s
	                .withWaitStrategy(WaitStrategies.exponentialWait(500, 10, TimeUnit.SECONDS))
	                .withStopStrategy(StopStrategies.neverStop())
	                .build();
	 
	    	ReSendThread reSendThread = new ReSendThread(msgId, rabbitMetaMessage);
	    	logger.info("重发消息，msgId:{}", msgId);
        try {
            retryer.call(reSendThread);
            //未发送成功，入死信队列
            if(!SUCESS_SEND)
            	     rabbitSender.sendMsgToDeadQueue((String)rabbitMetaMessage.getPayload());
        } catch (Exception e) {
            logger.error("重发消息异常");
        }
    }
	 
	 @Bean
	 public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
	    Jackson2JsonMessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter();
	    return jsonMessageConverter;
	 }
}
