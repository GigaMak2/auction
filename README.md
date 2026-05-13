# 실시간 역경매 서비스

![GigaChal.png](GigaChal.png)

> 구매자가 원하는 물품을 등록하면, 판매자들이 경쟁적으로 가격을 낮춰 입찰하는 **역경매(Reverse Auction)** 서비스입니다.

```
구매자  →  경매 등록 (최대 희망가 설정)
판매자  →  입찰 (더 낮은 가격 경쟁)
마감    →  최저가 판매자 자동 낙찰
```

---

## 1. 팀원 소개

<div align="center">
  <table>
    <tbody>
      <tr>
        <td align="center" valign="top" width="25%">
          <a href="https://github.com/hhjo96"><img src="https://wsrv.nl/?url=github.com/hhjo96.png&w=120&h=120&fit=cover&mask=circle" width="120" alt="조현희"/></a><br /><br />
          <b>팀장</b><br />
          <a href="https://github.com/hhjo96">조현희</a><br />
          <sub>──────────</sub><br />
          입찰<br />부하테스트<br />경매 상태 자동화<br />Outbox 패턴 적용
        </td>
        <td align="center" valign="top" width="25%">
          <a href="https://github.com/minky5004"><img src="https://wsrv.nl/?url=github.com/minky5004.png&w=120&h=120&fit=cover&mask=circle" width="120" alt="윤민기"/></a><br /><br />
          <b>팀원</b><br />
          <a href="https://github.com/minky5004">윤민기</a><br />
          <sub>──────────</sub><br />
          AI 상담 챗봇<br />모니터링<br />리뷰 이미지<br />기타 인프라
        </td>
        <td align="center" valign="top" width="25%">
          <a href="https://github.com/imprity"><img src="https://wsrv.nl/?url=github.com/imprity.png&w=120&h=120&fit=cover&mask=circle" width="120" alt="조성진"/></a><br /><br />
          <b>팀원</b><br />
          <a href="https://github.com/imprity">조성진</a><br />
          <sub>──────────</sub><br />
          CI/CD<br />경매 검색<br />경매 조회<br />메인 인프라
        </td>
        <td align="center" valign="top" width="25%">
          <a href="https://github.com/soomin0209"><img src="https://wsrv.nl/?url=github.com/soomin0209.png&w=120&h=120&fit=cover&mask=circle" width="120" alt="김수민"/></a><br /><br />
          <b>팀원</b><br />
          <a href="https://github.com/soomin0209">김수민</a><br />
          <sub>──────────</sub><br />
          인증/인가<br />카테고리<br />리뷰<br />실시간 알림<br />실시간 채팅<br />가상스레드 적용
        </td>
      </tr>
    </tbody>
  </table>
</div>

---

## 2. 프로젝트 개요

**개발 기간**: 2026.04.07 ~ 2026.05.14

### 핵심 차별점 — AI 상담 챗봇

별도 AI 채팅방에서 **GPT-4o** 기반 AI가 실시간 시세, 판매자 신뢰도, 경쟁 입찰 현황 등을 분석해드립니다.
DB의 실제 거래 데이터를 **Tool Calling + RAG**로 실시간 조회하므로 hallucination 없이 플랫폼 데이터를 정확하게 안내합니다.

![User Flow](./UserFlow.png)

---

## 3. 기술 스택

<div align="center">

### **Language**
<img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">

### **Backend**
<img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/QueryDSL-0078D4?style=for-the-badge&logo=github&logoColor=white">

### **Security**
<img src="https://img.shields.io/badge/springsecurity-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=jsonwebtokens&logoColor=white">

### **Real-time**
<img src="https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white"> <img src="https://img.shields.io/badge/SSE-FF6C37?style=for-the-badge&logo=server&logoColor=white">

### **Database**
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white"> <img src="https://img.shields.io/badge/Kibana-005571?style=for-the-badge&logo=kibana&logoColor=white"> <img src="https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white">

### **Message Queue**
<img src="https://img.shields.io/badge/Amazon%20EventBridge-FF4F8B?style=for-the-badge&logo=amazonaws&logoColor=white">

### **AI Model**
<img src="https://img.shields.io/badge/OpenAI%20Embedding-412991?style=for-the-badge&logo=openai&logoColor=white">

### **Infra / Cloud**
<img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20ECS-FF9900?style=for-the-badge&logo=amazonecs&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20ECR-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"> <img src="https://img.shields.io/badge/AWS%20Lambda-FF9900?style=for-the-badge&logo=awslambda&logoColor=white"> <img src="https://img.shields.io/badge/CloudFront-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/Route%2053-8C4FFF?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/ElastiCache-C925D1?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/NAT%20Gateway-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/AWS%20IAM-DD344C?style=for-the-badge&logo=amazonaws&logoColor=white">

### **CI/CD**
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Github%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">

### **Monitoring**
<img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white"> <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white"> <img src="https://img.shields.io/badge/Grafana%20Alloy-F46800?style=for-the-badge&logo=grafana&logoColor=white"> <img src="https://img.shields.io/badge/CloudWatch-FF4F8B?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/K6-7D64FF?style=for-the-badge&logo=k6&logoColor=white">

### **Test**
<img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white"> <img src="https://img.shields.io/badge/Mockito-25A162?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/Testcontainers-9B489A?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20REST%20Docs-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white">

### **Open API**
<img src="https://img.shields.io/badge/Google%20OAuth2-4285F4?style=for-the-badge&logo=google&logoColor=white"> <img src="https://img.shields.io/badge/Kakao%20OAuth2-FFCD00?style=for-the-badge&logo=kakao&logoColor=black"> <img src="https://img.shields.io/badge/Naver%20OAuth2-03C75A?style=for-the-badge&logo=naver&logoColor=white">

### **Collaboration**
<img src="https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=github&logoColor=white"> <img src="https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white"> <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white"> <img src="https://img.shields.io/badge/Draw.io-F08705?style=for-the-badge&logo=diagramsdotnet&logoColor=white"> <img src="https://img.shields.io/badge/ERDCloud-4A90D9?style=for-the-badge&logo=cloud&logoColor=white"> <img src="https://img.shields.io/badge/CodeRabbit-FF4500?style=for-the-badge&logo=rabbit&logoColor=white">

</div>

---

## 4. 아키텍처

![Architecture](./Architecture.png)

---

## 5. ERD

![ERD](./ERD.png)

---

## 6. 패키지 구조

### auction (메인 서버)

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
    ├── auction/      # 경매 CRUD, 검색 (ES + pgvector), EventBridge 연동
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

### 도메인 상태 흐름

```
READY ──► ACTIVE ──► DONE   (낙찰)
  │              └──► NO_BID (유찰)
  └──► CANCELLED (READY 상태 + 시작 10분 전까지)
```

---

## 7. 주요 기능

### 1. 회원 인증
- JWT 기반 Stateless 인증 (Access 30분 / Refresh 7일)
- 소셜 로그인: Google, Kakao, Naver OAuth2
- 로그인 5회 실패 시 5분 계정 잠금, Refresh Token 탈취 시 즉시 무효화

### 2. 경매
- EventBridge Scheduler로 시작/종료 스케줄 자동 등록 및 낙찰/유찰 자동 처리
- Elasticsearch + PostgreSQL 이중 검색 (Nori 한국어 형태소 분석)

### 3. 입찰
- Redisson 분산락으로 동시 입찰 경합 방지
- 현재 최저가보다 낮은 가격만 등록 가능

### 4. 유저간 실시간 채팅
- 낙찰 후 구매자-판매자 1:1 채팅방 자동 생성
- WebSocket + STOMP + SockJS, Redis Pub/Sub으로 다중 서버 인스턴스 간 메시지 공유

### 5. 리뷰
- 낙찰 경매 당사자(구매자/판매자)만 작성 가능, 별점 필수
- S3 Presigned URL로 이미지 업로드, 리뷰 저장 시 pgvector 임베딩 자동 연동

### 6. AI 상담 챗봇

SSE 스트리밍으로 실시간 응답을 수신하며, Tool Calling과 RAG를 통해 DB의 실제 데이터를 기반으로 답변합니다.

| Tool | 역할 |
|------|------|
| `getBidsByAuctionId` | 경매 입찰 현황 · 최저가 · 경쟁 분석 |
| `getRecentAuctionResults` | 상품 시세 · 낙찰 이력 조회 |
| `getSellerStats` | 판매자 종합 신뢰도 (낙찰 횟수, 평균 평점) |
| `getSellerReviewInsights` | 판매자 후기 키워드 분석 (RAG) |
| `getMyAuctions` | 내가 등록한 경매 현황 · 현재 최저가 |
| `getMyBids` | 내가 입찰한 경매 현황 · 1위 여부 |
| `getAuctionStatsByCategory` | 카테고리별 낙찰 통계 · 시세 분석 |
| `searchAuctionDescriptions` | 낙찰 경매 상품 설명 의미 검색 (RAG) |

### 7. CI/CD

GitHub Actions 기반으로 CI는 push/PR 시 자동 빌드 및 테스트, CD는 수동 트리거(커밋 해시 입력)로 ECR push → ECS 무중단 배포까지 진행합니다. AWS 인증은 OIDC(키 없이 역할 기반)로 처리합니다.

---

## 9. 로컬 실행 방법

### 사전 요구사항

- Java 21
- Docker / Docker Compose

### 1단계 — 환경변수 파일 생성

프로젝트 루트에 `.env` 파일을 생성합니다.

```dotenv
# Database
POSTGRES_DATABASE=auction
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_password
POSTGRES_URL=jdbc:postgresql://localhost:5432/auction

# Redis
REDIS_HOST=localhost

# Elasticsearch
ELASTICSEARCH_PASSWORD=your_es_password
ELASTICSEARCH_URIS=http://localhost:9200

# Auth
JWT_SECRET=your_jwt_secret_key_min_32_chars
ADMIN_SECRET_KEY=your_admin_secret_key

# OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# AI
DEEPSEEK_API_KEY=your_deepseek_api_key
OPENAI_API_KEY=your_openai_api_key
```

> AWS 관련 변수(`LAMBDA_ARN`, `S3_BUCKET_NAME`, `CLOUDFRONT_DOMAIN` 등)는 로컬 실행 시 설정하지 않아도 됩니다.

### 2단계 — 인프라 실행

```bash
docker compose up -d postgres redis elasticsearch
```

Elasticsearch는 기동에 약 30초~1분 소요됩니다. 아래 명령으로 정상 기동 여부를 확인하세요.

```bash
docker compose ps
```

### 3단계 — 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

서버가 뜨면 `http://localhost:8080` 으로 접ㅛ근할 수 있습니다.