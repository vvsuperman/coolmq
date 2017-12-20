package com.coolmq.amqp.util;

public class RabbitMetaMessage {
	String exchange;
	String routingKey;
	String playload;
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
	public String getPlayload() {
		return playload;
	}
	public void setPlayload(String playload) {
		this.playload = playload;
	}

}
