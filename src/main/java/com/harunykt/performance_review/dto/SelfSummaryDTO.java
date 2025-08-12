package com.harunykt.performance_review.dto;

import com.harunykt.performance_review.model.EvaluationType;
import com.harunykt.performance_review.model.PeriodQuarter;

import java.util.List;
import java.util.Map;

public class SelfSummaryDTO {

    private Long userId;
    private Integer year;
    private PeriodQuarter quarter;
    private Map<EvaluationType,Double> averages;
    private  Map<EvaluationType, Long> counts;
    private List<SelfSummaryDTO.CommentItem> comments;



    public static class CommentItem {
        private EvaluationType type;
        private Integer year;
        private PeriodQuarter quarter;
        private String  comment;


        //Getter & Setters
    public EvaluationType getType() {
        return type;
    }

    public void setType(EvaluationType type) {
        this.type = type;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;

    }

    public PeriodQuarter getQuarter(){
        return quarter;
    }

    public void setQuarter(PeriodQuarter quarter) {
        this.quarter=quarter;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment=comment;
    }



    }

    //Getters & Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year=year;
    }

    public PeriodQuarter getQuarter(){
        return quarter;
    }

    public void setQuarter(PeriodQuarter quarter) {
        this.quarter=quarter;
    }

    public Map<EvaluationType, Double> getAverages() {
        return averages;
    }

    public void setAverages(Map<EvaluationType, Double> averages) {
        this.averages = averages;
    }

    public Map<EvaluationType, Long> getCounts() {
        return counts;
    }

    public void setCounts(Map<EvaluationType, Long> counts) {
        this.counts = counts;
    }

    public List<CommentItem> getComments() {
        return comments;
    }

    public void setComments(List<CommentItem> comments) {
        this.comments = comments;
    }

}
