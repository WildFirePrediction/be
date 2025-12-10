package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.repository.RegionRepository;
import com.capstone25.WildFirePrediction.service.RegionCsvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/regions")
public class RegionController {

    private final RegionCsvService regionCsvService;
    private final RegionRepository regionRepository;

    // CSV 업로드 -> DB 저장
    @PostMapping(
        value = "/upload",
        consumes = {"multipart/form-data"}
    )
    @Operation(summary = "지역 정보 CSV 업로드 (서버용)",
            description = "지역 정보가 담긴 CSV 파일을 업로드하여 DB에 저장합니다.")
    public ApiResponse<String> uploadRegionCsv(
            @RequestParam("file")
            @Parameter(description = "지역 정보가 담긴 CSV 파일", required = true)
            MultipartFile file)
        throws Exception {
        int count = regionCsvService.importRegionsFromCsv(file);
        return ApiResponse.onSuccess("Successfully imported " + count + " regions.");
    }

    // 저장된 지역 일부 확인용
    @GetMapping("/sample")
    @Operation(summary = "저장된 지역 정보 일부 조회",
            description = "DB에 저장된 지역 정보 중 일부(10개)를 조회합니다.")
    public ApiResponse<?> getSampleRegions() {
        return ApiResponse.onSuccess(regionRepository.findAll().stream().limit(10).toList());
    }
}
