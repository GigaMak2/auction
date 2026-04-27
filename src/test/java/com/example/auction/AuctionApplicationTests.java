package com.example.auction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.auction.testutils.BaseIntegrationTest;

@SpringBootTest
@ActiveProfiles("test")
class AuctionApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }

}
