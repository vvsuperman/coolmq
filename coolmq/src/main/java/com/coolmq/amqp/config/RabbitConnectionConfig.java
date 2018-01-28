package com.coolmq.amqp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <p><b>Description:</b> Rabbit MQ连接工厂配置
 * <p><b>Company:</b> 
 *
 * @author created by hongda at 11:33 on 2017-07-05
 * @version V0.1
 */
@Configuration
public class RabbitConnectionConfig {
    private Logger logger = LoggerFactory.getLogger(RabbitConnectionConfig.class);

    /**
     * 创建RabbitMQ连接工厂
     *
     * @param rabbitProperties RabbitMQ属性
     * @return CachingConnectionFactory
     * @throws Exception 异常
     */
    @Bean
    public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties rabbitProperties) throws Exception {
        logger.info("==> custom rabbitmq connection factory, addresses: {}", rabbitProperties.getAddresses());

        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
        if (rabbitProperties.determineHost() != null) {
            factory.setHost(rabbitProperties.determineHost());
        }
        factory.setPort(rabbitProperties.determinePort());
        if (rabbitProperties.determineUsername() != null) {
            factory.setUsername(rabbitProperties.determineUsername());
        }
        if (rabbitProperties.determinePassword() != null) {
            factory.setPassword(rabbitProperties.determinePassword());
        }
        if (rabbitProperties.determineVirtualHost() != null) {
            factory.setVirtualHost(rabbitProperties.determineVirtualHost());
        }
//        if (rabbitProperties.getRequestedHeartbeat() != null) {
//            factory.setRequestedHeartbeat(rabbitProperties.getRequestedHeartbeat());
//        }

        RabbitProperties.Ssl ssl = rabbitProperties.getSsl();
        if (ssl.isEnabled()) {
            factory.setUseSSL(true);
            if (ssl.getAlgorithm() != null) {
                factory.setSslAlgorithm(ssl.getAlgorithm());
            }
            factory.setKeyStore(ssl.getKeyStore());
            factory.setKeyStorePassphrase(ssl.getKeyStorePassword());
            factory.setTrustStore(ssl.getTrustStore());
            factory.setTrustStorePassphrase(ssl.getTrustStorePassword());
        }

        if (rabbitProperties.getConnectionTimeout() != null) {
            factory.setConnectionTimeout(rabbitProperties.getConnectionTimeout());
        }

        factory.afterPropertiesSet();

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(factory.getObject());
        connectionFactory.setAddresses(shuffle(rabbitProperties.determineAddresses()));
//        connectionFactory.setPublisherConfirms(rabbitProperties.isPublisherConfirms());
//        connectionFactory.setPublisherReturns(rabbitProperties.isPublisherReturns());
        /** 设置消息必须confirm */
        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);
        if (rabbitProperties.getCache().getChannel().getSize() != null) {
            connectionFactory.setChannelCacheSize(rabbitProperties.getCache().getChannel().getSize());
        }
        if (rabbitProperties.getCache().getConnection().getMode() != null) {
            connectionFactory.setCacheMode(rabbitProperties.getCache().getConnection().getMode());
        }
        if (rabbitProperties.getCache().getConnection().getSize() != null) {
            connectionFactory.setConnectionCacheSize(rabbitProperties.getCache().getConnection().getSize());
        }
        if (rabbitProperties.getCache().getChannel().getCheckoutTimeout() != null) {
            connectionFactory.setChannelCheckoutTimeout(rabbitProperties.getCache().getChannel().getCheckoutTimeout());
        }
        return connectionFactory;
    }

    /**
     * 用于启动时，每个节点连接随机的Rabbit MQ服务器
     *
     * @param addresses Rabbit MQ服务器列表，比如：ip1:port1,ip2:port2
     * @return 搅乱后的rabbit mq 服务器地址列表
     */
     String shuffle(String addresses) {
        String[] addrArr = StringUtils.commaDelimitedListToStringArray(addresses);
        List<String> addrList = Arrays.asList(addrArr);
        Collections.shuffle(addrList);

        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iter = addrList.iterator();
        while (iter.hasNext()) {
            stringBuilder.append(iter.next());
            if (iter.hasNext()) {
                stringBuilder.append(",");
            }
        }

        logger.info("==> rabbitmq shuffle addresses: {}", stringBuilder);

        return stringBuilder.toString();
    }

   
}

