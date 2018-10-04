package com.coolmq.amqp.config;

import com.coolmq.amqp.util.CompleteCorrelationData;
import com.coolmq.amqp.util.DBCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;


/**
 * <p><b>Description:</b> RabbitTemplate配置工厂类
 * <p><b>Company:</b> 
 *
 * @author created by hongda at 11:33 on 2017-07-05
 * @version V0.1
 */
@Configuration
public class RabbitTemplateConfig {
	 private Logger logger = LoggerFactory.getLogger(RabbitTemplateConfig.class);

	 @Autowired
	 ApplicationContext applicationContext;

     @Autowired
     private RedisTemplate<String, Object> redisTemplate;

     boolean returnFlag = false;
	 
     @Bean
     public RabbitTemplate customRabbitTemplate(ConnectionFactory connectionFactory) {
         logger.info("==> custom rabbitTemplate, connectionFactory:"+ connectionFactory);
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        // mandatory 必须设置为true，ReturnCallback才会调用
        rabbitTemplate.setMandatory(true);
        // 消息发送到RabbitMQ交换器后接收ack回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if(returnFlag){
                logger.error("mq发送错误，无对应的的交换机,confirm回掉,ack={},correlationData={} cause={} returnFlag={}",
                        ack, correlationData, cause, returnFlag);
            }

            logger.info("confirm回调，ack={} correlationData={} cause={}", ack, correlationData, cause);
            String msgId = correlationData.getId();

            /** 只要消息能投入正确的消息队列，并持久化，就返回ack为true*/
            if(ack){
                logger.info("消息已正确投递到队列, correlationData:{}", correlationData);
                //清除重发缓存
                String dbCoordinatior = ((CompleteCorrelationData)correlationData).getCoordinator();
                DBCoordinator coordinator = (DBCoordinator)applicationContext.getBean(dbCoordinatior);
                coordinator.setMsgSuccess(msgId);
            }else{
                logger.error("消息投递至交换机失败,业务号:{}，原因:{}",correlationData.getId(),cause);
            }

        });

        //消息发送到RabbitMQ交换器，但无相应Exchange时的回调
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            String messageId = message.getMessageProperties().getMessageId();

            logger.error("return回调，没有找到任何匹配的队列！message id:{},replyCode{},replyText:{},"
                    + "exchange:{},routingKey{}", messageId, replyCode, replyText, exchange, routingKey);
            returnFlag = true;
        });

//        /** confirm的超时时间*/
//        rabbitTemplate.waitForConfirms(MQConstants.TIME_GAP);

        return rabbitTemplate;
    }
	 
	 @Bean
     public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter();
        return jsonMessageConverter;
     }
}
