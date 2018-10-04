package com.itmuch.cloud.study.controller;

import com.itmuch.cloud.study.provider.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MessageProviderController {

 
  /** test if can get value from config center **/
//  @Value("${spring.rabbitmq.host}")
//  private String rabbitmqAddress;

  @Autowired
  MessageSender messageSender;


//  @Autowired
//  private RabbitSender rabbitSender;

  Logger logger = LoggerFactory.getLogger(getClass());


//  @GetMapping("testconfig")
//  public String testconfig() throws Exception {
//	  return rabbitmqAddress;
//  }


  @GetMapping("testtransmsg")
  public String testMessageWithAnnotation() throws Exception {
        messageSender.transSend();
		return "sucess";
  }

  @GetMapping("testsimplemq")
  @Transactional
  public String testSimpleSender() throws Exception {
	    /** 生成一个发送对象 */
		messageSender.send();
		//do some biz
		return "sucess";
  }

}
