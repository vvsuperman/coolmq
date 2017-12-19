package com.coolmq.amqp.config;

import com.coolmq.amqp.util.RabbitMetaMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
public class CustomRabbitAutoConfiguration {
    private Logger logger = LoggerFactory.getLogger(CustomRabbitAutoConfiguration.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

//        if (rabbitProperties.getConnectionTimeout() != null) {
//            factory.setConnectionTimeout(rabbitProperties.getConnectionTimeout());
//        }

        factory.afterPropertiesSet();

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(factory.getObject());
        connectionFactory.setAddresses(shuffle(rabbitProperties.determineAddresses()));
        connectionFactory.setPublisherConfirms(rabbitProperties.isPublisherConfirms());
        connectionFactory.setPublisherReturns(rabbitProperties.isPublisherReturns());
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
    private String shuffle(String addresses) {
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

    @Bean
    public RabbitTemplate customRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
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

        // 消息发送到RabbitMQ交换器，但无相应队列与交换器绑定时的回调
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
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter();
        return jsonMessageConverter;
    }
}
