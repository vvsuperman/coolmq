package demo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;

import com.coolmq.amqp.util.MQConstants;

public class BizQueueConfig {
	
	/**
     * 1 首先声明要使用哪个交换机
     */
	@Bean
    public DirectExchange businessExchange() {
        return new DirectExchange(MQConstants.BUSINESS_EXCHANGE);
    }

	 /**
     * 2 queue的名称bizQueue，以及一些参数配置
     */
   @Bean
   public Queue bizQueue() {
	   Map<String, Object> arguments = new HashMap<String, Object>();
	   /**配置的死信队列*/
	   arguments.put("x-dead-letter-exchange", MQConstants.DLX_EXCHANGE);
	   arguments.put("x-dead-letter-routing-key", MQConstants.DLX_ROUTING_KEY);
	   /**消息被确认前的最大等待时间*/
	   arguments.put("x-message-ttl", 60000);
	   /**消息队列的最大大长度*/
	   arguments.put("x-max-length", 300);
	   return new Queue("biz_queue_name",true,false,false,arguments);
   }
   
   /**
    * 3 绑定bizQueue到相应的key
    * 
    */
   @Bean
   public Binding bizBinding() {
       return BindingBuilder.bind(bizQueue()).to(businessExchange())
               .with("biz_key_name");
   }
    
   /**
    * 4 最后声明一个listener，用来监听
    */
   @Bean
   public SimpleMessageListenerContainer bizListenerContainer(ConnectionFactory connectionFactory, 
   		BizMessageListener bizMessageListener) {
   	
       SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
       container.setQueues(bizQueue());
       container.setExposeListenerChannel(true);
       container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
       container.setMessageListener(bizMessageListener);
       /** 设置消费者能处理消息的最大个数 */
       container.setPrefetchCount(100);
       return container;
   }
}
