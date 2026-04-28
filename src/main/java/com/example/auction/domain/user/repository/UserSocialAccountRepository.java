package com.example.auction.domain.user.repository;

import ch.qos.logback.core.testUtil.MockInitialContext;
import com.example.auction.domain.user.entity.UserSocialAccount;
import com.example.auction.domain.user.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {
    Optional<UserSocialAccount> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<UserSocialAccount> findByUserId(Long userId);
}
