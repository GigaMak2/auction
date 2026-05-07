package com.example.auction.common.config;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.config.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            String token = jwtProvider.resolveToken(bearerToken);

            if (token == null || !jwtProvider.validateAccessToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다");
            }

            Long userId = jwtProvider.getUserId(token);
            String role = jwtProvider.getRole(token);

            CustomUserDetails userDetails = new CustomUserDetails(userId, role);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            accessor.setUser(auth);

            // 세션 attributes에 저장
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("auth", auth);
            }

        } else if (StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            // SEND/SUBSCRIBE 시 세션 attributes에서 복원
            if (accessor.getSessionAttributes() != null) {
                UsernamePasswordAuthenticationToken auth =
                        (UsernamePasswordAuthenticationToken) accessor.getSessionAttributes().get("auth");
                if (auth != null) {
                    accessor.setUser(auth);
                }
            }
        }

        return message;
    }
}