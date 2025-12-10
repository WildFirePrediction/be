package com.capstone25.WildFirePrediction.service;

import com.capstone25.WildFirePrediction.domain.AIPredictedCell;
import com.capstone25.WildFirePrediction.domain.AIPredictionFire;
import com.capstone25.WildFirePrediction.domain.enums.FireStatus;
import com.capstone25.WildFirePrediction.dto.request.AIPredictionRequest.FireLocationDto;
import com.capstone25.WildFirePrediction.dto.request.AIPredictionRequest.FirePredictionRequestDto;
import com.capstone25.WildFirePrediction.dto.request.AIPredictionRequest.PredictedCellDto;
import com.capstone25.WildFirePrediction.dto.request.AIPredictionRequest.PredictionDto;
import com.capstone25.WildFirePrediction.repository.AIPredictedCellRepository;
import com.capstone25.WildFirePrediction.repository.AIPredictionFireRepository;
import com.capstone25.WildFirePrediction.sse.FireSseEmitterRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIPredictionService {

    private final AIPredictionFireRepository fireRepository;
    private final AIPredictedCellRepository cellRepository;
    private final FireSseEmitterRepository fireSseEmitterRepository;

    // AI 수신 데이터 비동기 처리
    @Async("aiPredictionExecutor")
    @Transactional
    public void processAIPrediction(FirePredictionRequestDto requestDto) {
        log.info("AI 예측 데이터 처리 시작 - fireId: {}, eventType: {}",
                requestDto.getFireId(), requestDto.getEventType());

        try {
            // event_type으로 분리
            if ("0".equals(requestDto.getEventType())) {
                processFirePrediction(requestDto);
            } else if ("1".equals(requestDto.getEventType())) {
                processFireEnd(requestDto);
            } else {
                log.error("알 수 없는 이벤트 타입: {}", requestDto.getEventType());
            }

            log.info("AI 예측 데이터 처리 완료 - fireId: {}", requestDto.getFireId());
        } catch (Exception e) {
            log.error("AI 예측 데이터 처리 실패 - fireId: {}, error: {}",
                    requestDto.getFireId(), e.getMessage(), e);
        }
    }

    // 화재 예측 데이터 저장 (event_type = 0)
    @Transactional
    public void processFirePrediction(FirePredictionRequestDto requestDto) {
        String fireId = requestDto.getFireId();

        // 1. 데이터 검증
        if (!validatePredictionData(requestDto)) {
            log.error("데이터 검증 실패 - fireId: {}, 처리 중단", fireId);
            return;
        }

        // 2. 기존 데이터 조회
        Optional<AIPredictionFire> existingFireOpt = fireRepository.findByFireId(fireId);
        AIPredictionFire fire;
        if (existingFireOpt.isPresent()) {
            // 기존 데이터가 있으면 업데이트
            fire = existingFireOpt.get();

            if (fire.getStatus() == FireStatus.END) {
                // 재발화
                log.info("종료된 화재 재발화 - fireId: {}, 상태 PROGRESS로 변경", fireId);
                fire.reactivateFire();
            } else {
                log.info("진행중인 화재 최신 데이터로 업데이트 - fireId: {}", fireId);
            }

            // 기존 셀 데이터 초기화 (DB도 삭제)
            int deletedCount = cellRepository.deleteAllByFireId(fire.getId());
            fire.getPredictedCells().clear();
            log.info("기존 예측 셀 벌크 삭제 완료 - fireId: {}, 삭제된 셀 개수: {}",
                    fireId, deletedCount);

            // 화재 정보 업데이트
            fire.updatePredictionData(
                    requestDto.getEventType(),
                    requestDto.getFireLocation().getLat(),
                    requestDto.getFireLocation().getLon(),
                    requestDto.getFireTimestamp(),
                    requestDto.getInferenceTimestamp(),
                    requestDto.getModel()
            );
        } else {
            // 없으면 새로 생성
            log.info("신규 화재 데이터 생성 - fireId: {}", fireId);
            fire = convertToFireEntity(requestDto);
        }

        // 3. 예측 셀 추가
        if (requestDto.getPredictions() != null) {
            for (PredictionDto prediction : requestDto.getPredictions()) {
                for (PredictedCellDto cellDto : prediction.getPredictedCells()) {
                    AIPredictedCell cell = convertToCellEntity(cellDto, prediction);
                    fire.addPredictedCell(cell);
                }
            }
        }

        // 4. DB 저장
        AIPredictionFire savedFire = fireRepository.save(fire);

        log.info("화재 예측 데이터 저장 완료 - fireId: {}, 예측 셀 개수: {}",
                fireId, savedFire.getPredictedCells().size());

        // 5. SSE 발송 (트랜잭션 커밋 후)
        registerAfterCommitSse(requestDto, "fire_prediction");
    }

    // 화재 종료 처리 (event_type = 1)
    @Transactional
    public void processFireEnd(FirePredictionRequestDto requestDto) {
        String fireId = requestDto.getFireId();

        // 1. 기존 화재 조회
        Optional<AIPredictionFire> fireOptional = fireRepository.findByFireId(fireId);
        if (fireOptional.isEmpty()) {
            log.warn("종료 처리할 화재를 찾을 수 없습니다 - fireId: {}", fireId);
            return;
        }
        AIPredictionFire fire = fireOptional.get();

        // 2. 이미 종료된 화재인지 확인
        if (fire.getStatus() == FireStatus.END) {
            log.info("이미 종료된 화재입니다. 종료 정보 업데이트 - fireId: {}", fireId);
        } else {
            log.info("화재 종료 처리 시작 - fireId: {}", fireId);
        }

        // 3. 예측 셀 전체 삭제 (DB 용량 절약)
        int deletedCount = cellRepository.deleteAllByFireId(fire.getId());
        fire.getPredictedCells().clear();
        log.info("화재 종료: 예측 셀 삭제 완료 - fireId: {}, 삭제된 셀 개수: {}",
                fireId, deletedCount);

        // 4. 화재 상태를 '종료'로 업데이트 (Fire 정보는 유지)
        fire.endFire(
                requestDto.getEndReason(),
                requestDto.getEndedTimestamp(),
                requestDto.getCompletionTimestamp()
        );

        // 5. DB 저장
        fireRepository.save(fire);
        log.info("화재 종료 처리 완료 - fireId: {}, endReason: {}",
                fireId, requestDto.getEndReason());

        // 6. SSE 발송 (트랜잭션 커밋 후)
        registerAfterCommitSse(requestDto, "fire_end");
    }

    // 예측 데이터 검증
    private boolean validatePredictionData(FirePredictionRequestDto requestDto) {
        // 위경도 범위 검증 (한국 영역)
        double lat = requestDto.getFireLocation().getLat();
        double lon = requestDto.getFireLocation().getLon();
        if (lat < 33 || lat > 39 || lon < 124 || lon > 132) {
            log.error("잘못된 좌표 범위 - fireId: {}, lat: {}, lon: {}",
                    requestDto.getFireId(), lat, lon);
            return false;
        }

        // predictions 배열 검증
        if (requestDto.getPredictions() == null || requestDto.getPredictions().isEmpty()) {
            log.error("예측 데이터가 비어있음 - fireId: {}", requestDto.getFireId());
            return false;
        }

        // 각 prediction의 predicted_cells 검증
        for (PredictionDto prediction : requestDto.getPredictions()) {
            if (prediction.getPredictedCells() == null || prediction.getPredictedCells().isEmpty()) {
                log.error("timestep {}의 예측 셀이 비어있음 - fireId: {}",
                        prediction.getTimestep(), requestDto.getFireId());
                return false;
            }
        }

        return true;
    }

    // 진행 중 화재 예측을 FirePredictionRequestDto 리스트로 반환
    @Transactional(readOnly = true)
    public List<FirePredictionRequestDto> getActiveFirePredictionsAsRequestDto() {
        // 1. 진행 중 화재 조회
        List<AIPredictionFire> fires = fireRepository.findAllProgressFiresWithCells();

        // 2. 엔티티 -> FirePredictionRequestDto 변환
        return fires.stream()
                .map(this::toFirePredictionRequestDto)
                .collect(Collectors.toList());
    }

    // Fire 엔티티 -> FirePredictionRequestDto 변환
    private FirePredictionRequestDto toFirePredictionRequestDto(AIPredictionFire fire) {

        // fire_location
        FireLocationDto locationDto = FireLocationDto.builder()
                .lat(fire.getFireLatitude())
                .lon(fire.getFireLongitude())
                .build();

        // AIPredictedCell -> PredictionDto (timestep별 그룹핑)
        Map<Integer, List<AIPredictedCell>> byTimeStep = fire.getPredictedCells().stream()
                .collect(Collectors.groupingBy(AIPredictedCell::getTimeStep));

        List<PredictionDto> predictionDtos = byTimeStep.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // timestep 오름차순
                .map(entry -> {
                    Integer timestep = entry.getKey();
                    List<AIPredictedCell> cells = entry.getValue();

                    String timestamp = cells.get(0).getPredictedTimestamp();

                    List<PredictedCellDto> cellDtos = cells.stream()
                            .map(cell -> PredictedCellDto.builder()
                                    .lat(cell.getLatitude())
                                    .lon(cell.getLongitude())
                                    .probability(cell.getProbability())
                                    .build())
                            .collect(Collectors.toList());

                    return PredictionDto.builder()
                            .timestep(timestep)
                            .timestamp(timestamp)
                            .predictedCells(cellDtos)
                            .build();
                })
                .collect(Collectors.toList());

        // FirePredictionRequestDto 생성 (event_type = "0" 고정)
        return FirePredictionRequestDto.builder()
                .eventType("0")
                .fireId(fire.getFireId())
                .fireLocation(locationDto)
                .fireTimestamp(fire.getFireTimestamp())
                .inferenceTimestamp(fire.getInferenceTimestamp())
                .model(fire.getModel())
                .predictions(predictionDtos)
                // 종료 관련 필드는 진행 중이므로 null
                .endedTimestamp(null)
                .completionTimestamp(null)
                .endReason(null)
                .lastStatus(null)
                .lastStatusCode(null)
                .build();
    }

    // SSE 발송용 유틸 메서드
    private void registerAfterCommitSse(FirePredictionRequestDto requestDto, String eventName) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("트랜잭션 커밋 후 SSE 발행 - fireId: {}, event: {}", requestDto.getFireId(), eventName);
                    fireSseEmitterRepository.sendToAll(requestDto, eventName);
                }
            });
        } else {
            // 트랜잭션이 없으면 바로 발행
            fireSseEmitterRepository.sendToAll(requestDto, eventName);
        }
    }

    // DTO -> Fire 엔티티 변환
    private AIPredictionFire convertToFireEntity(FirePredictionRequestDto dto) {
        return AIPredictionFire.builder()
                .fireId(dto.getFireId())
                .eventType(dto.getEventType())
                .fireLatitude(dto.getFireLocation().getLat())
                .fireLongitude(dto.getFireLocation().getLon())
                .fireTimestamp(dto.getFireTimestamp())
                .inferenceTimestamp(dto.getInferenceTimestamp())
                .model(dto.getModel())
                .status(FireStatus.PROGRESS)
                .build();
    }

    // DTO -> PredictedCell 엔티티 변환
    private AIPredictedCell convertToCellEntity(PredictedCellDto cellDto, PredictionDto predictionDto) {
        return AIPredictedCell.builder()
                .latitude(cellDto.getLat())
                .longitude(cellDto.getLon())
                .timeStep(predictionDto.getTimestep())
                .predictedTimestamp(predictionDto.getTimestamp())
                .probability(cellDto.getProbability())
                .build();
    }

    // 진행중인 화재 개수 조회
    public long getActiveFireCount() {
        return fireRepository.countProgressFiresCount();
    }

    // 특정 화재의 상세 정보 조회
    public Optional<AIPredictionFire> getFireDetail(String fireId) {
        return fireRepository.findByFireId(fireId);
    }
}
