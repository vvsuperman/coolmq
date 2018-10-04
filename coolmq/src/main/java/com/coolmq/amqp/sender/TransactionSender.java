package com.coolmq.amqp.sender;

import com.coolmq.amqp.annotation.TransMessage;
import com.coolmq.amqp.util.DBCoordinator;
import com.coolmq.amqp.util.MQConstants;
import com.coolmq.amqp.util.RabbitMetaMessage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/** 
 * 描述：封装sender
 * @author fw 
 * 创建时间：2017年10月14日 下午10:30:00 
 * @version 1.0.0 
 */  
@Component 
@Aspect 
public class TransactionSender {
	Logger logger = LoggerFactory.getLogger(TransactionSender.class);
	
	@Autowired
    RabbitSender rabbitSender;

	@Autowired
	DBCoordinator dbCoordinator;

	@Autowired
	ApplicationContext applicationContext;

	/**  定义注解类型的切点，只要方法上有该注解，都会匹配  */
    @Pointcut("@annotation(com.coolmq.amqp.annotation.TransMessage)")
    public void annotationSender(){          
    }  
    
    @Around("annotationSender()&& @annotation(rd)")
    public void sendMsg(ProceedingJoinPoint joinPoint, TransMessage rd) throws Throwable {
		logger.info("==> custom mq annotation,rd"+ rd);
    	String exchange = rd.exchange();
    	String bindingKey = rd.bindingKey();
    	String dbCoordinator = rd.dbCoordinator();
    	String bizName = rd.bizName() + MQConstants.DB_SPLIT + getCurrentDateTime();
    	DBCoordinator coordinator = null;

    	try{
    		coordinator = (DBCoordinator) applicationContext.getBean(dbCoordinator);
		}catch (Exception ex){
			logger.error("无消息存储类，事务执行终止");
			return;
    	}

		/**发送前暂存消息*/
    	coordinator.setMsgPrepare(bizName);

		Object returnObj = null;
    	/** 执行业务函数 */
    	try{
			returnObj = joinPoint.proceed();
		}catch (Exception ex){
    		logger.error("业务执行失败,业务名称:" + bizName);
    		throw ex;
		}

		if(returnObj == null) {
			returnObj = MQConstants.BLANK_STR;
		}   	
		
		/** 生成一个发送对象 */
		RabbitMetaMessage rabbitMetaMessage = new RabbitMetaMessage();

		rabbitMetaMessage.setMessageId(bizName);
		/**设置交换机 */
		rabbitMetaMessage.setExchange(exchange);
		/**指定routing key */
		rabbitMetaMessage.setRoutingKey(bindingKey);
		/** 设置需要传递的消息体,可以是任意对象 */
		rabbitMetaMessage.setPayload(returnObj);

		/** 将消息设置为ready状态*/
		coordinator.setMsgReady(bizName, rabbitMetaMessage);
		
		/** 发送消息 */
		try {
			rabbitSender.setCorrelationData(dbCoordinator);
			rabbitSender.send(rabbitMetaMessage);
		} catch (Exception e) {
			logger.error("第一阶段消息发送异常" + bizName + e);
			throw e;
		}
   }

   public static String getCurrentDateTime(){
	   SimpleDateFormat df = new SimpleDateFormat(MQConstants.TIME_PATTERN);
	   return df.format(new Date());
   }
}
