package demo;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConverter;

import com.coolmq.amqp.listener.AbstractMessageListener;

public class BizMessageListener extends AbstractMessageListener  {

	@Override
	public void receiveMessage(Message message, MessageConverter messageConverter) {
		Object bizObj = messageConverter.fromMessage(message);
		//do your biz
	}


}
