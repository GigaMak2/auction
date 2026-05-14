package com.example.auction.common.config;

import com.example.auction.domain.ai.listener.AuctionEmbedListener;
import com.example.auction.domain.userchat.listener.UserChatMessageListener;
import com.example.auction.domain.userchat.listener.UserChatRoomCreationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    public static final String AUCTION_EVENTS_CHANNEL = "auction-events";
    public static final String USER_CHAT_CHANNEL_PATTERN = "user-chat-room:*";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            AuctionEmbedListener auctionEmbedListener,
            UserChatRoomCreationListener userChatRoomCreationListener,
            UserChatMessageListener userChatMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(auctionEmbedListener, new ChannelTopic(AUCTION_EVENTS_CHANNEL));
        container.addMessageListener(userChatRoomCreationListener, new ChannelTopic(AUCTION_EVENTS_CHANNEL));
        container.addMessageListener(userChatMessageListener, new PatternTopic(USER_CHAT_CHANNEL_PATTERN));
        return container;
    }

}
