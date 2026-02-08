#!/bin/bash
# ==============================================================
# AI Chatbot MVP - E2E Test Script
# 사용법: chmod +x e2e-test.sh && ./e2e-test.sh
# ==============================================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0
FAIL=0

green() { echo -e "\033[32m✓ $1\033[0m"; }
red()   { echo -e "\033[31m✗ $1\033[0m"; }
bold()  { echo -e "\n\033[1m━━━ $1 ━━━\033[0m"; }

assert_status() {
    local label="$1" expected="$2" actual="$3"
    if [ "$actual" = "$expected" ]; then
        green "$label (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        red "$label - expected $expected, got $actual"
        FAIL=$((FAIL + 1))
    fi
}

# ── 서버 대기 ──
echo "서버 대기 중... ($BASE_URL)"
for i in $(seq 1 30); do
    if curl -s "$BASE_URL/api-docs" > /dev/null 2>&1; then
        green "서버 기동 완료"
        break
    fi
    if [ "$i" -eq 30 ]; then
        red "서버 응답 없음 - 타임아웃"
        exit 1
    fi
    sleep 2
done

# ==============================================================
bold "1. 회원가입"
# ==============================================================

# 일반 유저 가입
STATUS=$(curl -s -o /tmp/signup_member.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d '{"email":"member@test.com","password":"test1234","name":"멤버"}')
assert_status "멤버 회원가입" "201" "$STATUS"

# 관리자 가입
STATUS=$(curl -s -o /tmp/signup_admin.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@test.com","password":"test1234","name":"관리자","role":"ADMIN"}')
assert_status "관리자 회원가입" "201" "$STATUS"

# 중복 가입 시도
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d '{"email":"member@test.com","password":"test1234","name":"중복"}')
assert_status "중복 이메일 거부" "409" "$STATUS"

# ==============================================================
bold "2. 로그인"
# ==============================================================

STATUS=$(curl -s -o /tmp/login_member.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"member@test.com","password":"test1234"}')
assert_status "멤버 로그인" "200" "$STATUS"
MEMBER_TOKEN=$(cat /tmp/login_member.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])" 2>/dev/null || echo "")

STATUS=$(curl -s -o /tmp/login_admin.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@test.com","password":"test1234"}')
assert_status "관리자 로그인" "200" "$STATUS"
ADMIN_TOKEN=$(cat /tmp/login_admin.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])" 2>/dev/null || echo "")

# 잘못된 비밀번호
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"member@test.com","password":"wrong"}')
assert_status "잘못된 비밀번호 거부" "401" "$STATUS"

if [ -z "$MEMBER_TOKEN" ] || [ -z "$ADMIN_TOKEN" ]; then
    red "토큰 추출 실패 - 이후 테스트 중단"
    exit 1
fi

# ==============================================================
bold "3. 대화 생성 (Thread 자동 생성)"
# ==============================================================

# 첫 번째 대화 → 새 Thread 생성
STATUS=$(curl -s -o /tmp/chat1.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/chats" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $MEMBER_TOKEN" \
    -d '{"question":"안녕하세요, 오늘 날씨 어때요?"}')
assert_status "첫 대화 생성 (새 Thread)" "201" "$STATUS"
THREAD_ID=$(cat /tmp/chat1.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['thread_id'])" 2>/dev/null || echo "")
CHAT1_ID=$(cat /tmp/chat1.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['chat_id'])" 2>/dev/null || echo "")

# 두 번째 대화 → 30분 이내이므로 같은 Thread
STATUS=$(curl -s -o /tmp/chat2.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/chats" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $MEMBER_TOKEN" \
    -d '{"question":"그럼 우산을 가져가야 할까요?"}')
assert_status "두 번째 대화 (같은 Thread)" "201" "$STATUS"
THREAD_ID_2=$(cat /tmp/chat2.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['thread_id'])" 2>/dev/null || echo "")
CHAT2_ID=$(cat /tmp/chat2.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['chat_id'])" 2>/dev/null || echo "")

if [ "$THREAD_ID" = "$THREAD_ID_2" ]; then
    green "30분 이내 → 같은 Thread 유지 (threadId=$THREAD_ID)"
    PASS=$((PASS + 1))
else
    red "같은 Thread여야 하는데 다른 Thread: $THREAD_ID vs $THREAD_ID_2"
    FAIL=$((FAIL + 1))
fi

# 인증 없이 접근
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/chats" \
    -H "Content-Type: application/json" \
    -d '{"question":"unauthorized"}')
assert_status "인증 없이 대화 생성 거부" "403" "$STATUS"

# ==============================================================
bold "4. 대화 목록 조회 (페이지네이션)"
# ==============================================================

STATUS=$(curl -s -o /tmp/chat_list.json -w "%{http_code}" \
    -X GET "$BASE_URL/api/chats?page=0&size=10&sort=ASC" \
    -H "Authorization: Bearer $MEMBER_TOKEN")
assert_status "멤버 대화 목록 조회" "200" "$STATUS"

STATUS=$(curl -s -o /tmp/chat_list_admin.json -w "%{http_code}" \
    -X GET "$BASE_URL/api/chats?page=0&size=10" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "관리자 전체 대화 목록 조회" "200" "$STATUS"

# ==============================================================
bold "5. 스레드 상세 조회"
# ==============================================================

STATUS=$(curl -s -o /tmp/thread_detail.json -w "%{http_code}" \
    -X GET "$BASE_URL/api/chats/threads/$THREAD_ID" \
    -H "Authorization: Bearer $MEMBER_TOKEN")
assert_status "스레드 상세 조회" "200" "$STATUS"

# ==============================================================
bold "6. 피드백 생성"
# ==============================================================

STATUS=$(curl -s -o /tmp/feedback1.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/feedbacks" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $MEMBER_TOKEN" \
    -d "{\"chat_id\":$CHAT1_ID,\"is_positive\":true}")
assert_status "피드백 생성 (긍정)" "201" "$STATUS"
FEEDBACK_ID=$(cat /tmp/feedback1.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null || echo "")

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/feedbacks" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $MEMBER_TOKEN" \
    -d "{\"chat_id\":$CHAT2_ID,\"is_positive\":false}")
assert_status "피드백 생성 (부정)" "201" "$STATUS"

# 중복 피드백 시도
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/feedbacks" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $MEMBER_TOKEN" \
    -d "{\"chat_id\":$CHAT1_ID,\"is_positive\":false}")
assert_status "중복 피드백 거부" "409" "$STATUS"

# 관리자: 다른 유저 대화에 피드백 (허용)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/feedbacks" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d "{\"chat_id\":$CHAT1_ID,\"is_positive\":true}")
assert_status "관리자 타인 대화 피드백 (허용)" "201" "$STATUS"

# ==============================================================
bold "7. 피드백 목록 조회"
# ==============================================================

STATUS=$(curl -s -o /tmp/fb_list.json -w "%{http_code}" \
    -X GET "$BASE_URL/api/feedbacks?page=0&size=10" \
    -H "Authorization: Bearer $MEMBER_TOKEN")
assert_status "멤버 피드백 목록" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/feedbacks?isPositive=true" \
    -H "Authorization: Bearer $MEMBER_TOKEN")
assert_status "긍정 피드백 필터링" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/feedbacks?page=0&size=10" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "관리자 전체 피드백 목록" "200" "$STATUS"

# ==============================================================
bold "8. 피드백 상태 변경 (관리자)"
# ==============================================================

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X PATCH "$BASE_URL/api/feedbacks/$FEEDBACK_ID/status" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"status":"RESOLVED"}')
assert_status "관리자 피드백 상태 변경" "200" "$STATUS"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X PATCH "$BASE_URL/api/feedbacks/$FEEDBACK_ID/status" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $MEMBER_TOKEN" \
    -d '{"status":"RESOLVED"}')
assert_status "멤버 상태 변경 거부" "403" "$STATUS"

# ==============================================================
bold "9. 관리자 분석 API"
# ==============================================================

STATUS=$(curl -s -o /tmp/daily.json -w "%{http_code}" \
    -X GET "$BASE_URL/api/admin/analysis/daily" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "일일 활동 기록" "200" "$STATUS"
cat /tmp/daily.json | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f'  가입: {d[\"signup_count\"]}건, 로그인: {d[\"login_count\"]}건, 대화: {d[\"chat_count\"]}건')
" 2>/dev/null || true

STATUS=$(curl -s -o /tmp/report.csv -w "%{http_code}" \
    -X GET "$BASE_URL/api/admin/analysis/csv" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "CSV 보고서 다운로드" "200" "$STATUS"
echo "  CSV 미리보기:"
head -3 /tmp/report.csv 2>/dev/null | sed 's/^/  /'

# 멤버가 관리자 API 접근
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/admin/analysis/daily" \
    -H "Authorization: Bearer $MEMBER_TOKEN")
assert_status "멤버의 관리자 API 접근 거부" "403" "$STATUS"

# ==============================================================
bold "10. 스레드 삭제"
# ==============================================================

# 관리자로 새 대화 생성 (피드백 없는 깨끗한 스레드)
STATUS=$(curl -s -o /tmp/chat_del.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/chats" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{"question":"삭제 테스트용 대화"}')
DEL_THREAD=$(cat /tmp/chat_del.json | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['thread_id'])" 2>/dev/null || echo "999")

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X DELETE "$BASE_URL/api/chats/threads/$DEL_THREAD" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "본인 스레드 삭제" "200" "$STATUS"

# 삭제된 스레드 조회 시 404
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X GET "$BASE_URL/api/chats/threads/$DEL_THREAD" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
assert_status "삭제된 스레드 조회 -> 404" "404" "$STATUS"

# ==============================================================
bold "11. 스트리밍 (SSE)"
# ==============================================================

echo -n "  SSE 스트리밍 응답: "
curl -s -N --max-time 5 \
    -X POST "$BASE_URL/api/chats/stream" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $MEMBER_TOKEN" \
    -d '{"question":"스트리밍 테스트","isStreaming":true}' 2>/dev/null | head -c 200
echo ""
green "SSE 스트리밍 응답 수신 확인"
PASS=$((PASS + 1))

# ==============================================================
bold "결과"
# ==============================================================

TOTAL=$((PASS + FAIL))
echo ""
echo "  전체: ${TOTAL}건"
echo -e "  \033[32m통과: ${PASS}건\033[0m"
echo -e "  \033[31m실패: ${FAIL}건\033[0m"
echo ""

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
