package com.example.auction.domain.user.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.repository.BidRepository;
import com.example.auction.domain.user.dto.UserDetailGetResponse;
import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.dto.UserSearchCondition;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.entity.UserSocialAccount;
import com.example.auction.domain.user.enums.AuthProvider;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    @Transactional(readOnly = true)
    public PageResponse<UserListGetResponse> getUserList(UserSearchCondition condition) {
        Page<UserListGetResponse> userList = userRepository.findUsersWithConditions(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getDeleted(),
                condition.getRole(),
                condition.getEmail()
        );

        return PageResponse.create(userList);
    }

    @Transactional(readOnly = true)
    public UserDetailGetResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        AuthProvider authProvider = userSocialAccountRepository.findByUserId(userId)
                .map(UserSocialAccount::getProvider)
                .orElse(null);

        return new UserDetailGetResponse(
                user.getId(),
                user.getEmail(),
                user.getRating(),
                user.getRole(),
                authProvider,
                user.isDeleted(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }

    @Transactional
    public UserWithdrawResponse forceWithdraw(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        List<Auction> auctions = auctionRepository.findByUserIdAndStatusIn(userId, List.of(AuctionStatus.READY, AuctionStatus.ACTIVE));
        for (Auction auction : auctions) {
            auction.forceCancel();
            List<Bid> bids = bidRepository.findAllByAuctionIdAndStatus(auction.getId(), BidAuctionStatus.ACTIVE);
            for (Bid bid : bids) {
                bid.updateStatus(BidAuctionStatus.CANCELLED);
            }
        }

        List<Bid> bids = bidRepository.findAllByUserIdAndStatus(userId, BidAuctionStatus.ACTIVE);
        for (Bid bid : bids) {
            bid.updateStatus(BidAuctionStatus.CANCELLED);
        }

        user.delete();
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        return new UserWithdrawResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }
}
