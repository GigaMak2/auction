package com.example.auction.common.config;

import com.example.auction.domain.ai.listener.AuctionEmbedListener;
import com.example.auction.domain.userchat.listener.UserChatRoomCreationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    public static final String AUCTION_EVENTS_CHANNEL = "auction-events";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 레디스-자바 타입 파싱
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json()); // 알아서 타입 변경해줌

        //dto 타입 파싱
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            AuctionEmbedListener auctionEmbedListener,
            UserChatRoomCreationListener userChatRoomCreationListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(auctionEmbedListener, new ChannelTopic(AUCTION_EVENTS_CHANNEL));
        container.addMessageListener(userChatRoomCreationListener, new ChannelTopic(AUCTION_EVENTS_CHANNEL));
        return container;
    }

}
