package com.coolmq.amqp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;  
import java.lang.annotation.RetentionPolicy;  

/**
 * 注解类，用来无侵入的实现分布式事务
 * */

@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.METHOD)  
@Documented  
public @interface MqSender {  
	String exchange() default "";   //要发送的交换机
	String routingkey() default "";    //要发送的key
}  
