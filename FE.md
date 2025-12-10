## 1) API 목록
### 진행 중 화재 초기 로딩
- GET `/fires/active`
- 응답
  - FirePredictionRequestDto[] (AI와 동일, 해당 문서 참고)
- 각 요소 구조 예시
```
{
  "event_type": "0",
  "fire_id": "12345",
  "fire_location": { "lat": 36.5684, "lon": 128.7294 },
  "fire_timestamp": "2025-12-02T14:30:00",
  "inference_timestamp": "2025-12-02T14:31:23.456789",
  "model": "a3c_16ch_v3_lstm_rel",
  "predictions": [
    {
      "timestep": 1,
      "timestamp": "2025-12-02T14:40:00",
      "predicted_cells": [
        { "lat": 36.5685, "lon": 128.7295, "probability": 1.0 }
      ]
    }
  ]
}

```
### 산불 예측/종료 실시간 스트림 (SSE)
- GET `/fires/sse-stream`
- SSE 이벤트
  - connect
    - 연결 직후 1회 전송
    - data: "SSE connected"
  - fire_prediction
    - 신규/갱신 예측
    - data: FirePredictionRequestDto (`event_type` = "0")
  - fire_end
    - 화재 종료 알림
    - data: FirePredictionRequestDto (`event_type` = "1")

## 2) 프론트 처리 규칙
- 페이지 진입 시:
  1. GET /fires/active 호출
  2. 응답 배열 순회하며:
     - fire_location 기준으로 마커
     - predictions[].predicted_cells[].(lat, lon, probability)를 사용해서 폴리곤/레이어 생성
- SSE 구독: typescript 예시 (by. GPT)
```
const es = new EventSource("/fires/sse-stream");

es.addEventListener("connect", (e) => {
  console.log("SSE connected:", e.data);
});

es.addEventListener("fire_prediction", (e) => {
  const data = JSON.parse(e.data); // FirePredictionRequestDto
  // data.fire_id 기준으로 기존 데이터 교체 or 신규 추가
  // data.predictions[].predicted_cells[]로 지도 갱신
});

es.addEventListener("fire_end", (e) => {
  const data = JSON.parse(e.data);
  // data.fire_id 기준으로 지도에서 해당 화재 관련 마커/레이어 제거
});

es.onerror = (err) => {
  console.error("SSE error:", err);
  // 필요 시 재연결 로직
};
```
- 중요한 포인트:
  - JSON 스키마는 AI와 주고받는 것과 100% 동일
  - event_type는 참고용이고, SSE 이벤트명(fire_prediction / fire_end)으로도 구분 가능.​