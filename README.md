# coolmq 用rabbitmq解决分布式事务
我们在rabbitmq上肉身实战了一下可靠消息，通常事务的两阶段提交太重，因此用，rabbitmq实现如下功能

发送消息到消息服务
消息队列将消息发送给监听
消息监听接受并处理消息

我们来看看可能发送异常的四种
## 1 直接无法到达消息服务
网络断了，抛出异常，业务直接回滚即可。如果出现connection closed错误，直接增加 connection数即可

connectionFactory.setChannelCacheSize(100);
## 2 消息已经到达服务器，但返回的时候出现异常
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

## 3 消息送达后，消息服务自己挂了
如果设置了消息持久化，那么ack= true是在消息持久化完成后，就是存到硬盘上之后再发送的，确保消息已经存在硬盘上，万一消息服务挂了，消息服务恢复是能够再重发消息

## 4 未送达消费者
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

## 5 确认消息丢失
消息返回时假设确认消息丢失了，那么消息服务会重发消息。注意，如果你设置了autoAck= false，但又没应答 channel.baskAck也没有应答 channel.baskNack，那么会导致非常严重的错误：消息队列会被堵塞住，可参考http://blog.sina.com.cn/s/blog_48d4cf2d0102w53t.html 所以，无论如何都必须应答

## 6 消费者业务处理异常
消息监听接受消息并处理，假设抛异常了，第一阶段事物已经完成，如果要配置回滚则过于麻烦，即使做事务补偿也可能事务补偿失效的情况，所以这里可以做一个重复执行，比如guava的retry，设置一个指数时间来循环执行，如果n次后依然失败，发邮件、短信，用人肉来兜底。
参考：http://blog.csdn.net/reviveds...
