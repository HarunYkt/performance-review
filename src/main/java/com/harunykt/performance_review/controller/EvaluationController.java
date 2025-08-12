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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 1) Giriş yapan kişinin anonim özeti (isteğe bağlı yıl/çeyrek filtresi)
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

    // 2) Değerlendirme ekleme (yıl/çeyrek dahil)
    @PostMapping
    public ResponseEntity<?> createEvaluation(
            @RequestBody CreateEvaluationRequest request,
            Authentication auth
    ) {
        User evaluator = userService.getByEmail(auth.getName());
        User evaluated  = userService.getById(request.getEvaluatedId());

        // Tek bir entity oluşturup servise veriyoruz
        Evaluation e = new Evaluation();
        e.setEvaluated(evaluated);
        e.setType(request.getType());
        e.setScore(request.getScore());
        e.setComment(request.getComment());
        e.setPeriodYear(request.getPeriodYear());
        e.setPeriodQuarter(request.getPeriodQuarter());

        evaluationService.createEvaluation(evaluator, e);
        return ResponseEntity.ok("Değerlendirme başarıyla kaydedildi");
    }

    // 3) Giriş yapan kullanıcının yaptığı değerlendirmeler
    @GetMapping("/me/given")
    public ResponseEntity<List<Evaluation>> getMyEvaluations(Authentication auth) {
        User evaluator = userService.getByEmail(auth.getName());
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEvaluator(evaluator);
        return ResponseEntity.ok(evaluations);
    }

    // 4) Yönetici: belirli kullanıcı için anonim özet (+ dönem filtresi)
    @GetMapping("/{userId}")
    public ResponseEntity<?> getEvaluationsForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PeriodQuarter quarter,
            Authentication auth
    ) {
        User requester = userService.getByEmail(auth.getName());
        if (requester.getRole() != UserRole.MANAGER) {
            return ResponseEntity.status(403).body("Bu bilgiye sadece yöneticiler erişebilir.");
        }

        User evaluated = userService.getById(userId);

        // Güvenlik: sadece yöneticisi olduğun çalışanı görebil
        if (evaluated.getManager() == null || !evaluated.getManager().getId().equals(requester.getId())) {
            return ResponseEntity.status(403).body("Sadece yöneticisi olduğunuz çalışanların özetini görebilirsiniz.");
        }

        // Ham listeler
        List<Evaluation> peerEvals = evaluationService.getEvaluationsForUser(evaluated)
                .stream()
                .filter(e -> e.getType() == EvaluationType.PEER_TO_PEER
                        || e.getType() == EvaluationType.PEER_TO_MANAGER)
                .toList();

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
        private Long evaluatedId;
        private Integer score;
        private String comment;
        private EvaluationType type;
        private Integer periodYear;          // örn: 2025
        private PeriodQuarter periodQuarter; // Q1, Q2, Q3, Q4

        public Long getEvaluatedId() { return evaluatedId; }
        public void setEvaluatedId(Long evaluatedId) { this.evaluatedId = evaluatedId; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public EvaluationType getType() { return type; }
        public void setType(EvaluationType type) { this.type = type; }
        public Integer getPeriodYear() { return periodYear; }
        public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
        public PeriodQuarter getPeriodQuarter() { return periodQuarter; }
        public void setPeriodQuarter(PeriodQuarter periodQuarter) { this.periodQuarter = periodQuarter; }
    }
}
