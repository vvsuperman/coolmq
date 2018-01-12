package com.coolmq.amqp.util;

/**
 * <p><b>Description:</b> 常量类 <p>
 * <b>Company:</b> 
 *
 * @author created by hongda at 22:49 on 2017-10-23
 * @version V0.1
 */
public class RabbitMetaMessage {
	String exchange;
	String routingKey;
	boolean autoTrigger;
	boolean returnCallback;
	Object payload;
	
	public Object getPayload() {
		return payload;
	}
	public void setPayload(Object payload) {
		this.payload = payload;
	}
	public boolean isReturnCallback() {
		return returnCallback;
	}
	public void setReturnCallback(boolean returnCallback) {
		this.returnCallback = returnCallback;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public String getRoutingKey() {
		return routingKey;
	}
	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}
	public boolean isAutoTrigger() {
		return autoTrigger;
	}
	public void setAutoTrigger(boolean autoTrigger) {
		this.autoTrigger = autoTrigger;
	}
	
	
	

}
