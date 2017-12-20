package com.coolmq.amqp.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coolmq.amqp.listener.DeadLetterMessageListener;
import com.coolmq.amqp.util.MQConstants;

import java.util.HashMap;
import java.util.Map;


/**
 * <p><b>Description:</b> RabbitMQ交换机、队列的配置类.定义交换机、key、queue并做好绑定，
 * 这里只绑定了死信队列。建议每个队列定义自己的QueueConfig
 * 
 * <p><b>Company:</b> 
 *
 * @author created by fw at 21:54 on 2017-10-23
 * @version V0.1
 */
@Configuration
public class RabbitQueueConfig {

    //========================== 声明交换机 ==========================
    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(MQConstants.DLX_EXCHANGE);
    }

    /**
     * 业务交换机
     */
    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange(MQConstants.BUSINESS_EXCHANGE);
    }

    //========================== 声明队列 ===========================
    /**
     * 死信队列
     */
    @Bean
    public Queue dlxQueue() {
        return new Queue(MQConstants.DLX_QUEUE);
    }

    //====================== 声明队列与交换机绑定 =======================
    /**
     * 通过死信路由key绑定死信交换机和死信队列
     */
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange())
                .with(MQConstants.DLX_ROUTING_KEY);
    }
    
    /**
     * 死信队列的监听
     * @param connectionFactory RabbitMQ连接工厂
     * @param DeadLetterMessageListener 死信的监听者
     * @return 监听容器对象
     */
    @Bean
    public SimpleMessageListenerContainer redisLogListenerContainer(ConnectionFactory connectionFactory, 
    		DeadLetterMessageListener deadLetterMessageListener) {
    	
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(dlxQueue());
        container.setExposeListenerChannel(true);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener(deadLetterMessageListener);
        return container;
    }
    
}
