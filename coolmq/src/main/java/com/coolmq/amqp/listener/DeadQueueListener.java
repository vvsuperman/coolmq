//package com.itmuch.cloud.study.amqp.listener;
//
//
//import com.coolmq.amqp.util.AlertSender;
//import com.coolmq.amqp.util.MQConstants;
//import com.rabbitmq.client.Channel;
//import org.springframework.amqp.rabbit.annotation.Exchange;
//import org.springframework.amqp.rabbit.annotation.Queue;
//import org.springframework.amqp.rabbit.annotation.QueueBinding;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Component;
//import sun.plugin2.message.Message;
//
//import java.io.IOException;
//
///**队列的注解实现形式，不好做统一控制
// * 死信队列 1 被reject而又无法requeue 2 TTL过期 3 到达队列最大长度
// *
// * */
//
//
//@Component
//public class DeadQueueListener {
//
//    @Autowired
//    AlertSender alertSender;
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = MQConstants.DLX_QUEUE, durable = "true" ),
//            exchange =  @Exchange( value = MQConstants.DLX_EXCHANGE, ignoreDeclarationExceptions = "true"),
//            key = MQConstants.DLX_ROUTING_KEY
//    ))
//    public void  deadMsgListener(Message message, Channel channel,
//                                 @Header(AmqpHeaders.DELIVERY_TAG) long ta) throws IOException {
//
//        alertSender.doSend(message.toString());
//        channel.basicAck(ta, false);
//
//    }
//}
