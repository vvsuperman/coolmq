package com.coolmq.amqp.util;
/**
 * <p><b>Description:</b> 常量类 <p>
 * <b>Company:</b> 
 *
 * @author created by fw at 22:49 on 2017-10-23
 * @version V0.1
 */
public class MQConstants {

	/** 消息重发计数*/
	public static final String MQ_RESEND_COUNTER = "mq.resend.counter";

	/** 消息最大重发次数*/
	public static final long MAX_RETRY_COUNT = 3;

	/** 分隔符*/
	public static final String DB_SPLIT = ",";

	/** 缓存超时时间,超时进行重发 */
	public static final long TIME_GAP = 2000;

	public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";


	/**处于ready状态消息*/
	public static final Object MQ_MSG_READY = "mq.msg.ready";

	/**处于prepare状态消息*/
	public static final Object MQ_MSG_PREPARE = "mq.msg.prepare";
	
	/**你的业务交换机名称*/
	public static final String BUSINESS_EXCHANGE = "business.exchange";
	/**你的业务队列名称*/
	public static final String BUSINESS_QUEUE = "business.queue";
	/**你的业务key*/
	public static final String BUSINESS_KEY = "business.key";
	
	
	public static final String MQ_PRODUCER_RETRY_KEY = "mq.producer.retry.key";
	public static final String MQ_CONSUMER_RETRY_COUNT_KEY = "mq.consumer.retry.count.key";
	/**死信队列配置*/
	public static final String DLX_EXCHANGE = "dlx.exchange";
	public static final String DLX_QUEUE = "dlx.queue";
	public static final String DLX_ROUTING_KEY = "dlx.routing.key";
	
	/** 最大重试次数 */
	public static final int MAX_CONSUMER_COUNT = 5; 
	/** 递增时的基本常量 */
	public static final int BASE_NUM = 2;
	/** 空的字符串 */
	public static final String BLANK_STR = "";



}
