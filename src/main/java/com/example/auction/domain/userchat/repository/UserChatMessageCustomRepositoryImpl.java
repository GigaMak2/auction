package com.example.auction.domain.userchat.repository;

import com.example.auction.domain.userchat.entity.UserChatMessage;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

import static com.example.auction.domain.userchat.entity.QUserChatMessage.userChatMessage;

@Repository
@RequiredArgsConstructor
public class UserChatMessageCustomRepositoryImpl implements UserChatMessageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserChatMessage> findByCursor(Long roomId, Long cursor, int size) {
        List<UserChatMessage> messages = queryFactory
                .selectFrom(userChatMessage)
                .where(
                        userChatMessage.roomId.eq(roomId),
                        cursor != null ? userChatMessage.id.lt(cursor) : null
                )
                .orderBy(userChatMessage.id.desc())
                .limit(size)
                .fetch();
        Collections.reverse(messages);
        return messages;
    }
}
