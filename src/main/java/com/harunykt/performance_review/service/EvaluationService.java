package com.harunykt.performance_review.service;

import com.harunykt.performance_review.model.*;
import com.harunykt.performance_review.repository.EvaluationRepository;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Table(
        name = "evaluations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_eval_period",
                columnNames = {"evaluator_id", "evaluated_id","type","period_year","period_quarter"}
        )
)
@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;

    @Autowired
    public EvaluationService(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    // ✅ Değerlendirme kaydet
    public Evaluation createEvaluation ( User evaluator,
                                         User evaluated,
                                         int score,
                                         String comment,
                                         EvaluationType type) {
        //Kullanıcı kendine Değerlendiremez
        if (evaluator.getId().equals(evaluated.getId())){
            throw new IllegalArgumentException("Kişi kendisini Değerlendiremez");
        }
        //Rol Kuralları

        switch (type) {
            case PEER_TO_PEER -> {
                if (evaluator.getRole() != UserRole.EMPLOYEE || evaluated.getRole() != UserRole.EMPLOYEE) {
                    throw new IllegalArgumentException("PEER_TO_PEER Sadece EMPLOYE -> EMPLOYE için geçerlidir.");
                }
            }
            case PEER_TO_MANAGER -> {
                if (evaluator.getRole()!= UserRole.EMPLOYEE || evaluated.getRole()!=UserRole.MANAGER) {
                    throw new IllegalArgumentException("PEER_TP_MANAGER Sadece EMPLOYEE -> MANAGER İçin Geçerlidir");
                }
            }
            case MANAGER_TO_EMPLOYEE -> {
                if (evaluator.getRole()!= UserRole.MANAGER || evaluated.getRole()!=UserRole.EMPLOYEE) {
                    throw new IllegalArgumentException("MANAGER_TO_EMPLOYEE Sadece MANAGER -> EMPLOYEE İçin geçerlidir");

                }
            }
            default -> throw new IllegalArgumentException("Desteklenmeyen Değerlendirme türü");
        }

        LocalDate now = LocalDate.now();
        //Kayıt oluşturma
        Evaluation evaluation = new Evaluation();
        evaluation.setEvaluator(evaluator);
        evaluation.setEvaluated(evaluated);
        evaluation.setScore(score);
        evaluation.setComment(comment);
        evaluation.setType(type);
        evaluation.setDate(now);


        evaluation.setPeriodYear(now.getYear());
        evaluation.setPeriodQuarter(PeriodQuarter.fromMonth(now.getMonthValue()));

        var existing = evaluationRepository
                .findFirstByEvaluatorIdAndEvaluatedIdAndTypeAndPeriodYearAndPeriodQuarter(
                        evaluator.getId(), evaluation.getId(), type, evaluation.getPeriodYear(),evaluation.getPeriodQuarter()
                );

        System.out.println("DEBUG => evaluatorId=" + evaluator.getId()
                + ", evaluatedId=" + evaluated.getId()
                + ", type=" + type
                + ", year=" + evaluation.getPeriodYear()
                + ", quarter=" + evaluation.getPeriodQuarter());


        if (existing.isPresent()) {
            throw new IllegalArgumentException("Bu dönemde bu kullanıcıyı aynı türde değerlendirdiniz.");
        }

        return evaluationRepository.saveAndFlush(evaluation);


    }
    // ✅ Belirli bir kullanıcıya yapılan tüm değerlendirmeler (gizlilik filtrelenir controller’da)
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

    // ✅ Ortalama skor
    public double getAverageScore(User user, EvaluationType type) {
        List<Evaluation> evaluations = evaluationRepository.findByEvaluatedAndType(user, type);
        if (evaluations.isEmpty()) return 0.0;

        return evaluations.stream()
                .mapToInt(Evaluation::getScore)
                .average()
                .orElse(0.0);
    }
}
