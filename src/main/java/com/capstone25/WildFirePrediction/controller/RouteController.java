package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.dto.request.RouteRequest;
import com.capstone25.WildFirePrediction.dto.response.RouteResponse;
import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.TmapRouteService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/routes")
public class RouteController {
    private final TmapRouteService tmapRouteService;

    public RouteController(TmapRouteService tmapRouteService) {
        this.tmapRouteService = tmapRouteService;
    }

    // 보행자 경로 조회
    @PostMapping("")
    @Operation(summary = "보행자 경로 조회",
            description = "출발지와 목적지 좌표를 받아 보행자 경로를 조회합니다.\n"
                    + "request는 위도/경도 순, response는 경도/위도 순입니다. 혼동 주의하세요.")
    public ApiResponse<RouteResponse> getPedestrianRoute(
            @RequestBody RouteRequest request) {

        log.info("경로 조회 요청: {}, {} → {}, {}",
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon());

        RouteResponse response = tmapRouteService.getTmapRoute(request);

        return ApiResponse.onSuccess(response);
    }

}
