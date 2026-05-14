package com.example.auction.common.config;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.domain.userchat.repository.UserChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final UserChatRoomRepository userChatRoomRepository;

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

            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("auth", auth);
            } else {
                throw new IllegalStateException("WebSocket 세션 attributes를 사용할 수 없습니다");
            }

        } else if (StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            if (accessor.getSessionAttributes() != null) {
                UsernamePasswordAuthenticationToken auth =
                        (UsernamePasswordAuthenticationToken) accessor.getSessionAttributes().get("auth");
                if (auth != null) {
                    accessor.setUser(auth);
                }
            }

            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/sub/chat/")) {
                String roomIdStr = destination.substring("/sub/chat/".length());
                Long roomId;
                try {
                    roomId = Long.parseLong(roomIdStr);
                } catch (NumberFormatException e) {
                    throw new AccessDeniedException("유효하지 않은 채팅방 경로입니다: " + destination);
                }
                UsernamePasswordAuthenticationToken auth =
                        (UsernamePasswordAuthenticationToken) accessor.getUser();
                if (auth == null) {
                    throw new AccessDeniedException("인증 정보가 없습니다");
                }
                Long userId = ((CustomUserDetails) auth.getPrincipal()).getUserId();

                if (!userChatRoomRepository.existsByIdAndMember(roomId, userId)) {
                    throw new AccessDeniedException("채팅방 접근 권한이 없습니다");
                }
            }
        }

        return message;
    }
}