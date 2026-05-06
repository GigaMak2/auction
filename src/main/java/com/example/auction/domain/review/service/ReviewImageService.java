package com.example.auction.domain.review.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.review.dto.ReviewImagePresignResponse;
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

    // 허용할 이미지 MIME 타입 — Presigned URL 발급 시 요청값 검증에 사용
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    // Presigned URL 유효 시간 — 클라이언트가 이 시간 안에 S3에 업로드해야 함
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

    private final S3Presigner s3Presigner;

    // S3 버킷 이름 — 업로드 대상 버킷
    @Value("${aws.s3.bucket}")
    private String bucket;

    // CloudFront 도메인 — DB에 저장할 이미지 조회 URL의 기반
    @Value("${aws.cloudfront.domain}")
    private String cloudfrontDomain;

    public ReviewImagePresignResponse generatePresignedUrl(Long userId, String contentType) {
        // 허용되지 않은 MIME 타입이면 예외
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ServiceErrorException(ReviewErrorEnum.REVIEW_IMAGE_INVALID_TYPE);
        }

        // image/jpeg → jpg, image/png → png, image/webp → webp 로 확장자 변환
        String extension = contentType.substring(contentType.indexOf('/') + 1).replace("jpeg", "jpg");
        // S3 객체 키 — reviews/{userId}/{uuid}.확장자 형태로 유저별 경로 분리
        String key = "reviews/" + userId + "/" + UUID.randomUUID() + "." + extension;

        // S3에 직접 PUT 업로드할 수 있는 임시 서명 URL 생성
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
