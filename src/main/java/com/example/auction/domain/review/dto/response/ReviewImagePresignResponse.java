package com.example.auction.domain.review.dto.response;

// presigned URL 발급 응답 — 업로드용 임시 URL과 DB 저장용 CloudFront URL을 함께 반환
public record ReviewImagePresignResponse(
        String presignedUrl,  // 클라이언트가 S3에 직접 PUT 업로드할 임시 URL (10분 유효)
        String imageUrl       // DB에 저장할 CloudFront 이미지 조회 URL
) {}