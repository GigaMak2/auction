package com.example.auction.domain.userchat.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.userchat.dto.UserChatMessageListResponse;
import com.example.auction.domain.userchat.dto.UserChatMessageRequest;
import com.example.auction.domain.userchat.dto.UserChatRoomResponse;
import com.example.auction.domain.userchat.entity.UserChatMessage;
import com.example.auction.domain.userchat.entity.UserChatRoom;
import com.example.auction.domain.userchat.exception.UserChatErrorEnum;
import com.example.auction.domain.userchat.publisher.UserChatMessagePublisher;
import com.example.auction.domain.userchat.repository.UserChatMessageRepository;
import com.example.auction.domain.userchat.repository.UserChatRoomRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserChatServiceTest {

    @InjectMocks
    private UserChatService userChatService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserChatRoomRepository userChatRoomRepository;

    @Mock
    private UserChatMessageRepository userChatMessageRepository;

    @Mock
    private UserChatMessagePublisher userChatMessagePublisher;

    private static final Long BUYER_ID  = 1L;
    private static final Long SELLER_ID = 2L;
    private static final Long AUCTION_ID = 10L;
    private static final Long ROOM_ID   = 100L;

    @BeforeEach
    void initTransactionSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }


    // ========================
    // createRoom
    // ========================

    @Test
    @DisplayName("채팅방 생성 성공")
    void createRoom_success() {
        // given
        given(userRepository.findByIdAndDeletedFalse(BUYER_ID)).willReturn(Optional.of(mock(User.class)));
        given(userRepository.findByIdAndDeletedFalse(SELLER_ID)).willReturn(Optional.of(mock(User.class)));
        given(userChatRoomRepository.existsByBuyerIdAndSellerIdAndAuctionId(BUYER_ID, SELLER_ID, AUCTION_ID)).willReturn(false);

        // when
        userChatService.createRoom(BUYER_ID, SELLER_ID, AUCTION_ID);

        // then
        verify(userChatRoomRepository).save(any(UserChatRoom.class));
    }

    @Test
    @DisplayName("채팅방 생성 성공 - 이미 존재하면 저장 안 함 (멱등성)")
    void createRoom_success_alreadyExists() {
        // given
        given(userRepository.findByIdAndDeletedFalse(BUYER_ID)).willReturn(Optional.of(mock(User.class)));
        given(userRepository.findByIdAndDeletedFalse(SELLER_ID)).willReturn(Optional.of(mock(User.class)));
        given(userChatRoomRepository.existsByBuyerIdAndSellerIdAndAuctionId(BUYER_ID, SELLER_ID, AUCTION_ID)).willReturn(true);

        // when
        userChatService.createRoom(BUYER_ID, SELLER_ID, AUCTION_ID);

        // then
        verify(userChatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 구매자 없음")
    void createRoom_fail_buyerNotFound() {
        // given
        given(userRepository.findByIdAndDeletedFalse(BUYER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userChatService.createRoom(BUYER_ID, SELLER_ID, AUCTION_ID))
                .isInstanceOf(ServiceErrorException.class);
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 판매자 없음")
    void createRoom_fail_sellerNotFound() {
        // given
        given(userRepository.findByIdAndDeletedFalse(BUYER_ID)).willReturn(Optional.of(mock(User.class)));
        given(userRepository.findByIdAndDeletedFalse(SELLER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userChatService.createRoom(BUYER_ID, SELLER_ID, AUCTION_ID))
                .isInstanceOf(ServiceErrorException.class);
    }


    // ========================
    // getRooms
    // ========================

    @Test
    @DisplayName("채팅방 목록 조회 성공")
    void getRooms_success() {
        // given
        UserChatRoom room1 = UserChatRoom.of(BUYER_ID, SELLER_ID, AUCTION_ID);
        ReflectionTestUtils.setField(room1, "id", 1L);
        UserChatRoom room2 = UserChatRoom.of(BUYER_ID, SELLER_ID, 20L);
        ReflectionTestUtils.setField(room2, "id", 2L);

        given(userChatRoomRepository.findAllByBuyerIdOrSellerIdOrderByCreatedAtDesc(BUYER_ID, BUYER_ID))
                .willReturn(List.of(room1, room2));

        // when
        List<UserChatRoomResponse> result = userChatService.getRooms(BUYER_ID);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().counterpartId()).isEqualTo(SELLER_ID);
    }

    @Test
    @DisplayName("채팅방 목록 조회 성공 - 채팅방 없음")
    void getRooms_empty() {
        // given
        given(userChatRoomRepository.findAllByBuyerIdOrSellerIdOrderByCreatedAtDesc(BUYER_ID, BUYER_ID))
                .willReturn(List.of());

        // when
        List<UserChatRoomResponse> result = userChatService.getRooms(BUYER_ID);

        // then
        assertThat(result).isEmpty();
    }


    // ========================
    // getMessages
    // ========================

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 없음 (최신부터)")
    void getMessages_success_noCursor() {
        // given
        UserChatRoom room = UserChatRoom.of(BUYER_ID, SELLER_ID, AUCTION_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);

        UserChatMessage msg1 = UserChatMessage.of(ROOM_ID, BUYER_ID, "안녕");
        ReflectionTestUtils.setField(msg1, "id", 1L);
        UserChatMessage msg2 = UserChatMessage.of(ROOM_ID, SELLER_ID, "안녕하세요");
        ReflectionTestUtils.setField(msg2, "id", 2L);

        given(userChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userChatMessageRepository.findByCursor(ROOM_ID, null, 20)).willReturn(List.of(msg1, msg2));

        // when
        UserChatMessageListResponse result = userChatService.getMessages(ROOM_ID, BUYER_ID, null, 20);

        // then
        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().getFirst().content()).isEqualTo("안녕");
        assertThat(result.nextCursor()).isNull(); // 2개 < size(20) → 다음 페이지 없음
    }

    @Test
    @DisplayName("메시지 목록 조회 성공 - cursor 있음 + 다음 페이지 존재")
    void getMessages_success_withCursorAndHasNext() {
        // given
        UserChatRoom room = UserChatRoom.of(BUYER_ID, SELLER_ID, AUCTION_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);

        List<UserChatMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            UserChatMessage msg = UserChatMessage.of(ROOM_ID, BUYER_ID, "메시지" + i);
            ReflectionTestUtils.setField(msg, "id", (long) i);
            messages.add(msg);
        }

        given(userChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userChatMessageRepository.findByCursor(ROOM_ID, 100L, 20)).willReturn(messages);

        // when
        UserChatMessageListResponse result = userChatService.getMessages(ROOM_ID, BUYER_ID, 100L, 20);

        // then
        assertThat(result.messages()).hasSize(20);
        assertThat(result.nextCursor()).isEqualTo(1L); // 첫 번째 메시지 id
    }

    @Test
    @DisplayName("메시지 목록 조회 실패 - 채팅방 없음")
    void getMessages_fail_roomNotFound() {
        // given
        given(userChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userChatService.getMessages(ROOM_ID, BUYER_ID, null, 20))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserChatErrorEnum.USER_CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("메시지 목록 조회 실패 - 본인 채팅방 아님")
    void getMessages_fail_forbidden() {
        // given
        UserChatRoom room = UserChatRoom.of(BUYER_ID, SELLER_ID, AUCTION_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);

        given(userChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

        // when & then
        assertThatThrownBy(() -> userChatService.getMessages(ROOM_ID, 999L, null, 20))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserChatErrorEnum.USER_CHAT_ROOM_FORBIDDEN.getMessage());
    }


    // ========================
    // sendMessage
    // ========================

    @Test
    @DisplayName("메시지 전송 성공")
    void sendMessage_success() {
        // given
        UserChatRoom room = UserChatRoom.of(BUYER_ID, SELLER_ID, AUCTION_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);

        UserChatMessageRequest request = new UserChatMessageRequest("안녕하세요");

        given(userChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userChatMessageRepository.save(any(UserChatMessage.class))).willAnswer(invocation -> {
            UserChatMessage msg = invocation.getArgument(0);
            ReflectionTestUtils.setField(msg, "id", 1L);
            return msg;
        });

        // when
        userChatService.sendMessage(ROOM_ID, BUYER_ID, request);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        // then
        verify(userChatMessageRepository).save(any(UserChatMessage.class));
        verify(userChatMessagePublisher).publish(any(), any());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 채팅방 없음")
    void sendMessage_fail_roomNotFound() {
        // given
        given(userChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userChatService.sendMessage(ROOM_ID, BUYER_ID, new UserChatMessageRequest("hi")))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserChatErrorEnum.USER_CHAT_ROOM_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 본인 채팅방 아님")
    void sendMessage_fail_forbidden() {
        // given
        UserChatRoom room = UserChatRoom.of(BUYER_ID, SELLER_ID, AUCTION_ID);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);

        given(userChatRoomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));

        // when & then
        assertThatThrownBy(() -> userChatService.sendMessage(ROOM_ID, 999L, new UserChatMessageRequest("hi")))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserChatErrorEnum.USER_CHAT_ROOM_FORBIDDEN.getMessage());
    }
}