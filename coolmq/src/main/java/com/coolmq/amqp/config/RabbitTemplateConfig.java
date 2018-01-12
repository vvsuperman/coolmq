package com.coolmq.amqp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.coolmq.amqp.util.MQConstants;
import com.coolmq.amqp.util.RabbitMetaMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


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
	 private ObjectMapper objectMapper = new ObjectMapper();

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
            // 只要消息能投入正确的交换器中，ack就为true
            if (ack) {
                if (!metaMessage.isReturnCallback()) {
                    logger.info("消息已正确投递到队列，correlationData:{}", correlationData);
                    // 清除重发缓存
                    redisTemplate.opsForHash().delete(MQConstants.MQ_PRODUCER_RETRY_KEY, cacheKey);
                } else {
                    logger.warn("交换机投机消息至队列失败，correlationData:{}", correlationData);
                }
            } else {
                logger.error("消息投递至交换机失败，correlationData:{}，原因：{}", correlationData, cause);
                if (!metaMessage.isAutoTrigger()) {
                    metaMessage.setAutoTrigger(true);
                    try {
                        redisTemplate.opsForHash().put(MQConstants.MQ_PRODUCER_RETRY_KEY, cacheKey, objectMapper.writeValueAsString(metaMessage));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //消息发送到RabbitMQ交换器，但无相应Exchange时的回调
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            String cacheKey = message.getMessageProperties().getMessageId();

            logger.error("return回调，没有找到任何匹配的队列！message id:{},replyCode{},replyText:{},"
                    + "exchange:{},routingKey{}", cacheKey, replyCode, replyText, exchange, routingKey);

            RabbitMetaMessage metaMessage = (RabbitMetaMessage) redisTemplate.opsForHash().get(MQConstants.MQ_PRODUCER_RETRY_KEY, cacheKey);

            metaMessage.setReturnCallback(true);

            // 由于amqp奇葩协议规定，return比confirm先回调
            redisTemplate.opsForHash().put(MQConstants.MQ_PRODUCER_RETRY_KEY, cacheKey, metaMessage);
        });
        return rabbitTemplate;
    }
	 
	 @Bean
     public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter();
        return jsonMessageConverter;
     }
}
