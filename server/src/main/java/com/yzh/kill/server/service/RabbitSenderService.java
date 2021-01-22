package com.yzh.kill.server.service;

import com.yzh.kill.model.dto.KillSuccessUserInfo;
import com.yzh.kill.model.mapper.ItemKillSuccessMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Rabbit发送方service
 */
@Service
public class RabbitSenderService {

    public static final Logger log = LoggerFactory.getLogger(RabbitSenderService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Environment env;

    @Autowired(required = false)
    private ItemKillSuccessMapper itemKillSuccessMapper;

    /**
     * 秒杀成功异步发送邮件通知消息，进发送队列
     *
     * @param orderNo 订单编号
     */
    public void sendKillSuccessEmailMsg(String orderNo) {
        log.info("秒杀成功异步发送邮件通知消息-ready to send message：{}", orderNo);
        try {
            if (StringUtils.isNotBlank(orderNo)) {
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderNo);
                if (info != null) {
                    // rabbitmq发送消息的逻辑
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    // 将消息发送到交换机
                    rabbitTemplate.setExchange(env.getProperty("mq.kill.item.success.email.exchange"));
                    // 设置该消息的routingKey
                    rabbitTemplate.setRoutingKey(env.getProperty("mq.kill.item.success.email.routing.key"));

                    // 将info充当消息发送至队列
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            // 获取消息属性
                            MessageProperties messageProperties = message.getMessageProperties();
                            // 保证消息可靠性，进行持久化
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            // 设置消息头，指定确切类型，消费者那里直接用对象接收
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("秒杀成功异步发送邮件通知消息-发生异常，消息为：{}", orderNo, e.fillInStackTrace());
        }
    }

    /**
     * 秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单
     * 死信队列，延迟处理消息
     *
     * @param orderCode
     */
    public void sendKillSuccessOrderExpireMsg(final String orderCode) {
        try {
            if (StringUtils.isNotBlank(orderCode)) {
                // 将对象信息充当消息进行发送
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderCode);
                if (info != null) {
                    // 消息在传输过程中的传输格式
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    // 设定交换机
                    rabbitTemplate.setExchange(env.getProperty("mq.kill.item.success.kill.dead.prod.exchange"));
                    // 设定路由key
                    rabbitTemplate.setRoutingKey(env.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
                    // 发送消息
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties mp = message.getMessageProperties();
                            // 消息持久化，保证消息可靠传输
                            mp.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            // 消息头部信息
                            mp.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
                            // 动态设置TTL(Time To Live,为了测试方便，暂且设置10s)，通过更改配置文件
                            // 超过设置的时间就将死信队列的消息放到真正的死信队列中
                            mp.setExpiration(env.getProperty("mq.kill.item.success.kill.expire"));
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单-发生异常，消息为：{}", orderCode, e.fillInStackTrace());
        }
    }
}
