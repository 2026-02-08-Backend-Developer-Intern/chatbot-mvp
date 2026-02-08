# AI Chatbot MVP

3시간 안에 돌아가는 챗봇 API를 만드는 과제. Kotlin + Spring Boot 기반.

---

## 실행 방법

### Docker (권장)

```bash
docker-compose up --build
```

PostgreSQL 15.8 + Spring Boot가 한 번에 뜬다. `http://localhost:8080/swagger-ui.html`에서 API 확인 가능

### 로컬 (H2)

```bash
./gradlew bootRun
```

H2 인메모리 DB로 바로 실행된다. H2 콘솔은 `http://localhost:8080/h2-console`

### E2E 테스트

```bash
chmod +x e2e-test.sh
./e2e-test.sh
```

전체 API 플로우를 curl로 돌려 본다. 회원가입부터 CSV 다운로드까지 한 번에 검증

### 시딩 계정

| 이메일 | 비밀번호 | 권한 |
|---|---|---|
| admin@chatbot.com | admin1234 | ADMIN |
| user@chatbot.com | user1234 | MEMBER |

---

## 기술 스택

- Kotlin 1.9 + Spring Boot 3.2
- Spring Data JPA (Hibernate)
- Spring Security + JWT
- WebClient (OpenAI API 연동, SSE 스트리밍)
- PostgreSQL 15.8 (Docker) / H2 (로컬)
- Swagger (springdoc-openapi)

---

## 과제 분석

요구사항을 처음 읽었을 때 핵심이 뭔지 추렸다.

**비즈니스 관점에서 이 과제가 원하는 것:**
- 영업 직원이 고객한테 시연할 수 있는 API
- 지금은 MVP지만, 나중에 RAG(문서 학습) 같은 걸 붙일 수 있어야 함
- 고객사 직원이 OpenAI를 직접 쓸 수준은 아님 -> 우리가 래핑해서 제공

여기서 "지속적으로 확장 개발 가능해야 합니다"가 사실상 이 과제의 진짜 채점 포인트라고 판단했다. 구현량보다 구조가 중요하다는 뜻인 것 같다 ?

그래서 전략을 이렇게 잡고:

1. AI 연동 부분은 **인터페이스(AiClient)로 추상화**해서, 구현체를 바꿔 끼울 수 있게 만든다.
2. 시간이 촉박하니 "모든 기능을 다 구현하는 것"보다 "핵심 로직이 깔끔하게 동작하는 것"에 집중한다.
3. 30분 스레드 정책이 이 과제에서 가장 비즈니스 로직다운 부분이니까, 여기에 유닛 테스트를 쓴다.

---

## 설계 의도

### AiClient 인터페이스 분리

```
AiClient (interface)
├── OpenAiClient   → 실제 OpenAI API 호출
├── MockAiClient   → API Key 없을 때 자동 활성화
└── (미래) RagAiClient → RAG 파이프라인 연동
```

`@ConditionalOnExpression`으로 API Key 유무에 따라 구현체가 자동 전환된다. 새로운 AI 프로바이더를 붙이려면 `AiClient`를 구현하는 클래스 하나만 만들면 된다. 기존 코드는 건드릴 필요 없음

### 30분 스레드 정책

Thread 엔티티에 `lastMessageAt`이라는 필드를 따로 뒀다. 이게 핵심인데, 만약 이 필드가 없으면 매 요청마다 Chat 테이블을 JOIN해서 "가장 최근 대화 시각"을 계산해야 한다. Chat이 쌓일수록 느려진다.

`lastMessageAt`을 Thread에 비정규화해두면 `WHERE user_id = ? ORDER BY last_message_at DESC LIMIT 1` 한 방이면 끝. 여기에 복합 인덱스 `(user_id, last_message_at DESC)`를 걸어서 인덱스 스캔으로 처리되게 했다.

### 패키지 구조

```
com.assignment.chatbot
├── global/        공통 (Security, Exception, Config)
├── user/          인증/인가
├── chat/          대화 + 스레드 (핵심 도메인)
├── feedback/      피드백
└── analysis/      관리자 분석/보고
```

도메인형 패키지 구조를 택했다. 계층형(controller/, service/, repository/)보다 도메인별 응집도가 높고, 나중에 모듈 분리나 MSA 전환 시에도 경계가 명확하다.

---

## AI 활용

솔직하게 말하면 Claude를 꽤 썼다. 다만 아무 생각 없이 "다 짜줘" 한 건 아니고, 용도를 분리했다.

**AI한테 맡긴 것:**
- 보일러플레이트 코드 (SecurityConfig, JWT 필터, DTO 클래스 등)
- CSV 생성 같은 단순 유틸리티 로직
- 테스트 스크립트 초안

**내가 직접 검증하고 수정한 것:**
- 스레드 30분 정책의 정확한 경계값 처리 (>= 30분 vs > 30분)
- Feedback unique 제약조건: 처음에 `chat_id`만 걸었다가 요구사항을 다시 읽고 `(chat_id, user_id)` 복합으로 수정. 한 대화에 여러 유저가 각자 하나씩 피드백을 남길 수 있어야 해서.
- `ApiResponse<out T>` 공변성 이슈: Kotlin 제네릭이 기본 invariant라서 `ApiResponse<Nothing>`을 `ApiResponse<ChatResponse>` 자리에 넣으면 컴파일 에러 남. `out` 키워드로 해결.
- `@ConditionalOnProperty`에 `negate` 파라미터가 없는 문제: Spring Boot 문서를 확인하고 `@ConditionalOnExpression`으로 변경.

**어려웠던 점:**
AI가 생성한 코드를 그대로 쓰면 컴파일 에러가 나는 경우가 있었다. Kotlin 특유의 타입 시스템(nullable, variance)이 Java 기반 Spring과 만나면서 생기는 미묘한 불일치가 대부분. 결국 AI 출력물을 코드 리뷰하듯 검증하는 과정이 필수였다.

---

## 가장 어려웠던 기능

### 1. 스트리밍 응답과 DB 저장의 양립

SSE 스트리밍으로 토큰을 하나씩 클라이언트에 보내면서, 동시에 완성된 전체 답변을 DB에 저장해야 한다. 이 두 가지가 서로 다른 타이밍에 일어난다는 게 문제.

해결: Chat 엔티티를 빈 answer로 먼저 저장하고, `Flux`의 `doOnNext`로 토큰을 `StringBuilder`에 누적, `doOnComplete`에서 최종 answer를 DB에 반영하는 방식으로 처리했다.

다만 이 방식에는 한계가 있다. 스트리밍 중간에 클라이언트가 연결을 끊으면 `doOnComplete`가 호출되지 않아 빈 answer가 남을 수 있다. 프로덕션이라면 `doOnCancel`에서도 현재까지 누적된 내용을 저장하는 로직이 필요하다. 3시간 안에는 여기까지만 ..

### 2. 피드백 권한 모델

요구사항을 꼼꼼히 읽어보면 피드백 규칙이 좀 복잡하다:
- MEMBER는 **자기 대화**에만 피드백 가능
- ADMIN은 **모든 대화**에 피드백 가능
- 한 대화에 대해 **유저당 하나**의 피드백만 허용 (다른 유저는 별개)

처음에 `chat_id`에만 UNIQUE를 걸었다가 "하나의 대화에는 서로 다른 사용자들이 생성한 n개의 피드백이 존재할 수 있습니다"를 읽고 `(chat_id, user_id)` 복합 유니크로 수정했다.

---

## API 목록

| Method | Path | 설명 | 권한 |
|---|---|---|---|
| POST | /api/auth/signup | 회원가입 | 공개 |
| POST | /api/auth/login | 로그인 | 공개 |
| POST | /api/chats | 대화 생성 | 인증 |
| POST | /api/chats/stream | 대화 생성 (SSE) | 인증 |
| GET | /api/chats | 대화 목록 (페이지네이션) | 인증 |
| GET | /api/chats/threads/{id} | 스레드 상세 | 인증 |
| DELETE | /api/chats/threads/{id} | 스레드 삭제 | 인증 (본인) |
| POST | /api/feedbacks | 피드백 생성 | 인증 |
| GET | /api/feedbacks | 피드백 목록 (필터/페이지) | 인증 |
| PATCH | /api/feedbacks/{id}/status | 피드백 상태 변경 | ADMIN |
| GET | /api/admin/analysis/daily | 일일 활동 기록 | ADMIN |
| GET | /api/admin/analysis/csv | CSV 보고서 | ADMIN |

---

## 한계점과 개선 방향

3시간이라 못 한 것들:

- **Refresh Token**: 현재 Access Token만 발급. 실제 서비스라면 Refresh Token + 토큰 갈아타기 로직 필요
- **Rate Limiting**: OpenAI API 호출에 대한 요청 제한 없음. 프로덕션이면 Bucket4j 같은 걸 붙여야 한다
- **대화 컨텍스트 길이 제한**: 스레드 내 모든 이전 대화를 OpenAI에 보내는데, 대화가 길어지면 토큰 한도 초과, 슬라이딩 윈도우나 요약 전략 필요
- **테스트 커버리지**: ThreadService 유닛 테스트만 작성. 통합 테스트와 컨트롤러 테스트는 시간 부족으로 생략
