package com.harunykt.performance_review.dto;

public class AnonymousEvaluationDTO {

    private int score;
    private  String comment;

    public AnonymousEvaluationDTO(int score , String comment) {
        this.comment = comment;
        this.score = score;

    }

    public int getScore(){
        return score;
    }

    public String getComment() {
        return comment;
    }
}
