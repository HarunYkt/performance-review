package com.harunykt.performance_review.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.harunykt.performance_review.model.PeriodQuarter;

import java.time.LocalDate;

@Entity
@Table(name = "evaluations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_eval_period",
                columnNames = {"evaluator_id","evaluated_id","type", "period_year","period_quarter"}
        )
)
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String employeeEmail;

    private Integer periodYear;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private PeriodQuarter periodQuarter;

    // Değerlendirmeyi yapan (anonim tutulacak ama DB ilişkisi gerekli)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    // Değerlendirilen kişi
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluated_id", nullable = false)
    private User evaluated;

    // 1–5 arası puan
    @Min(1) @Max(5)
    @Column(nullable = false)
    private int score;

    // Yoruma açık alan
    @Column(length = 2000)
    private String comment;

    // Değerlendirme türü
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EvaluationType type;

    // Değerlendirmenin tarihi
    @Column(nullable = false)
    private LocalDate date;

    /* ---------- Lifecycle ---------- */
    @PrePersist
    protected void onCreate() {
        if (this.date == null) {
            this.date = java.time.LocalDate.now();
        }
        if (this.periodYear == null) {
            this.periodYear = this.date.getYear();
        }
        if (this.periodQuarter == null) {
            this.periodQuarter = PeriodQuarter.fromMonth(this.date.getMonthValue());
        }
    }


    /* ---------- Getters / Setters ---------- */

    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }

    public PeriodQuarter getPeriodQuarter() { return periodQuarter; }
    public void setPeriodQuarter(PeriodQuarter periodQuarter) { this.periodQuarter = periodQuarter; }


    public Long getId() { return id; }

    public User getEvaluator() { return evaluator; }
    public void setEvaluator(User evaluator) { this.evaluator = evaluator; }

    public User getEvaluated() { return evaluated; }
    public void setEvaluated(User evaluated) { this.evaluated = evaluated; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public EvaluationType getType() { return type; }
    public void setType(EvaluationType type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
