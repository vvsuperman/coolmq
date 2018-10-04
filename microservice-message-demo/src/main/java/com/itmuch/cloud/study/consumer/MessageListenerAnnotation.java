//package com.itmuch.cloud.study.consumer;
//
////import com.coolmq.amqp.util.AlertSender;
////import com.coolmq.amqp.util.MQConstants;
//import com.rabbitmq.client.Channel;
//import org.springframework.amqp.rabbit.annotation.*;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Component;
//import org.springframework.amqp.core.Message;
//
//import java.io.IOException;
//
///**
// * 死信队列 1 被reject而又无法requeue 2 TTL过期 3 到达队列最大长度
// *
// * */
//
//
//@Component
//public class MessageListenerAnnotation {
//
//    @RabbitListener(
//            bindings = @QueueBinding(
//                value = @Queue(value = "queue.transmsg", durable = "true"
//                        //,arguments = @Argument(name = "x-message-ttl", value = "10000",type = "java.lang.Integer")
//                ),
//                exchange =  @Exchange( value = "exchange.transmsg",ignoreDeclarationExceptions = "true",durable = "true"),
//                key = "key.transmsg"
//            )
//    )
//    public void  msgListener(Message message, Channel channel,
//                                 @Header(AmqpHeaders.DELIVERY_TAG) long ta) throws IOException {
//
//        System.out.println("receive msg..........."+ new String(message.getBody(),"UTF-8"));
//        channel.basicAck(ta, false);
//
//    }
//}
