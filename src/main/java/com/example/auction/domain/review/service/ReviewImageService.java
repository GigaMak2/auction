package com.example.auction.domain.review.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.review.dto.response.ReviewImagePresignResponse;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.cloudfront.domain}")
    private String cloudfrontDomain;

    public ReviewImagePresignResponse generatePresignedUrl(Long userId, String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceErrorException(ReviewErrorEnum.REVIEW_IMAGE_INVALID_TYPE);
        }

        // image/jpeg → jpg, image/png → png, image/webp → webp 로 확장자 변환
        String extension = contentType.substring(contentType.indexOf('/') + 1).replace("jpeg", "jpg");
        String key = "reviews/" + userId + "/" + UUID.randomUUID() + "." + extension;

        String presignedUrl = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(PRESIGN_DURATION)
                        .putObjectRequest(r -> r
                                .bucket(bucket)
                                .key(key)
                                .contentType(contentType)
                        )
                        .build()
        ).url().toString();

        // trailing slash 제거 후 CloudFront URL 조합 — 이중 슬래시 방지
        String domain = cloudfrontDomain.endsWith("/")
                ? cloudfrontDomain.substring(0, cloudfrontDomain.length() - 1)
                : cloudfrontDomain;
        return new ReviewImagePresignResponse(presignedUrl, domain + "/" + key);
    }
}
