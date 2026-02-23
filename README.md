# 🎧 SOGEUN (소근)

**음악 좋아요로 나의 반경을 넓혀나가는 위치 기반 소셜 플랫폼**

SOGEUN은 사용자가 현재 듣고 있는 음악을 주변 사람들과 실시간으로 공유할 수 있는 위치 기반 서비스입니다.

방송을 켜면 반경 내 사용자에게 나의 음악이 전달되고, 다른 사용자가 “소근(좋아요)”을 누를수록 방송 범위가 점점 확장됩니다.

단순한 음악 공유를 넘어, 같은 공간에서 서로의 음악을 공유하는 경험을 통해 새로운 연결을 만들어냅니다.

내 취향이 주변을 물들이고, 인기 있는 방송은 더 넓은 지역으로 퍼져나갑니다.

지금 이 순간, 내 주변에서는 어떤 음악이 흐르고 있을까요?

---

## ✨ 주요 기능

### 📡 음악 송출

- 현재 공유하고 싶은 음악을 주변 사용자에게 실시간 송출
- 방송 On / Off 기능
- 반경 내 사용자에게만 전달되는 위치 기반 방송

### 💙 소근 (좋아요) 시스템

- 방송을 듣는 사용자가 소근(좋아요) 가능
- 소근 수에 따라 송출 반경 자동 확장
- 소근을 많이 받을수록 더 넓은 지역으로 확산 가능

### 📍 위치 기반 서비스

- Redis GEO 기반 위치 서비스
- 실시간 사용자 위치 저장 및 관리
- 설정된 반경 내 사용자 탐색
- 거리 기반 방송 수신

### 🔔 실시간 이벤트

- 방송 시작 / 종료 알림
- 반경 확장 이벤트 전파
- 실시간 상태 동기화

### 🎵 음악 정보 관리

- 재생 중인 음악 메타데이터 표시
- 아티스트 / 제목 / 앨범 아트 제공
- 음악 변경 시 실시간 업데이트

### 🔒 보안 기능

- Spring Security 기반 인증/인가
- BCrypt 비밀번호 암호화
- 세션 기반 인증

---

## 🛠 기술 스택

**Backend**

- Spring Boot
- Java
- Spring Security

**Database**

- MySQL
- JPA / Hibernate

**Real-time**

- Server-Sent Events (SSE)

**Cloud**

- AWS Ec2

---

## 📁 프로젝트 구조

```
src/main/java/com/example/sogeun/
├── SogeunApplication.java          # 메인 애플리케이션
├── SecurityConfig.java             # 보안 설정
├── config
│   └── OpenApiConfig.java          # Swagger 설정
├── controller/                     # REST API & WebSocket 컨트롤러
│   ├── UserController.java         # 사용자 관리
│   ├── BroadcastController.java    # 방송 관리
│   ├── MusicController.java        # 음악 관련 API
│   └── LocationController.java     # 위치 관리
├── dto/                            # 데이터 전송 객체
├── entity/                         # JPA 엔티티
│   ├── User.java                   # 사용자
│   ├── Broadcast.java              # 방송
│   ├── Music.java                  # 음악
│   └── BroadcastMusicLike.java     # 소근   
├── repository/                     # 데이터 액세스 계층
├── service/                        # 비즈니스 로직
├── common                   
│   └──exception                    # 예외 처리
├── sse/                            # 실시간 송출 및 알림 관련 (SSE)
│   ├── BroadcastController.java    # 송출 관련 API
│   ├── LocationController.java     # 위치 관련 API
│   ├── SseConnectController.java   # SSE 연결 관련 API
│   ├── Service/                    # 송출 관련 비즈니스 로직
│   └── dto/                        # 송출용 데이터 전송 객체
```


## 📊 핵심 특징

### 🎯 반경 확장 알고리즘

- 기본 반경에서 시작
- 소근 수에 따라 단계적으로 확장

### ⚡ 실시간 전파 구조

- 방송 상태 변경 즉시 전달
- 반경 내 사용자에게 이벤트 전송
- 효율적인 네트워크 사용

### 🌐 지역 기반 소셜 경험

- 같은 공간에서 같은 음악을 듣는 경험
- 익명 기반 취향 공유
- 자연스러운 커뮤니티 형성

---

## 📢 SSE 선택 이유

- 실시간성:
- 효율성:

---

## 👥 Team

GDG 홍익대학교 프로젝트 팀

**권젼젼젼 — Backend Team**