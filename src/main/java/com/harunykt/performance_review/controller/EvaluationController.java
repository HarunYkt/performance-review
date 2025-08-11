package com.harunykt.performance_review.controller;

import com.harunykt.performance_review.dto.AnonymousEvaluationDTO;
import com.harunykt.performance_review.model.Evaluation;
import com.harunykt.performance_review.model.EvaluationType;
import com.harunykt.performance_review.model.PeriodQuarter;
import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import com.harunykt.performance_review.service.EvaluationService;
import com.harunykt.performance_review.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

    // ✅ 1) Değerlendirme ekleme
    @PostMapping
    public ResponseEntity<?> createEvaluation(
            @RequestBody EvaluationRequest request,
            Authentication authentication
    ) {
        try {
            User evaluator = userService.findByEmail(authentication.getName()).orElseThrow();
            User evaluated = userService.findById(request.getEvaluatedId()).orElseThrow();

            evaluationService.createEvaluation(
                    evaluator,
                    evaluated,
                    request.getScore(),
                    request.getComment(),
                    request.getType()
            );

            return ResponseEntity.ok("Değerlendirme başarıyla kaydedildi");

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (DataIntegrityViolationException ex) {
            // UNIQUE kısıtı ihlali (aynı dönem ve aynı türde tekrar değerlendirme)
            return ResponseEntity.badRequest().body("Bu dönemde bu kullanıcıyı aynı türde zaten değerlendirdiniz.");
        }
    }

    // ✅ 2) Giriş yapan kişinin yaptığı değerlendirmeler
    @GetMapping("/me/given")
    public ResponseEntity<?> getMyEvaluations(Authentication authentication) {
        User evaluator = userService.findByEmail(authentication.getName()).orElseThrow();
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEvaluator(evaluator);
        return ResponseEntity.ok(evaluations);
    }

    // ✅ 3) Yönetici: kullanıcı için anonim özet (+ dönem filtresi)
    @GetMapping("/{userId}")
    public ResponseEntity<?> getEvaluationsForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            Authentication authentication
    ) {
        User requester = userService.findByEmail(authentication.getName()).orElseThrow();
        if (requester.getRole() != UserRole.MANAGER) {
            return ResponseEntity.status(403).body("Bu bilgiye sadece yöneticiler erişebilir.");
        }

        // quarter -> enum
        PeriodQuarter q = null;
        if (quarter != null && !quarter.isBlank()) {
            q = PeriodQuarter.fromString(quarter); // Q1 | Q2 | Q3 | Q4
        }

        // 🔒 Lambdalar için final kopyalar
        final Integer filterYear = year;
        final PeriodQuarter filterQuarter = q;

        User evaluated = userService.findById(userId).orElseThrow();

        // Ham listeler
        List<Evaluation> peerEvals = evaluationService.getEvaluationsForUser(evaluated)
                .stream()
                .filter(e -> e.getType() == EvaluationType.PEER_TO_PEER
                        || e.getType() == EvaluationType.PEER_TO_MANAGER)
                .toList();

        List<Evaluation> managerEvals = evaluationService.getManagerEvaluationsForUser(evaluated);

        // Dönem filtresi (tek akışta)
        List<Evaluation> filteredPeerEvals = peerEvals.stream()
                .filter(e -> filterYear == null || filterYear.equals(e.getPeriodYear()))
                .filter(e -> filterQuarter == null || e.getPeriodQuarter() == filterQuarter)
                .toList();

        List<Evaluation> filteredManagerEvals = managerEvals.stream()
                .filter(e -> filterYear == null || filterYear.equals(e.getPeriodYear()))
                .filter(e -> filterQuarter == null || e.getPeriodQuarter() == filterQuarter)
                .toList();

        // Ortalamalar
        double peerAvg = filteredPeerEvals.stream().mapToInt(Evaluation::getScore).average().orElse(0.0);
        double managerAvg = filteredManagerEvals.stream().mapToInt(Evaluation::getScore).average().orElse(0.0);

        // Anonim yorumlar
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

    // İç DTO (basit istek gövdesi)
    public static class EvaluationRequest {
        private Long evaluatedId;
        private int score;
        private String comment;
        private EvaluationType type;

        public Long getEvaluatedId() { return evaluatedId; }
        public void setEvaluatedId(Long evaluatedId) { this.evaluatedId = evaluatedId; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public EvaluationType getType() { return type; }
        public void setType(EvaluationType type) { this.type = type; }
    }
}
