# 메모리 누수 진단 가이드

## 🔴 증상
- JVM 메모리가 계속 증가하여 11GB 전부 사용
- 스레드 동작이 멈춤
- Kafka 메시지 처리 정지

## 🔍 진단 방법

### 1. 큐 상태 모니터링 로그 확인

30초마다 자동으로 출력되는 로그:
```
[ticker-basic] Queue Health Check: size=180000/200000 (90.0%), buffer=500,
  coordinator=[active=1, poolSize=1, taskCount=12345],
  workers=[active=15, poolSize=15, completed=10000, taskCount=15000]
```

**분석 포인트**:
- `size/capacity`: 큐 사용률 (90% 이상이면 위험)
- `buffer`: Coordinator의 임시 버퍼 크기
- `coordinator active`: 0이면 Coordinator 스레드 멈춤 ❌
- `workers active`: 0이면 Worker 스레드 모두 멈춤 ❌
- `completed vs taskCount`: 차이가 크면 Worker가 작업 처리 못함

### 2. GC 로그 확인

```bash
# GC 로그 위치
cat /var/log/gc.log

# Full GC 빈도 확인
grep "Full GC" /var/log/gc.log | tail -20

# GC 시간 확인
grep "GC.*ms" /var/log/gc.log | tail -50
```

**의심 패턴**:
```
[Full GC (Allocation Failure) 11000M->10950M(11400M), 5.234 secs]
[Full GC (Allocation Failure) 11000M->10980M(11400M), 6.123 secs]
```
→ Full GC 후에도 메모리가 거의 안 줄어듦 = **메모리 누수 확정**

### 3. Heap Dump 분석

OOM 발생 시 자동 생성:
```bash
ls -lh /var/log/heap_dump.hprof
```

Eclipse MAT 또는 VisualVM으로 분석:
1. 가장 많은 메모리를 사용하는 객체 확인
2. `ArrayBlockingQueue` 크기 확인
3. `SseEmitter` 개수 확인
4. `ArrayList` (buffer) 크기 확인

### 4. 스레드 덤프 확인

```bash
# 컨테이너 내부에서
jstack <PID> > thread_dump.txt

# 또는 kill -3으로 스레드 덤프 생성
kill -3 <PID>
```

**확인사항**:
- `batch-coordinator` 스레드가 WAITING/BLOCKED 상태인지
- `batch-flush-worker` 스레드들이 모두 멈춰있는지
- DB 커넥션 대기 중인 스레드 수

## 🎯 가능한 원인별 해결 방법

### 원인 1: Worker 스레드 처리 속도 부족

**증상**:
- 큐 사용률이 계속 90% 이상
- `workers active=15` (모두 바쁨)
- `completed` 숫자가 천천히 증가

**해결**:
```yaml
# application-prod.yml
app:
  batch-accumulator:
    worker-thread-count: 30  # 15 → 30
```

### 원인 2: DB 커넥션 부족/데드락

**증상**:
- Worker 스레드가 멈춤 (`active=0`)
- 로그에 DB 에러
- HikariCP timeout 에러

**해결**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100  # 50 → 100
      leak-detection-threshold: 60000  # 1분
```

### 원인 3: Coordinator 스레드 멈춤

**증상**:
- `coordinator active=0`
- 큐는 비어있지만 메시지 처리 안됨

**해결**:
- 로그에서 Coordinator 예외 확인
- 코드 리뷰 필요

### 원인 4: Consumer 속도가 너무 빠름

**증상**:
- 큐가 빠르게 가득 참
- Worker가 따라가지 못함

**해결**:
```yaml
# application-prod.yml
kafka:
  consumer:
    max-poll-records: 50  # 100 → 50 (더 줄이기)
    fetch-max-wait: 1000  # 500 → 1000 (더 늦추기)
  listener:
    concurrency: 1  # Consumer 스레드 1개로 유지
```

### 원인 5: 메시지 크기가 너무 큼

**증상**:
- 적은 개수의 메시지로도 메모리 많이 사용
- Heap dump에서 byte[] 또는 String이 대부분

**해결**:
```yaml
app:
  batch-accumulator:
    queue-capacity: 100000  # 200000 → 100000 (절반으로)
    batch-size: 500  # 1000 → 500
```

### 원인 6: SSE 연결이 끊어지지 않음

**증상**:
- Heap dump에서 `SseEmitter` 객체 수천 개
- `CopyOnWriteArraySet` 크기 매우 큼

**해결**:
- SSE timeout 설정 확인
- Controller에서 timeout 명시:
```java
SseEmitter emitter = new SseEmitter(60000L); // 60초 timeout
```

## 🚨 긴급 대응 (메모리 부족 시)

### 1. Consumer 일시 중지
```bash
# Kafka consumer group 확인
kafka-consumer-groups.sh --bootstrap-server kafka.kafka.dwer.kr:9092 \
  --group coin-wts-consumer-prod --describe

# 파티션 일시 중지 (애플리케이션 재시작)
# application-prod.yml에서 concurrency: 0
```

### 2. 큐 비우기
- 애플리케이션 재시작 시 큐 내용은 손실됨 (메모리만 해제)
- 중요 데이터라면 재시작 전 DLT로 전송 필요

### 3. 임시 메모리 증가
```groovy
// build.gradle
'-Xmx20000M'  // 11.4GB → 20GB (임시)
```

## 📊 정상 상태 기준

모니터링 로그가 다음과 같아야 정상:

```log
[ticker-basic] Queue Health Check: size=50000/200000 (25.0%), buffer=100,
  coordinator=[active=1, poolSize=1, taskCount=50000],
  workers=[active=10, poolSize=15, completed=48000, taskCount=50000]
```

**정상 조건**:
- ✅ 큐 사용률 < 60%
- ✅ coordinator active = 1
- ✅ workers active > 0
- ✅ completed ≈ taskCount (차이 < 10%)

## 🔧 권장 프로덕션 설정

```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100
      leak-detection-threshold: 60000

  kafka:
    consumer:
      max-poll-records: 50      # 낮은 값으로 시작
      fetch-max-wait: 1000
    listener:
      concurrency: 1            # 안정화 후 증가

app:
  batch-accumulator:
    batch-size: 500             # 작은 배치로 시작
    max-latency-ms: 100
    queue-capacity: 100000      # 절반으로 줄이기
    worker-thread-count: 30     # 충분한 워커
    coordinator-thread-count: 1
```

## 📝 체크리스트

메모리 누수 발생 시 순서대로 확인:

- [ ] 큐 상태 모니터링 로그 확인
- [ ] Worker 스레드가 동작 중인지 확인 (active > 0)
- [ ] Coordinator 스레드가 동작 중인지 확인 (active = 1)
- [ ] DB 커넥션 풀 상태 확인 (HikariCP 로그)
- [ ] GC 로그에서 Full GC 빈도 확인
- [ ] Heap dump 분석 (가장 큰 객체 확인)
- [ ] 스레드 덤프 분석 (데드락 확인)
- [ ] SSE 연결 수 확인
- [ ] Worker 스레드 수 증가 시도
- [ ] Consumer 속도 늦추기 시도
