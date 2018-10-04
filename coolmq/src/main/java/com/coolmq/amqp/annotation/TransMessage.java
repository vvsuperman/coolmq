package com.coolmq.amqp.annotation;

import java.lang.annotation.*;

/**
 * 注解类，用来无侵入的实现分布式事务
 * */
@Inherited
@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.METHOD)  
@Documented  
public @interface TransMessage {
	String exchange() default "";   //要发送的交换机
	String bindingKey() default "";    //要发送的key
	String bizName() default "";      //业务编号
	String dbCoordinator() default "";	//消息落库的处理方式db or redis
}  
