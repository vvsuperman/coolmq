package com.coolmq.amqp.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;  
import org.aspectj.lang.annotation.Aspect;  
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.coolmq.amqp.sender.RabbitSender;
import com.coolmq.amqp.util.MQConstants;
import com.coolmq.amqp.util.RabbitMetaMessage;

/** 
 * 描述：封装sender
 * @author fw 
 * 创建时间：2017年10月14日 下午10:30:00 
 * @version 1.0.0 
 */  
@Component 
@Aspect 
public class SenderWraper {  
	Logger logger = LoggerFactory.getLogger(SenderWraper.class);
	
	@Autowired
	RabbitSender rabbitSender;
		
	/**  定义注解类型的切点，只要方法上有该注解，都会匹配  */
    @Pointcut("@annotation(com.coolmq.amqp.annotation.MqSender)")
    public void annotationSender(){          
    }  
    
    @Around("annotationSender()&& @annotation(args)")
    public void sendMsg(ProceedingJoinPoint joinPoint, MqSender args) throws Throwable {
    		
    		/** annotaton中的exchange和queue不得为空 */
    	    if(joinPoint.getArgs().length!=1 && joinPoint.getArgs()[0] == null) {
      	   logger.error("senderWraper args is null");    
     	}	
    	    
    	    String exchange = args.exchange();
    	    String routingkey = args.routingkey();
    	    
    		/** 执行业务函数 */
		Object returnObj = joinPoint.proceed();
		if(returnObj == null) {
			returnObj = MQConstants.BLANK_STR;
		}   	
		
		/** 生成一个发送对象 */
		RabbitMetaMessage  rabbitMetaMessage = new RabbitMetaMessage();
		/**设置交换机 */
		rabbitMetaMessage.setExchange(exchange);
		/**指定routing key */
		rabbitMetaMessage.setRoutingKey(routingkey);
		/** 设置需要传递的消息体,可以是任意对象 */
		rabbitMetaMessage.setPayload(returnObj);	
		
		/** 发送消息 */
		try {
			rabbitSender.send(rabbitMetaMessage);
		} catch (Exception e) {
			logger.error("消息发送异常" + e.toString());
			throw e;
		}
   }
}
