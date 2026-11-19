package team.themoment.hellogsmv3.domain.oneseo.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.*;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.service.*;
import team.themoment.hellogsmv3.domain.oneseo.service.CreateOneseoService;
import team.themoment.hellogsmv3.domain.oneseo.service.ModifyOneseoService;
import team.themoment.hellogsmv3.domain.oneseo.service.ModifyRealOneseoArrivedYnService;
import team.themoment.hellogsmv3.domain.oneseo.service.QueryOneseoByIdService;
import team.themoment.hellogsmv3.domain.oneseo.service.SearchOneseoService;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;
import team.themoment.sdk.response.CommonApiResponse;

@Tag(name = "Oneseo API", description = "원서 관련 API입니다.")
@Validated
@RestController
@RequestMapping("/oneseo/v3")
@RequiredArgsConstructor
public class OneseoController {

    private final CreateOneseoService createOneseoService;
    private final ModifyOneseoService modifyOneseoService;
    private final ModifyOneseoByApplicantService modifyOneseoByApplicantService;
    private final ModifyRealOneseoArrivedYnService modifyRealOneseoArrivedYnService;
    private final ModifyCompetencyEvaluationScoreService modifyCompetencyEvaluationScoreService;
    private final ModifyInterviewScoreService modifyInterviewScoreService;
    private final QueryAdmissionTicketsService queryAdmissionTicketsService;
    private final DownloadExcelService downloadExcelService;
    private final SearchOneseoService searchOneseoService;
    private final QueryOneseoByIdService queryOneseoByIdService;
    private final OneseoTempStorageService oneseoTempStorageService;
    private final ModifyEntranceIntentionService modifyEntranceIntentionService;
    private final QueryOneseoEditabilityService queryOneseoEditabilityService;
    private final UploadExcelService uploadExcelService;
    private final RequestOneseoEditPermissionService requestOneseoEditPermissionService;
    private final ApproveOneseoEditPermissionService approveOneseoEditPermissionService;
    private final ModifyPersonalInfoService modifyPersonalInfoService;

    @Operation(summary = "내 원서 등록", description = "원서를 등록합니다. 이미 원서가 있으면 400입니다. 성적 계산 외부 서비스의 오류 상태가 전달될 수 있습니다.\n요청 가능 기간: 원서 접수 기간(시작·종료 시각 제외)\n권한: APPLICANT, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족 또는 요청 가능 기간이 아님", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content()),
            @ApiResponse(responseCode = "429", description = "외부 성적 계산 서비스 요청 한도 초과", content = @Content()),
            @ApiResponse(responseCode = "500", description = "내부 처리 또는 외부 서비스 실패", content = @Content()),
            @ApiResponse(responseCode = "502", description = "외부 성적 계산 서비스 게이트웨이 오류", content = @Content()),
            @ApiResponse(responseCode = "503", description = "외부 성적 계산 서비스 이용 불가", content = @Content())})
    @PostMapping("/oneseo/me")
    public CommonApiResponse create(@RequestBody @Valid OneseoReqDto reqDto, @AuthRequest Long memberId) {
        createOneseoService.execute(reqDto, memberId);
        return CommonApiResponse.created("원서가 생성되었습니다.");
    }

    @Operation(summary = "원서 수정", description = "회원 ID로 원서를 수정합니다. 1차 결과 산출 이후에는 불가합니다. 전형 변경 시 접수번호를 재발급하고 이력을 저장하며, 수정 권한을 NONE으로 회수합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 1차 결과 산출 이후인 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content()),
            @ApiResponse(responseCode = "429", description = "외부 성적 계산 서비스 요청 한도 초과", content = @Content()),
            @ApiResponse(responseCode = "500", description = "내부 처리 또는 외부 서비스 실패", content = @Content()),
            @ApiResponse(responseCode = "502", description = "외부 성적 계산 서비스 게이트웨이 오류", content = @Content()),
            @ApiResponse(responseCode = "503", description = "외부 성적 계산 서비스 이용 불가", content = @Content())})
    @PutMapping("/oneseo/{memberId}")
    public CommonApiResponse modifyByAdmin(@RequestBody @Valid OneseoReqDto reqDto,
            @PathVariable("memberId") Long memberId) {
        modifyOneseoService.execute(reqDto, memberId);
        return CommonApiResponse.success("원서가 수정되었습니다.");
    }

    @Operation(summary = "실물 원서 제출 여부 수정", description = "요청 본문 없이 실물 원서 도착 여부를 YES ↔ NO로 토글합니다. 중복 호출 시 원래 상태로 돌아갑니다. 1차 결과 산출 이후에는 불가합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @PatchMapping("/arrived-status/{memberId}")
    public ArrivedStatusResDto modifyArrivedStatus(@PathVariable Long memberId) {
        return modifyRealOneseoArrivedYnService.execute(memberId);
    }

    @Operation(summary = "역량검사 점수 기입", description = "역량검사 점수를 0~100 범위로 입력합니다. 2차 결과 산출 이후에는 불가합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @PatchMapping("/competency-score/{memberId}")
    public CommonApiResponse modifyCompetencyScore(@PathVariable Long memberId,
            @RequestBody @Valid CompetencyEvaluationScoreReqDto competencyEvaluationScoreReqDto) {
        modifyCompetencyEvaluationScoreService.execute(memberId, competencyEvaluationScoreReqDto);
        return CommonApiResponse.success("수정되었습니다.");
    }

    @Operation(summary = "심층 면접 검사 점수 기입", description = "심층면접 점수를 0~100 범위로 입력합니다. 2차 결과 산출 이후에는 불가합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @PatchMapping("/interview-score/{memberId}")
    public CommonApiResponse modifyInterviewScore(@PathVariable Long memberId,
            @RequestBody @Valid InterviewScoreReqDto reqDto) {
        modifyInterviewScoreService.execute(memberId, reqDto);
        return CommonApiResponse.success("수정되었습니다.");
    }

    @Operation(summary = "원서 검색", description = "원서를 조건별로 검색합니다. keyword는 이름·학교명·전화번호의 부분 일치(LIKE) 검색입니다. FALL은 1차 또는 2차 불합격입니다. sortBy 생략 시 접수번호 내림차순이며 sortDirection은 무시합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content())})
    @GetMapping("/oneseo/search")
    public SearchOneseosResDto search(
            @Parameter(description = "0부터 시작하는 페이지 번호. 필수, 0 이상", example = "0") @RequestParam @Min(0) Integer page,
            @Parameter(description = "페이지 크기. 필수, 1 이상", example = "10") @RequestParam @Min(1) Integer size,
            @Parameter(description = "결과 필터. ALL: 전체, FIRST_PASS: 1차 합격, FINAL_PASS: 최종 합격, FALL: 1차 또는 2차 불합격") @RequestParam TestResultTag testResultTag,
            @Parameter(description = "전형 분류 필터") @RequestParam(required = false) ScreeningCategory screeningTag,
            @Parameter(description = "서류 제출 여부") @RequestParam(required = false) YesNo isSubmitted,
            @Parameter(description = "이름·학교명·전화번호 부분 일치 검색어") @RequestParam(name = "keyword", required = false) String keyword,
            @Parameter(description = "수정 상태 필터 (ANY_EDIT, REQUESTED, APPROVED)") @RequestParam(required = false) OneseoEditStatusTag status,
            @Parameter(description = "정렬 기준, 생략하면 접수번호 내림차순") @RequestParam(required = false) OneseoSortBy sortBy,
            @Parameter(description = "정렬 방향 (ASC, DESC), YES/NO 컬럼은 ASC일 때 YES가 먼저 옵니다.") @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection) {
        return searchOneseoService
                .execute(page, size, testResultTag, screeningTag, isSubmitted, keyword, status, sortBy, sortDirection);
    }

    @Operation(summary = "내 원서 조회", description = "내 원서를 조회합니다. 임시저장 캐시가 있으면 이를 우선 반환하며 TTL은 4일입니다. 임시저장본은 oneseoId·submitCode가 null이고 step이 포함되며 calculatedScore는 생략될 수 있습니다. 원서와 캐시가 모두 없으면 404입니다.\n권한: APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @GetMapping("/oneseo/me")
    public FoundOneseoResDto find(@AuthRequest Long memberId) {
        return queryOneseoByIdService.execute(memberId);
    }

    @Operation(summary = "원서 조회", description = "회원 ID로 원서를 조회합니다. 지원자의 임시저장과 조회 캐시를 공유하므로 아직 제출하지 않은 임시저장본이 반환될 수 있습니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @GetMapping("/oneseo/{memberId}")
    public FoundOneseoResDto findByAdmin(@PathVariable Long memberId) {
        return queryOneseoByIdService.execute(memberId);
    }

    @Operation(summary = "수험표 출력", description = "1차 전형 합격자(firstTestPassYn=YES)의 수험표 정보를 반환합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content())})
    @GetMapping("/admission-tickets")
    public List<AdmissionTicketsResDto> getAdmissionTickets() {
        return queryAdmissionTicketsService.execute();
    }

    @Operation(summary = "원서 임시 저장", description = "원서를 부분 임시저장합니다. 반환 본문에 원서 data는 없으며 GET /oneseo/v3/oneseo/me로 확인합니다. 이미 제출했고 수정 승인이 없으면 400입니다. 성적 입력 시 외부 성적 계산 오류가 전달될 수 있습니다.\n요청 가능 기간: 원서 접수 기간(시작·종료 시각 제외)\n권한: UNAUTHENTICATED, APPLICANT, ADMIN, ROOT (역할 매처는 없지만 로그인 세션 필요)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족 또는 요청 가능 기간이 아님", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content()),
            @ApiResponse(responseCode = "429", description = "외부 성적 계산 서비스 요청 한도 초과", content = @Content()),
            @ApiResponse(responseCode = "500", description = "내부 처리 또는 외부 서비스 실패", content = @Content()),
            @ApiResponse(responseCode = "502", description = "외부 성적 계산 서비스 게이트웨이 오류", content = @Content()),
            @ApiResponse(responseCode = "503", description = "외부 성적 계산 서비스 이용 불가", content = @Content())})
    @PostMapping("/temp-storage")
    public CommonApiResponse temp(@RequestBody @Valid OneseoTempReqDto reqDto,
            @Parameter(description = "임시저장 진행 단계. 필수이며 별도 범위 검증은 하지 않습니다.", example = "1") @RequestParam Integer step,
            @AuthRequest Long memberId) {
        oneseoTempStorageService.execute(reqDto, step, memberId);
        return CommonApiResponse.success("원서가 임시저장되었습니다.");
    }

    @Operation(summary = "엑셀 업로드", description = "첫 시트의 첫 행(헤더)을 건너뛰고 0열 수험번호, 3열 역량검사, 4열 심층면접 점수(0~100)를 읽습니다. 2차 결과 산출 여부를 검사하지 않아 기존 점수를 덮어쓸 수 있습니다.\n요청 가능 기간: 면접일 ~ 최종 결과 발표(시작·종료 시각 제외)\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족 또는 요청 가능 기간이 아님", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content()),
            @ApiResponse(responseCode = "500", description = "내부 처리 또는 외부 서비스 실패", content = @Content())})
    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonApiResponse uploadExcel(
            @Parameter(description = "필수 엑셀 파일. 최대 5MB. 첫 시트의 0·3·4열을 사용합니다.") @RequestParam("file") MultipartFile file) {
        uploadExcelService.execute(file);
        return CommonApiResponse.success("엑셀 파일이 성공적으로 업로드되었습니다.");
    }

    @Operation(summary = "엑셀 출력", description = "지원자 입학정보.xlsx 첨부 파일을 반환합니다. 일반·특별·정원 외 시트에는 1차 합격이면서 2차 합격 또는 미정인 원서가 포함되고, 불합격 시트에는 1차 또는 2차 불합격 원서가 포함됩니다. 1차 결과 미산출 원서는 제외됩니다.\n권한: ADMIN, ROOT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "엑셀 첨부 파일", content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "500", description = "내부 처리 또는 외부 서비스 실패", content = @Content())})
    @GetMapping("/excel")
    public void downloadExcel(HttpServletResponse response) {
        Workbook workbook = downloadExcelService.execute();
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename="
                            + URLEncoder.encode("지원자 입학정보.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException ex) {
            throw new RuntimeException("파일 작성과정에서 예외가 발생하였습니다.", ex);
        }
    }

    @Operation(summary = "입학등록 동의서 제출여부 수정", description = "최종 합격자(decidedMajor가 있는 지원자)의 입학등록 동의서 제출 여부를 수정합니다. 최종 합격자가 아니면 400입니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @PatchMapping("/entrance-intention/{memberId}")
    public CommonApiResponse modifyEntranceIntention(@PathVariable Long memberId,
            @RequestBody @Valid EntranceIntentionReqDto reqDto) {
        modifyEntranceIntentionService.execute(memberId, reqDto);
        return CommonApiResponse.success("수정되었습니다.");
    }

    @Operation(summary = "원서 수정 가능여부", description = "oneseoEditability는 전체 지원자 중 1차 결과가 하나라도 있으면 false인 전역 값이며, 개인 상태·접수 기간은 반영하지 않습니다. 본인 원서의 수정 상태도 반환하며 원서가 없으면 oneseoEditStatus는 null입니다.\n권한: UNAUTHENTICATED, APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content())})
    @GetMapping("/editability")
    public OneseoEditabilityResDto getOneseoEditability(@AuthRequest Long memberId) {
        return queryOneseoEditabilityService.execute(memberId);
    }

    @Operation(summary = "원서 수정 권한 요청", description = "원서 수정 권한을 요청합니다. JVM 기본 시간대 기준 09:00:00~16:00:00(양끝 포함)에만 가능하며, 1차 결과 산출 이후에는 불가합니다.\n권한: APPLICANT, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "APPLICANT·ROOT 권한이 아니거나 허용 시간 외 또는 1차 결과 산출 이후", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content()),
            @ApiResponse(responseCode = "409", description = "이미 APPROVED 또는 REQUESTED 상태인 경우", content = @Content())})
    @PostMapping("/oneseo/me/request")
    public CommonApiResponse requestEditPermission(@AuthRequest Long memberId) {
        requestOneseoEditPermissionService.execute(memberId);
        return CommonApiResponse.success("원서 수정 권한 요청이 완료되었습니다.");
    }

    @Operation(summary = "원서 수정 권한 승인", description = "REQUESTED 상태인 원서의 수정 권한을 승인합니다. 요청 상태가 아니면 400입니다. 1차 결과 산출 이후에도 승인은 가능하지만 지원자의 실제 수정은 제한됩니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @PatchMapping("/oneseo/{memberId}/approval")
    public CommonApiResponse approveEditPermission(@PathVariable Long memberId) {
        approveOneseoEditPermissionService.execute(memberId);
        return CommonApiResponse.success("원서 수정 권한 요청이 승인되었습니다.");
    }

    @Operation(summary = "내 원서 수정 (지원자)", description = "APPROVED 상태의 지원자가 원서를 한 번 수정합니다. 성공 시 수정 권한은 NONE으로 회수합니다. 미승인 또는 1차 결과 산출 이후에는 403입니다. 접수 기간 필터는 적용되지 않습니다.\n권한: APPLICANT, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content()),
            @ApiResponse(responseCode = "429", description = "외부 성적 계산 서비스 요청 한도 초과", content = @Content()),
            @ApiResponse(responseCode = "500", description = "내부 처리 또는 외부 서비스 실패", content = @Content()),
            @ApiResponse(responseCode = "502", description = "외부 성적 계산 서비스 게이트웨이 오류", content = @Content()),
            @ApiResponse(responseCode = "503", description = "외부 성적 계산 서비스 이용 불가", content = @Content())})
    @PutMapping("/oneseo/me")
    public CommonApiResponse modifyByApplicant(@RequestBody @Valid OneseoReqDto reqDto, @AuthRequest Long memberId) {
        modifyOneseoByApplicantService.execute(reqDto, memberId);
        return CommonApiResponse.success("원서가 수정되었습니다.");
    }

    @Operation(summary = "내 인적사항 수정", description = "지원자 본인의 이름·생년월일·성별을 수정합니다. 원서가 있으면 1차 결과 산출 이후에는 불가합니다. 기존 원서 캐시를 삭제하거나 임시저장본의 인적사항을 갱신합니다.\n권한: APPLICANT, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @PatchMapping("/personal-info/me")
    public CommonApiResponse modifyMyPersonalInfo(@RequestBody @Valid ModifyPersonalInfoReqDto reqDto,
            @AuthRequest Long memberId) {
        modifyPersonalInfoService.execute(reqDto, memberId, true);
        return CommonApiResponse.success("수정되었습니다.");
    }

    @Operation(summary = "인적사항 수정", description = "회원 ID로 이름·생년월일·성별을 수정합니다. 1차 결과 산출 이후에도 가능합니다. 회원이 없으면 404입니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값·형식 또는 업무 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인하지 않았거나 세션이 만료된 경우", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한이 부족하거나 작업 제한 조건을 만족하지 않은 경우", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 또는 원서가 존재하지 않는 경우", content = @Content())})
    @PatchMapping("/personal-info/{memberId}")
    public CommonApiResponse modifyPersonalInfo(@RequestBody @Valid ModifyPersonalInfoReqDto reqDto,
            @PathVariable Long memberId) {
        modifyPersonalInfoService.execute(reqDto, memberId, false);
        return CommonApiResponse.success("수정되었습니다.");
    }
}
