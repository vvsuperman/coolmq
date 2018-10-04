package com.coolmq.amqp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * <p><b>Description:</b> Rabbit MQ连接工厂配置
 * <p><b>Company:</b> 
 *
 * @author created by hongda at 11:33 on 2017-07-05
 * @version V0.1
 */
@Configuration
@EnableRabbit
public class RabbitConnectionConfig {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //将配置提出来，方便apollo配置中心,或做灵活配置
    @Value("${spring.rabbitmq.host}")
    String host;

    @Value("${spring.rabbitmq.port}")
    int port;

    @Value("${spring.rabbitmq.username}")
    String username;

    @Value("${spring.rabbitmq.password}")
    String password;

//    @Value("${spring.rabbitmq.connection-timeout}")
//    int connectionTimeout;
//
//    @Value("${spring.rabbitmq.template.receive-timeout}")
//    int receiveTimeout;



    @Value("${spring.rabbitmq.virtual.host}")
    String virtualHost;

    @Value("${spring.rabbitmq.cache.channel.size}")
    int cacheSize;


    /**
     * 创建RabbitMQ连接工厂
     *
     * @param
     * @return CachingConnectionFactory
     * @throws Exception 异常
     */
    @Bean
    public CachingConnectionFactory rabbitConnectionFactory() throws Exception {
        logger.info("==> custom rabbitmq connection factory");

        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();

        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        //factory.setConnectionTimeout(connectionTimeout);
//        factory.setAutomaticRecoveryEnabled(true);
        factory.afterPropertiesSet();

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(factory.getObject());
        connectionFactory.setPublisherReturns(true);
        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setChannelCacheSize(cacheSize);


        return connectionFactory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        logger.info("==> custom rabbitmq Listener factory:"+ connectionFactory);
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }


   
}

