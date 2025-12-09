package com.capstone25.WildFirePrediction.global.code.status;

import com.capstone25.WildFirePrediction.global.code.BaseErrorCode;
import com.capstone25.WildFirePrediction.global.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의하세요."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증되지 않은 사용자입니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    // TMAP 관련 에러
    TMAP_ROUTE_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "TMAP501", "TMAP 경로 API 조회에 실패했습니다."),

    // 대피소 API 전용 에러
    SHELTER_API_CONFIG_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "SHELTER500", "대피소 API 설정을 찾을 수 없습니다."),
    SHELTER_API_SERVICE_KEY_INVALID(HttpStatus.UNAUTHORIZED, "SHELTER401", "대피소 API 서비스키가 유효하지 않습니다."),
    SHELTER_API_RESPONSE_INVALID(HttpStatus.BAD_REQUEST, "SHELTER400", "대피소 API 응답 형식이 잘못되었습니다."),
    SHELTER_API_CALL_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "SHELTER503", "대피소 API 호출에 실패했습니다."),
    SHELTER_API_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "SHELTER408", "대피소 API 호출이 타임아웃 되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
