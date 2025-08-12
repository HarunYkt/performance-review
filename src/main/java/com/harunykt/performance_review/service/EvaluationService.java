package com.harunykt.performance_review.service;

import com.harunykt.performance_review.dto.SelfSummaryDTO;
import com.harunykt.performance_review.model.Evaluation;
import com.harunykt.performance_review.model.EvaluationType;
import com.harunykt.performance_review.model.PeriodQuarter;
import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import com.harunykt.performance_review.repository.EvaluationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final UserService userService; // ilişki ve id kontrolleri için

    @Autowired
    public EvaluationService(EvaluationRepository evaluationRepository, UserService userService) {
        this.evaluationRepository = evaluationRepository;
        this.userService = userService;
    }

    /**
     * Controller'ın çağırdığı yeni imza:
     *   createEvaluation(evaluator, evaluationEntity)
     *
     * Notlar:
     * - periodYear/periodQuarter request’ten gelmişse kullanır, yoksa "şimdi"den hesaplar.
     * - İlişki kuralları (manager/peer) ve duplicate kontrolü burada yapılır.
     */
    public Evaluation createEvaluation(User evaluator, Evaluation evaluation) {
        // evaluated'ı DB'den güvenle çek (id doğrulama)
        User evaluated = userService.getById(evaluation.getEvaluated().getId());

        // kişi kendini değerlendiremez
        if (evaluator.getId().equals(evaluated.getId())) {
            throw new IllegalArgumentException("Kişi kendisini değerlendiremez.");
        }

        // --- DÖNEM BİLGİLERİ ---
        LocalDate now = LocalDate.now();
        Integer year = evaluation.getPeriodYear() != null ? evaluation.getPeriodYear() : now.getYear();
        PeriodQuarter quarter = evaluation.getPeriodQuarter() != null
                ? evaluation.getPeriodQuarter()
                : PeriodQuarter.fromMonth(now.getMonthValue());

        evaluation.setPeriodYear(year);
        evaluation.setPeriodQuarter(quarter);
        evaluation.setDate(now);

        // --- İLİŞKİ KURALLARI ---
        switch (evaluation.getType()) {
            case MANAGER_TO_EMPLOYEE -> {
                if (evaluator.getRole() != UserRole.MANAGER || evaluated.getRole() != UserRole.EMPLOYEE) {
                    throw new IllegalArgumentException("MANAGER_TO_EMPLOYEE yalnızca MANAGER → EMPLOYEE için geçerlidir.");
                }
                if (evaluated.getManager() == null || !evaluated.getManager().getId().equals(evaluator.getId())) {
                    throw new IllegalArgumentException("Sadece yöneticisi olduğunuz çalışanı değerlendirebilirsiniz.");
                }
            }
            case PEER_TO_MANAGER -> {
                if (evaluator.getRole() != UserRole.EMPLOYEE || evaluated.getRole() != UserRole.MANAGER) {
                    throw new IllegalArgumentException("PEER_TO_MANAGER yalnızca EMPLOYEE → MANAGER için geçerlidir.");
                }
                if (evaluator.getManager() == null || !evaluator.getManager().getId().equals(evaluated.getId())) {
                    throw new IllegalArgumentException("Sadece kendi yöneticinizi değerlendirebilirsiniz.");
                }
            }
            case PEER_TO_PEER -> {
                if (evaluator.getRole() != UserRole.EMPLOYEE || evaluated.getRole() != UserRole.EMPLOYEE) {
                    throw new IllegalArgumentException("PEER_TO_PEER yalnızca aynı seviyedeki çalışanlar arasında yapılır.");
                }
                if (evaluator.getManager() == null || evaluated.getManager() == null ||
                        !evaluator.getManager().getId().equals(evaluated.getManager().getId())) {
                    throw new IllegalArgumentException("Aynı yöneticiye bağlı çalışanlar birbirini değerlendirebilir.");
                }
            }
            default -> throw new IllegalArgumentException("Desteklenmeyen değerlendirme türü.");
        }

        // --- DUPLICATE KONTROL ---
        var existing = evaluationRepository
                .findFirstByEvaluatorIdAndEvaluatedIdAndTypeAndPeriodYearAndPeriodQuarter(
                        evaluator.getId(),
                        evaluated.getId(),
                        evaluation.getType(),
                        year,
                        quarter
                );
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Bu dönemde bu kullanıcıyı aynı türde zaten değerlendirdiniz.");
        }

        // kaydet
        evaluation.setEvaluator(evaluator);
        evaluation.setEvaluated(evaluated);
        return evaluationRepository.saveAndFlush(evaluation);
    }

    // ✅ Belirli bir kullanıcıya yapılan tüm değerlendirmeler
    public List<Evaluation> getEvaluationsForUser(User user) {
        return evaluationRepository.findByEvaluated(user);
    }

    // ✅ Belirli bir kullanıcıya yapılmış sadece MANAGER değerlendirmeleri
    public List<Evaluation> getManagerEvaluationsForUser(User user) {
        return evaluationRepository.findByEvaluatedAndType(user, EvaluationType.MANAGER_TO_EMPLOYEE);
    }

    // ✅ Giriş yapan kişinin yaptığı değerlendirmeler
    public List<Evaluation> getEvaluationsByEvaluator(User evaluator) {
        return evaluationRepository.findByEvaluator(evaluator);
    }

    // ✅ Ortalama skor (gerekirse)
    public double getAverageScore(User user, EvaluationType type) {
        List<Evaluation> evaluations = evaluationRepository.findByEvaluatedAndType(user, type);
        if (evaluations.isEmpty()) return 0.0;
        return evaluations.stream().mapToInt(Evaluation::getScore).average().orElse(0.0);
    }

    // ✅ “Benim özetim” DTO derleyici
    public SelfSummaryDTO buildSelfSummary(Long userId, Integer year, PeriodQuarter quarter) {
        List<Evaluation> list;
        if (year != null && quarter != null) {
            list = evaluationRepository.findByEvaluatedIdAndPeriodYearAndPeriodQuarter(userId, year, quarter);
        } else if (year != null) {
            list = evaluationRepository.findByEvaluatedIdAndPeriodYear(userId, year);
        } else {
            list = evaluationRepository.findByEvaluatedId(userId);
        }

        Map<EvaluationType, Double> avg = list.stream()
                .collect(Collectors.groupingBy(Evaluation::getType, Collectors.averagingInt(Evaluation::getScore)));

        Map<EvaluationType, Long> cnt = list.stream()
                .collect(Collectors.groupingBy(Evaluation::getType, Collectors.counting()));

        List<SelfSummaryDTO.CommentItem> comments = list.stream()
                .filter(e -> e.getComment() != null && !e.getComment().isBlank())
                .map(e -> {
                    var c = new SelfSummaryDTO.CommentItem();
                    c.setType(e.getType());
                    c.setYear(e.getPeriodYear());
                    c.setQuarter(e.getPeriodQuarter());
                    c.setComment(e.getComment()); // anonim: evaluator bilgisi yok
                    return c;
                })
                .toList();

        var dto = new SelfSummaryDTO();
        dto.setUserId(userId);
        dto.setYear(year);
        dto.setQuarter(quarter);
        dto.setAverages(avg);
        dto.setCounts(cnt);
        dto.setComments(comments);
        return dto;
    }
}
