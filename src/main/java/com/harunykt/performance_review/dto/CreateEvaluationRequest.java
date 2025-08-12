package com.harunykt.performance_review.dto;

import com.harunykt.performance_review.model.EvaluationType;
import com.harunykt.performance_review.model.PeriodQuarter;

public class CreateEvaluationRequest {

    private  Long evaluatedId;
    private Integer score;
    private String comment;
    private EvaluationType type;
    private Integer periodYear;
    private PeriodQuarter periodQuarter;

    public Long getEvaluatedId() {
        return evaluatedId;
    }

    public void setEvaluatedId(Long evaluatedId) {
        this.evaluatedId=evaluatedId;

    }

    public Integer getScore () {
        return score;
    }

    public void setScore (Integer score) {
        this.score=score;

    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment=comment;
    }

    public EvaluationType getType() {
        return type;
    }

    public void setType(EvaluationType type) {
        this.type=type;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear=periodYear;

    }

    public PeriodQuarter getPeriodQuarter() {
        return periodQuarter;
    }

    public void setPeriodQuarter(PeriodQuarter periodQuarter) {
        this.periodQuarter=periodQuarter;
    }


}
