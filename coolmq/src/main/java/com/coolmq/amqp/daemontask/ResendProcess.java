package com.coolmq.amqp.daemontask;


import com.coolmq.amqp.config.RabbitTemplateConfig;
import com.coolmq.amqp.sender.RabbitSender;
import com.coolmq.amqp.util.AlertSender;
import com.coolmq.amqp.util.DBCoordinator;
import com.coolmq.amqp.util.MQConstants;
import com.coolmq.amqp.util.RabbitMetaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResendProcess {
    private Logger logger = LoggerFactory.getLogger(RabbitTemplateConfig.class);

    @Autowired
    DBCoordinator dbCoordinator;

    @Autowired
    RabbitSender rabbitSender;

    @Autowired
    AlertSender alertSender;

    /**prepare状态的消息超时告警*/
    public void alertPrepareMsg() throws Exception{
        List<String> messageIds = dbCoordinator.getMsgPrepare();
        for(String messageId: messageIds){
            alertSender.doSend(messageId);
        }
    }


    public void resendReadyMsg() throws Exception{
        List<RabbitMetaMessage> messages = dbCoordinator.getMsgReady();
        for(RabbitMetaMessage message: messages){
            long msgCount = dbCoordinator.getResendValue(MQConstants.MQ_RESEND_COUNTER,message.getMessageId());
            if( msgCount > MQConstants.MAX_RETRY_COUNT){
                alertSender.doSend(message.getMessageId());
            }
            rabbitSender.send(message);
            dbCoordinator.incrResendKey(MQConstants.MQ_RESEND_COUNTER, message.getMessageId());
        }
    }


}
