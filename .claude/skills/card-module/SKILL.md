---
name: card-module
description: card-mizer에 새 기능을 추가한다. 헥사고날 아키텍처 4모듈 구조를 따른다.
argument-hint: "[기능 설명 - 예: 새 카드사 어댑터 추가]"
---

## 멀티모듈 기능 추가 워크플로우

대상: **$ARGUMENTS**

### 모듈 구조 (헥사고날 아키텍처)
```
card-common/    → Money + 공유 VO
card-core/      → 도메인 모델, 유스케이스, 포트 (프레임워크 무관)
card-api/       → REST API, 데모 UI, 시뮬레이터, 정규화
card-infra/     → JPA 어댑터 + 외부 API 어댑터
```

### 구현 순서
1. **card-core** — 도메인 모델/포트 인터페이스 정의 (순수 Java, 프레임워크 의존 없음)
2. **card-common** — 공유 VO 필요 시 추가
3. **card-infra** — JPA 어댑터 또는 외부 API 어댑터 구현
4. **card-api** — REST 컨트롤러 + 테스트

### 주의사항
- card-core는 Spring 의존성 없이 순수 Java로 작성
- 포트/어댑터 패턴 준수 (core → port interface, infra → adapter implementation)
- Flyway 마이그레이션으로 스키마 관리
- 테스트 작성 필수 (25+ 기존 테스트와 일관성 유지)
