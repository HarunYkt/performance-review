package com.harunykt.performance_review.controller;

import com.harunykt.performance_review.dto.AnonymousEvaluationDTO;
import com.harunykt.performance_review.dto.SelfSummaryDTO;
import com.harunykt.performance_review.model.Evaluation;
import com.harunykt.performance_review.model.EvaluationType;
import com.harunykt.performance_review.model.PeriodQuarter;
import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import com.harunykt.performance_review.service.EvaluationService;
import com.harunykt.performance_review.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final UserService userService;

    @Autowired
    public EvaluationController(EvaluationService evaluationService, UserService userService) {
        this.evaluationService = evaluationService;
        this.userService = userService;
    }

    // 0) Tüm değerlendirmeleri listele (sadece yöneticiler)
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEvaluations(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PeriodQuarter quarter,
            @RequestParam(required = false) Long evaluatorId,
            @RequestParam(required = false) Long evaluatedId
    ) {
        List<Evaluation> evaluations = evaluationService.getAllEvaluations();

        // Filtreleme
        List<Evaluation> filteredEvaluations = evaluations.stream()
                .filter(e -> year == null || year.equals(e.getPeriodYear()))
                .filter(e -> quarter == null || e.getPeriodQuarter() == quarter)
                .filter(e -> evaluatorId == null || e.getEvaluator().getId().equals(evaluatorId))
                .filter(e -> evaluatedId == null || e.getEvaluated().getId().equals(evaluatedId))
                .toList();

        // DTO formatında döndür
        List<Map<String, Object>> result = filteredEvaluations.stream()
                .map(e -> {
                    Map<String, Object> evalMap = new HashMap<>();
                    evalMap.put("id", e.getId());
                    evalMap.put("evaluatorName", e.getEvaluator().getFullName());
                    evalMap.put("evaluatorEmail", e.getEvaluator().getEmail());
                    evalMap.put("evaluatedName", e.getEvaluated().getFullName());
                    evalMap.put("evaluatedEmail", e.getEvaluated().getEmail());
                    evalMap.put("score", e.getScore());
                    evalMap.put("comment", e.getComment());
                    evalMap.put("type", e.getType());
                    evalMap.put("periodYear", e.getPeriodYear());
                    evalMap.put("periodQuarter", e.getPeriodQuarter());
                    evalMap.put("date", e.getDate());
                    return evalMap;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    // 1) Giriş yapan kişinin anonim özeti (isteğe bağlı yıl/çeyrek filtresi)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<SelfSummaryDTO> mySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PeriodQuarter quarter,
            Authentication auth
    ) {
        User me = userService.getByEmail(auth.getName());
        var dto = evaluationService.buildSelfSummary(me.getId(), year, quarter);
        return ResponseEntity.ok(dto);
    }

    // 2) Değerlendirme ekleme (yıl/çeyrek isteğe bağlı; yoksa otomatik doldurulur)
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<?> createEvaluation(
            @RequestBody @Valid CreateEvaluationRequest request,
            Authentication auth
    ) {
        User evaluator = userService.getByEmail(auth.getName());
        User evaluated  = userService.getByEmail(request.getEmployeeEmail());

        // Debug: Manager ilişkilerini kontrol et
        System.out.println("=== DEBUG INFO ===");
        System.out.println("Evaluator: " + evaluator.getFullName() + " (" + evaluator.getRole() + ")");
        System.out.println("Evaluator Manager: " + (evaluator.getManager() != null ? evaluator.getManager().getFullName() : "NULL"));
        System.out.println("Evaluated: " + evaluated.getFullName() + " (" + evaluated.getRole() + ")");
        System.out.println("Evaluated Manager: " + (evaluated.getManager() != null ? evaluated.getManager().getFullName() : "NULL"));
        System.out.println("==================");

        // Dönem alanları boş ise periodStart tarihinden hesapla
        Integer year = request.getPeriodYear();
        PeriodQuarter quarter = request.getPeriodQuarter();

        if (year == null || quarter == null) {
            LocalDate dateToUse;

            // periodStart varsa onu kullan, yoksa bugünü kullan
            if (request.getPeriodStart() != null && !request.getPeriodStart().isEmpty()) {
                try {
                    dateToUse = LocalDate.parse(request.getPeriodStart());
                } catch (Exception e) {
                    dateToUse = LocalDate.now();
                }
            } else {
                dateToUse = LocalDate.now();
            }

            if (year == null) year = dateToUse.getYear();
            if (quarter == null) {
                int m = dateToUse.getMonthValue();
                if (m <= 3) quarter = PeriodQuarter.Q1;
                else if (m <= 6) quarter = PeriodQuarter.Q2;
                else if (m <= 9) quarter = PeriodQuarter.Q3;
                else quarter = PeriodQuarter.Q4;
            }
        }

        // Evaluation type'ı otomatik belirle
        EvaluationType evaluationType;
        if (evaluator.getId().equals(evaluated.getId())) {
            evaluationType = EvaluationType.SELF_EVALUATION;
        } else if (evaluator.getRole() == UserRole.MANAGER && evaluated.getRole() == UserRole.EMPLOYEE) {
            evaluationType = EvaluationType.MANAGER_TO_EMPLOYEE;
        } else if (evaluator.getRole() == UserRole.EMPLOYEE && evaluated.getRole() == UserRole.MANAGER) {
            evaluationType = EvaluationType.PEER_TO_MANAGER;
        } else if (evaluator.getRole() == UserRole.EMPLOYEE && evaluated.getRole() == UserRole.EMPLOYEE) {
            evaluationType = EvaluationType.PEER_TO_PEER;
        } else {
            throw new IllegalArgumentException("Desteklenmeyen değerlendirme türü kombinasyonu.");
        }

        // Tek bir entity oluşturup servise veriyoruz
        Evaluation e = new Evaluation();
        e.setEvaluated(evaluated);
        e.setType(evaluationType);
        e.setScore(request.getScore());
        e.setComment(request.getComments());
        e.setPeriodYear(year);
        e.setPeriodQuarter(quarter);

        var created = evaluationService.createEvaluation(evaluator, e);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Değerlendirme başarıyla kaydedildi");
        resp.put("id", created.getId());
        resp.put("periodYear", created.getPeriodYear());
        resp.put("periodQuarter", created.getPeriodQuarter());
        return ResponseEntity.status(201).body(resp);
    }

    // 3) Giriş yapan kullanıcının yaptığı değerlendirmeler
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/given")
    public ResponseEntity<List<Map<String, Object>>> getMyEvaluations(Authentication auth) {
        User evaluator = userService.getByEmail(auth.getName());
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEvaluator(evaluator);

        // DTO formatında döndür (Hibernate proxy sorununu önlemek için)
        List<Map<String, Object>> result = evaluations.stream()
                .map(e -> {
                    Map<String, Object> evalMap = new HashMap<>();
                    evalMap.put("id", e.getId());
                    evalMap.put("evaluatedName", e.getEvaluated().getFullName());
                    evalMap.put("evaluatedEmail", e.getEvaluated().getEmail());
                    evalMap.put("score", e.getScore());
                    evalMap.put("comment", e.getComment());
                    evalMap.put("type", e.getType());
                    evalMap.put("periodYear", e.getPeriodYear());
                    evalMap.put("periodQuarter", e.getPeriodQuarter());
                    evalMap.put("date", e.getDate());
                    return evalMap;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    // 3.1) Giriş yapan kullanıcının aldığı değerlendirmeler (profil sayfası için - filtrelenebilir)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/received")
    public ResponseEntity<Map<String, Object>> getMyReceivedEvaluations(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PeriodQuarter quarter,
            @RequestParam(required = false) EvaluationType type,
            Authentication auth
    ) {
        try {
            System.out.println("=== /me/received DEBUG ===");
            System.out.println("Auth name: " + auth.getName());
            System.out.println("Year filter: " + year);
            System.out.println("Quarter filter: " + quarter);
            System.out.println("Type filter: " + type);

            User evaluated = userService.getByEmail(auth.getName());
            System.out.println("Found user: " + evaluated.getFullName() + " (ID: " + evaluated.getId() + ")");

            List<Evaluation> evaluations = evaluationService.getEvaluationsByEvaluated(evaluated);
            System.out.println("Found " + evaluations.size() + " evaluations for user");

            // Filtreleme
            List<Evaluation> filteredEvaluations = evaluations.stream()
                    .filter(e -> year == null || year.equals(e.getPeriodYear()))
                    .filter(e -> quarter == null || e.getPeriodQuarter() == quarter)
                    .filter(e -> type == null || e.getType() == type)
                    .toList();

            System.out.println("After filtering: " + filteredEvaluations.size() + " evaluations");

            // DTO formatında döndür (evaluator bilgisi ile - Hibernate proxy fix)
            List<Map<String, Object>> evaluationList = filteredEvaluations.stream()
                    .map(e -> {
                        try {
                            Map<String, Object> evalMap = new HashMap<String, Object>();
                            evalMap.put("id", e.getId());

                            // Hibernate proxy sorununu önlemek için null check
                            if (e.getEvaluator() != null) {
                                evalMap.put("evaluatorName", e.getEvaluator().getFullName());
                                evalMap.put("evaluatorEmail", e.getEvaluator().getEmail());
                            } else {
                                evalMap.put("evaluatorName", "Bilinmeyen");
                                evalMap.put("evaluatorEmail", "");
                            }

                            evalMap.put("score", e.getScore());
                            evalMap.put("comment", e.getComment());
                            evalMap.put("type", e.getType());
                            evalMap.put("periodYear", e.getPeriodYear());
                            evalMap.put("periodQuarter", e.getPeriodQuarter());
                            evalMap.put("date", e.getDate());
                            return evalMap;
                        } catch (Exception ex) {
                            System.err.println("Error mapping evaluation: " + ex.getMessage());
                            ex.printStackTrace();
                            Map<String, Object> emptyMap = new HashMap<String, Object>();
                            return emptyMap;
                        }
                    })
                    .collect(Collectors.toList());

            // İstatistikler hesapla (safe)
            Map<String, Object> statistics = new HashMap<>();
            try {
                if (!filteredEvaluations.isEmpty()) {
                    double averageScore = filteredEvaluations.stream()
                            .mapToInt(Evaluation::getScore)
                            .average()
                            .orElse(0.0);

                    Map<EvaluationType, Long> typeCount = filteredEvaluations.stream()
                            .collect(Collectors.groupingBy(Evaluation::getType, Collectors.counting()));

                    Map<Integer, Long> yearCount = filteredEvaluations.stream()
                            .filter(e -> e.getPeriodYear() != null)
                            .collect(Collectors.groupingBy(Evaluation::getPeriodYear, Collectors.counting()));

                    statistics.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
                    statistics.put("totalCount", filteredEvaluations.size());
                    statistics.put("typeBreakdown", typeCount);
                    statistics.put("yearBreakdown", yearCount);
                } else {
                    statistics.put("averageScore", 0.0);
                    statistics.put("totalCount", 0);
                    statistics.put("typeBreakdown", new HashMap<>());
                    statistics.put("yearBreakdown", new HashMap<>());
                }
            } catch (Exception e) {
                System.err.println("Error calculating statistics: " + e.getMessage());
                e.printStackTrace();
                // Fallback statistics
                statistics.put("averageScore", 0.0);
                statistics.put("totalCount", 0);
                statistics.put("typeBreakdown", new HashMap<>());
                statistics.put("yearBreakdown", new HashMap<>());
            }

            // Sonuç objesi
            Map<String, Object> result = new HashMap<>();
            result.put("evaluations", evaluationList);
            result.put("statistics", statistics);
            Map<String, Object> filters = new HashMap<>();
            filters.put("year", year);
            filters.put("quarter", quarter);
            filters.put("type", type);
            result.put("filters", filters);

            System.out.println("Returning result with " + evaluationList.size() + " evaluations");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("ERROR in /me/received endpoint: " + e.getMessage());
            e.printStackTrace();

            // Return empty result instead of 500 error
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("evaluations", new ArrayList<>());
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("averageScore", 0.0);
            emptyStats.put("totalCount", 0);
            emptyStats.put("typeBreakdown", new HashMap<>());
            emptyStats.put("yearBreakdown", new HashMap<>());
            emptyResult.put("statistics", emptyStats);
            
            Map<String, Object> emptyFilters = new HashMap<>();
            emptyFilters.put("year", year);
            emptyFilters.put("quarter", quarter);
            emptyFilters.put("type", type);
            emptyResult.put("filters", emptyFilters);

            return ResponseEntity.ok(emptyResult);
        }
    }

    // 4) Yönetici: belirli kullanıcı için anonim özet (+ dönem filtresi)
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/{userId}")
    public ResponseEntity<?> getEvaluationsForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PeriodQuarter quarter,
            Authentication auth
    ) {
        User requester = userService.getByEmail(auth.getName());
        if (requester.getRole() != UserRole.MANAGER) {
            // 403'ü GlobalExceptionHandler düzgün JSON yapısı ile döndürsün
            throw new AccessDeniedException("Bu bilgiye sadece yöneticiler erişebilir.");
        }

        User evaluated = userService.getById(userId);

        // Güvenlik: sadece yöneticisi olduğun çalışanı görebil
        if (evaluated.getManager() == null || !evaluated.getManager().getId().equals(requester.getId())) {
            throw new AccessDeniedException("Sadece yöneticisi olduğunuz çalışanların özetini görebilirsiniz.");
        }

        // Ham listeler
        // Peer değerlendirmeleri: organizasyon tanımına göre PEER_TO_PEER'i baz alıyoruz
        List<Evaluation> peerEvals = evaluationService.getEvaluationsForUser(evaluated)
                .stream()
                .filter(e -> e.getType() == EvaluationType.PEER_TO_PEER)
                .toList();

        // Manager değerlendirmeleri: yöneticiden çalışana
        List<Evaluation> managerEvals = evaluationService.getManagerEvaluationsForUser(evaluated);

        // Dönem filtresi
        List<Evaluation> filteredPeerEvals = peerEvals.stream()
                .filter(e -> year == null || year.equals(e.getPeriodYear()))
                .filter(e -> quarter == null || e.getPeriodQuarter() == quarter)
                .toList();

        List<Evaluation> filteredManagerEvals = managerEvals.stream()
                .filter(e -> year == null || year.equals(e.getPeriodYear()))
                .filter(e -> quarter == null || e.getPeriodQuarter() == quarter)
                .toList();

        // Ortalamalar
        double peerAvg = filteredPeerEvals.stream().mapToInt(Evaluation::getScore).average().orElse(0.0);
        double managerAvg = filteredManagerEvals.stream().mapToInt(Evaluation::getScore).average().orElse(0.0);

        // Anonim yorumlar (puan + yorum)
        List<AnonymousEvaluationDTO> peerComments = filteredPeerEvals.stream()
                .map(e -> new AnonymousEvaluationDTO(e.getScore(), e.getComment()))
                .toList();

        List<AnonymousEvaluationDTO> managerComments = filteredManagerEvals.stream()
                .map(e -> new AnonymousEvaluationDTO(e.getScore(), e.getComment()))
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("user", evaluated.getFullName());
        response.put("peerAverageScore", peerAvg);
        response.put("managerAverageScore", managerAvg);
        response.put("peerEvaluations", peerComments);
        response.put("managerEvaluations", managerComments);

        return ResponseEntity.ok(response);
    }

    // ---- İÇ DTO ----
    public static class CreateEvaluationRequest {
        @NotNull @Size(min = 1, message = "Email boş olamaz") private String employeeEmail;
        @Min(1) @Max(10) private Integer score;          // 1-10 aralığı (projede genelde bu kullanılıyor)
        @Size(max = 500) private String comments;
        private EvaluationType type = EvaluationType.PEER_TO_PEER;  // Default değer
        private String periodStart;   // Frontend'den gelen tarih formatı
        private String periodEnd;     // Frontend'den gelen tarih formatı
        @Min(2000) @Max(2100) private Integer periodYear;   // örn: 2025 (opsiyonel; yoksa otomatik set edilir)
        private PeriodQuarter periodQuarter;                // Q1, Q2, Q3, Q4 (opsiyonel; yoksa otomatik set edilir)

        public String getEmployeeEmail() { return employeeEmail; }
        public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public EvaluationType getType() { return type; }
        public void setType(EvaluationType type) { this.type = type; }
        public Integer getPeriodYear() { return periodYear; }
        public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
        public PeriodQuarter getPeriodQuarter() { return periodQuarter; }
        public void setPeriodQuarter(PeriodQuarter periodQuarter) { this.periodQuarter = periodQuarter; }

        public String getPeriodStart() { return periodStart; }
        public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }

        public String getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }
    }
}
