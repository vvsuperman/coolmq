package com.coolmq.amqp.listener;

import cn.com.spdbccc.hotelbank.base.constant.MQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

/**
 * <p><b>Description:</b> 死信队列消息监听<p>
 * <b>Company:</b> Newtouch
 *
 * @author created by hongda at 22:49 on 2017-10-23
 * @version V0.1
 */
@Component
public class DeadLetterMessageListener implements ChannelAwareMessageListener {
	private Logger logger = LoggerFactory.getLogger(DeadLetterMessageListener.class);

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	/*@Autowired
	private DeadLetterMessageMapper deadLetterMessageMapper;

	@Autowired
	private MailServiceImpl mailService;*/
	
	// 收件人
	/*@Value("${recipient.email.address}")
	private String emailRecipient;*/

	/**
	 * Callback for processing a received Rabbit message.
	 * <p>Implementors are supposed to process the given Message,
	 * typically sending reply messages through the given Session.
	 * @param message the received AMQP message (never <code>null</code>)
	 * @param channel the underlying Rabbit Channel (never <code>null</code>)
	 * @throws Exception Any.
	 */
	@Override
	public void onMessage(Message message, Channel channel) throws Exception {
		MessageProperties messageProperties = message.getMessageProperties();
        // 消息体
		String messageBody = new String(message.getBody());

		logger.warn("dead letter message：{} | tag：{}", messageBody, message.getMessageProperties().getDeliveryTag());

		/*// 入库
		insertRecord(logKey, message);
		// 发邮件
		sendEmail(logKey, messageProperties.getMessageId(), messageBody);*/

		channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

		redisTemplate.opsForHash().delete(MQConstants.MQ_CONSUMER_RETRY_COUNT_KEY, messageProperties.getMessageId());
	}

	/**
	 * 入库
	 *//*
	private void insertRecord(String logKey, Message message) {
		try {
			MessageProperties msgProp = message.getMessageProperties();
			DeadLetterMessageVo entity = new DeadLetterMessageVo();
			entity.setId(Identity.asyncUUID());
			entity.setMsgId(msgProp.getMessageId());
			entity.setMsgBody(new String(message.getBody()));
			entity.setDeliverTag("" + msgProp.getDeliveryTag());
			// PRODUCER为生产,CONSUMER为消费
			entity.setType(StringUtil.isNotBlank(msgProp.getType()) ? msgProp.getType() : Constants.MQ_CONSUMER);
			deadLetterMessageMapper.insert(entity);
			logger.info("{}|死信入库, {}", logKey, entity.toString());
		} catch (Exception e) {
			logger.error("{}|死信入库发生异常,{}", logKey, e.getMessage());
		}
	}

	*//**
	 * 发邮件
	 *//*
	private void sendEmail(String logKey, String msgId, String msgBody) {
		try {
			String subject = "MQ处理异常";
			StringBuilder cnt = new StringBuilder();
			cnt.append("ID:" + msgId + "\n");
			cnt.append("消息体:" + msgBody + "\n");
			cnt.append("出现异常,请及时处理");
			mailService.sendSimpleMail(emailRecipient, subject, cnt.toString());
			logger.info("{}|发送邮件成功!", logKey);
		} catch (Exception e) {
			logger.error("{}|发送邮件----异常----", logKey, e.getMessage());
		}
	}*/
}
