# auction

> 역경매 서비스의 **메인 애플리케이션 서버**입니다.
> 경매·입찰·리뷰 등 핵심 도메인 CRUD와 함께 AI 상담 챗봇, 유저간 실시간 채팅, 모니터링을 담당합니다.

---

## 처리 흐름

### 경매 상태 자동화

```
EventBridge Scheduler
        │  (경매 시작/종료 5분 전 트리거)
        ▼
   auction-lambda
        │  RDS 상태 변경 + Redis Pub/Sub 발행
        ▼
  Redis auction-events
        │
        ▼
auction (메인 서버) ── Elasticsearch 도큐먼트 동기화
                   └── pgvector 임베딩 동기화 (낙찰 시)
```

### AI 상담 챗봇

```
클라이언트
    │  POST /api/chat/rooms/{roomId}/messages
    ▼
AiController (SSE text/event-stream)
    │
    ▼
AiService ── Redis 컨텍스트 캐시 조회 (최근 N개 메시지)
    │
    ▼
ChatClient (DeepSeek V3)
    │  Tool Calling 판단
    ├── AuctionTools (PostgreSQL 조회)
    └── RAG (pgvector 유사도 검색)
    │
    ▼
SSE 이벤트 스트리밍: TOKEN → DONE (첫 메시지 시 TOPIC 포함)
```

### 도메인 상태 흐름

```
READY ──► ACTIVE ──► DONE   (낙찰)
  │              └──► NO_BID (유찰)
  └──► CANCELLED (READY 상태 + 시작 10분 전까지)
```

---

## 주요 기능

### 1. 경매 (Auction)

- EventBridge Scheduler로 시작/종료 스케줄 자동 등록
- Elasticsearch + PostgreSQL 이중 검색 (Nori 한국어 형태소 분석), ES 장애 시 PostgreSQL fallback
- `max_price` API 응답 노출 금지 (판매자에게 최대 희망가 숨김)

### 2. 입찰 (Bid)

- Redisson 분산락으로 동시 입찰 경합 방지 (순차 처리)
- 현재 최저가보다 낮은 가격만 등록 가능, `max_price` 초과 불가
- 경매 상태 enum 외에 종료 시각과 현재 시각 비교로 이중 검증

### 3. AI 상담 챗봇

SSE 스트리밍으로 실시간 응답을 수신하며, Tool Calling과 RAG를 통해 DB의 실제 데이터를 기반으로 답변합니다.

| Tool | 역할 | 방식 |
|------|------|------|
| `getBidsByAuctionId` | 경매 입찰 현황 · 최저가 · 경쟁 분석 | PostgreSQL |
| `getRecentAuctionResults` | 상품 시세 · 낙찰 이력 조회 | PostgreSQL |
| `getSellerStats` | 판매자 종합 신뢰도 (낙찰 횟수, 평균 평점) | PostgreSQL |
| `getSellerReviewInsights` | 판매자 후기 키워드 분석 | RAG (pgvector) |
| `getMyAuctions` | 내가 등록한 경매 현황 · 현재 최저가 | PostgreSQL |
| `getMyBids` | 내가 입찰한 경매 현황 · 1위 여부 | PostgreSQL |
| `getAuctionStatsByCategory` | 카테고리별 낙찰 통계 · 시세 분석 | PostgreSQL |
| `searchAuctionDescriptions` | 낙찰 경매 상품 설명 의미 검색 | RAG (pgvector) |

**RAG 구현 포인트**

- 후기 RAG: HyDE(가상 문서 생성) + Contextual Retrieval + 감성 방향 score 필터
- 경매 설명 RAG: Lambda Pub/Sub 파이프라인으로 낙찰 시 자동 임베딩 저장

### 4. 유저간 실시간 채팅

- 낙찰 후 구매자-판매자 1:1 채팅방 자동 생성 (Redis Pub/Sub 이벤트 수신)
- WebSocket + STOMP + SockJS, Redis Pub/Sub 브로커로 다중 서버 인스턴스 간 메시지 공유

### 5. 리뷰 (Review)

- 낙찰 경매 당사자(구매자/판매자)만 작성 가능, 복합 유니크 제약 `(reviewer_id, reviewee_id, auction_id)`
- S3 Presigned PUT URL 발급 (10분 TTL) → CloudFront URL로 반환
- 리뷰 저장 시 pgvector 임베딩 자동 연동

### 6. 모니터링

- Spring Actuator로 JVM 메트릭 노출 (포트 9001, 앱 포트 8080과 분리)
- Grafana Alloy 사이드카가 scrape → Amazon Managed Prometheus(AMP) remote_write
- AWS IAM 역할 기반 SigV4 인증 (Access Key 없음)
- Amazon Managed Grafana(AMG)에서 JVM, HTTP 요청 현황 실시간 시각화

---

## 패키지 구조

```
com.example.auction/
├── common/
│   ├── config/       # Security, Redis, QueryDSL, WebSocket, Spring AI, pgvector
│   ├── dto/          # BaseResponse, PageResponse
│   ├── entity/       # BaseEntity (생성일, 수정일, 삭제일)
│   └── exception/    # GlobalExceptionHandler, ServiceErrorException
│
└── domain/
    ├── auth/         # 회원가입, 로그인, 토큰 갱신, 소셜 로그인
    ├── user/         # 마이페이지, 비밀번호 변경, 회원 탈퇴
    ├── auction/      # 경매 CRUD, 검색 (ES + PostgreSQL), EventBridge 연동
    │   ├── result/   # 낙찰 결과 조회
    │   └── search/   # Elasticsearch 도큐먼트, 검색 서비스
    ├── bid/          # 입찰 생성 (분산락), 입찰 조회
    ├── category/     # 카테고리 트리 조회, 관리자 CRUD
    ├── review/       # 리뷰 CRUD, S3 Presigned URL, RAG 임베딩 연동
    ├── chat/         # AI 채팅방 CRUD, 메시지 목록 (커서 페이징), Redis 컨텍스트
    ├── ai/           # SSE 스트리밍, Tool Calling 8종, RAG 2종, 임베딩 서비스
    ├── userchat/     # 유저간 실시간 채팅 (WebSocket + Redis Pub/Sub)
    └── notification/ # Redis Pub/Sub 알림 발행
```

---

## 환경변수

| 변수명 | 설명 | 필수 |
|--------|------|------|
| `POSTGRES_URL` | PostgreSQL JDBC URL | ✅ |
| `POSTGRES_USERNAME` | DB 사용자명 | ✅ |
| `POSTGRES_PASSWORD` | DB 비밀번호 | ✅ |
| `REDIS_HOST` | Redis 호스트 | ✅ |
| `ELASTICSEARCH_URIS` | Elasticsearch URI | ✅ |
| `ELASTICSEARCH_PASSWORD` | Elasticsearch 비밀번호 | ✅ |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) | ✅ |
| `ADMIN_SECRET_KEY` | 관리자 계정 생성 키 | ✅ |
| `DEEPSEEK_API_KEY` | DeepSeek V3 채팅 API 키 | ✅ |
| `OPENAI_API_KEY` | OpenAI 임베딩 API 키 | ✅ |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 | ✅ |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | Kakao OAuth2 | ✅ |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | Naver OAuth2 | ✅ |
| `LAMBDA_ARN` | 경매 상태 자동화 Lambda ARN | ❌ |
| `ROLE_ARN` | EventBridge 실행 역할 ARN | ❌ |
| `S3_BUCKET_NAME` | 리뷰 이미지 S3 버킷명 | ❌ |
| `CLOUDFRONT_DOMAIN` | 리뷰 이미지 CDN 도메인 | ❌ |

> AWS 관련 변수(`LAMBDA_ARN`, `S3_BUCKET_NAME` 등)는 로컬 실행 시 설정하지 않아도 됩니다.

---

## 로컬 실행 방법

### 사전 요구사항

- Java 21
- Docker / Docker Compose

### 1단계 — 환경변수 파일 생성

프로젝트 루트에 `.env` 파일을 생성합니다.

```dotenv
POSTGRES_DATABASE=auction
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_password
POSTGRES_URL=jdbc:postgresql://localhost:5432/auction

REDIS_HOST=localhost

ELASTICSEARCH_PASSWORD=your_es_password
ELASTICSEARCH_URIS=http://localhost:9200

JWT_SECRET=your_jwt_secret_key_min_32_chars
ADMIN_SECRET_KEY=your_admin_secret_key

GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

DEEPSEEK_API_KEY=your_deepseek_api_key
OPENAI_API_KEY=your_openai_api_key
```

### 2단계 — 인프라 실행

```bash
docker compose up -d postgres redis elasticsearch
```

> Elasticsearch 기동에 약 30초~1분 소요됩니다. `docker compose ps`로 상태를 확인하세요.

### 3단계 — 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

서버가 뜨면 `http://localhost:8080` 으로 접근할 수 있습니다.

---

## 기술 스택

<div align="center">

<img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=openjdk&logoColor=white">
<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
<img src="https://img.shields.io/badge/Spring%20AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/QueryDSL-0078D4?style=for-the-badge&logo=github&logoColor=white">
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
<img src="https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white">
<img src="https://img.shields.io/badge/Amazon%20ECS-FF9900?style=for-the-badge&logo=amazonecs&logoColor=white">
<img src="https://img.shields.io/badge/Amazon%20RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white">
<img src="https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white">
<img src="https://img.shields.io/badge/CloudFront-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white">
<img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white">
<img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white">
<img src="https://img.shields.io/badge/Github%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">

</div>