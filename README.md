# 用法

## 更新

1. 自定义注解插入发送切面
2. 抽象消息存储
3. 扩展元消息

## 项目结构说明  
coolmq为实际包  
microservice-demo们为spring-boot集成demo  

## 使用说明  
1. maven引入coolmq依赖  
2. 在项目启动中配置包扫描:@SpringBootApplication(scanBasePackages= {"com.coolmq.amqp.config"}),以自动装配项目bean  
3. 声明队列: BizQueueConfig.java  
4. 声明消息接受者: TransMessageListener.java  
5. provider中加入 @TransMessage包裹业务操作即可
6. 代码使用示例请参考microservice-message-demo

## 视频教程
[《Spring Cloud分布式事务(一):Rabbitmq基础》](https://segmentfault.com/l/1500000015339065)  
[《Spring Cloud分布式事务(二):原理与源码》](https://segmentfault.com/l/1500000012729662)  
[《Spring Cloud分布式事务(三):深入补偿机制》](https://segmentfault.com/l/1500000016673126)

# coolmq 用rabbitmq解决分布式事务
传统的事务解决方案，例如TCC，都太消耗资源，而rabbitmq用两阶段确认确保了消息只要发送，就能送达。本方案是基于Spring-Boot Amqp，已经在生产上部署实践，可用于支付等跨服务调用的业务情况

## 一 两阶段确认
### 1 发送确认
发送确认用来确保消息是否已送达消息队列。

发送端会维护一个ack回调，来监听消息是否送达消息队列。对于无法被路由的消息，一旦没法找到一个队列来消费它，就会触发确认无法消费，此时ack＝false。对于可以被路由的消息，当消息被queue接受时，会触发ack=true;对于设置了持久化（persistent）的消息，当消息成功的持久化到硬盘上才会触发；对于设置了镜像（mirror）的消息，那么是当所有的mirror接受到这个消息。

### 2 消费确认（Delivery Acknowledgements）
消费者若成功的消费了消息，同样会给消息队列返回一个消费成功的确认消息。
一旦有消费者成功注册到相应的消息服务，消息将会被消息服务通过basic.deliver推（push）给消费者，此时消息会包含一个deliver tag用来唯一的标识消息。如果此时是手动模式，就需要手动的确认消息已经被成功消费，否则消息服务将会重发消息（因为消息已经持久化到了硬盘上，所以无论消息服务是不是可能挂掉，都会重发消息）。而且必须确认，无论是成功或者失败，否则会引起非常严重的问题

## 二 可能出现的异常情况
我们来看看可能发送异常的四种
### 1 直接无法到达消息服务
网络断了，抛出异常，业务直接回滚即可。如果出现connection closed错误，直接增加 connection数即可
   
   connectionFactory.setChannelCacheSize(100);
### 2 消息已经到达服务器，但返回的时候出现异常
rabbitmq提供了确认ack机制，可以用来确认消息是否有返回。因此我们可以在发送前在db中(内存或关系型数据库)先存一下消息，如果ack异常则进行重发

    /**confirmcallback用来确认消息是否有送达消息队列*/     
    rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
        if (!ack) {
            //try to resend msg
        } else {
            //delete msg in db
        }
    });
     /**若消息找不到对应的Exchange会先触发returncallback */
     rabbitTemplate.setReturnCallback((message, replyCode, replyText, tmpExchange, tmpRoutingKey) -> {
        try {
            Thread.sleep(Constants.ONE_SECOND);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("send message failed: " + replyCode + " " + replyText);
        rabbitTemplate.send(message);
    });
    
如果消息没有到exchange,则confirm回调,ack=false
如果消息到达exchange,则confirm回调,ack=true
但如果是找不到exchange，则会先触发returncallback

### 3 消息送达后，消息服务自己挂了
如果设置了消息持久化，那么ack= true是在消息持久化完成后，就是存到硬盘上之后再发送的，确保消息已经存在硬盘上，万一消息服务挂了，消息服务恢复是能够再重发消息

### 4 未送达消费者
消息服务收到消息后，消息会处于"UNACK"的状态，直到客户端确认消息

      channel.basicQos(1); // accept only one unack-ed message at a time (see below)
      final Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
          String message = new String(body, "UTF-8");

          System.out.println(" [x] Received '" + message + "'");
          try {
            doWork(message);
          } finally {
             //确认收到消息
            channel.basicAck(envelope.getDeliveryTag(), false);
          }
        }
      };
    boolean autoAck = false;
    channel.basicConsume(TASK_QUEUE_NAME, autoAck, consumer);

### 5 确认消息丢失
消息返回时假设确认消息丢失了，那么消息服务会重发消息。注意，如果你设置了autoAck= false，但又没应答 channel.baskAck也没有应答 channel.baskNack，那么会导致非常严重的错误：消息队列会被堵塞住，可参考http://blog.sina.com.cn/s/blog_48d4cf2d0102w53t.html 所以，无论如何都必须应答

### 6 消费者业务处理异常
消息监听接受消息并处理，假设抛异常了，第一阶段事物已经完成，如果要配置回滚则过于麻烦，即使做事务补偿也可能事务补偿失效的情况，所以这里可以做一个重复执行，比如guava的retry，设置一个指数时间来循环执行，如果n次后依然失败，发邮件、短信，用人肉来兜底。
参考：http://blog.csdn.net/reviveds...
