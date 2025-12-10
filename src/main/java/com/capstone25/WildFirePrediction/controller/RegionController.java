package com.capstone25.WildFirePrediction.controller;

import com.capstone25.WildFirePrediction.global.ApiResponse;
import com.capstone25.WildFirePrediction.repository.RegionRepository;
import com.capstone25.WildFirePrediction.service.RegionCsvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
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

    // 검색용
    @GetMapping("/search")
    @Operation(
            summary = "지역 통합 검색",
            description = "시/도, 시군구, 읍면동명을 모두 대상으로 부분 일치 검색합니다.<br>" +
                    "예) keyword=서울 → 서울특별시 전체<br>" +
                    "예) keyword=동작 → 동작구 포함 행 모두<br>" +
                    "예) keyword=흑석 → 흑석동 포함 행 모두"
    )
    public ApiResponse<List<RegionSearchResponse>> searchRegions(
            @RequestParam String keyword
    ) {
        var regions = regionRepository
                .findBySidoContainingOrSigunguContainingOrEupmyeondongContaining(
                        keyword, keyword, keyword
                );

        var result = regions.stream()
                .map(r -> new RegionSearchResponse(
                        r.getId(),
                        r.getSido(),
                        r.getSigungu(),
                        r.getEupmyeondong()
                ))
                .toList();

        return ApiResponse.onSuccess(result);
    }

    public record RegionSearchResponse(
            Long id,
            String sido,
            String sigungu,
            String eupmyeondong
    ) {}

    @GetMapping("/sido-list")
    @Operation(summary = "시/도 목록 조회")
    public ApiResponse<List<String>> getSidoList() {
        return ApiResponse.onSuccess(regionRepository.findDistinctSido());
    }

    @GetMapping("/sigungu-by-sido")
    @Operation(summary = "시/도 기준 시군구 목록 조회",
            description = "예: sido=서울특별시 → 종로구, 중구, 용산구 ...")
    public ApiResponse<List<String>> getSigunguBySido(@RequestParam String sido) {
        List<String> sigunguList = regionRepository.findDistinctSigunguBySido(sido);
        return ApiResponse.onSuccess(sigunguList);
    }

    @GetMapping("/by-sido-sigungu")
    @Operation(summary = "시/도 + 시군구 기준 동 목록 조회")
    public ApiResponse<?> getEupmyeondongBySidoAndSigungu(
            @RequestParam String sido,
            @RequestParam String sigungu
    ) {
        return ApiResponse.onSuccess(
                regionRepository.findBySidoAndSigungu(sido, sigungu)
        );
    }
}
