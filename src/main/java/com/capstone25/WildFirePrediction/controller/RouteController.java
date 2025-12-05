package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.dto.request.RouteRequest;
import com.capstone25.WildFirePrediction.dto.response.RouteResponse;
import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.service.TmapRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final TmapRouteService tmapRouteService;

    public RouteController(TmapRouteService tmapRouteService) {
        this.tmapRouteService = tmapRouteService;
    }

    // 보행자 경로 조회 (테스트)
    @PostMapping("/test")
    public ApiResponse<RouteResponse> getPedestrianRoute(
            @RequestBody RouteRequest request) {

        log.info("경로 조회 요청: {} → {}",
                request.getStartName(), request.getEndName());

        RouteResponse response = tmapRouteService.getTmapRoute(request);

        return ApiResponse.onSuccess(response);
    }

}
