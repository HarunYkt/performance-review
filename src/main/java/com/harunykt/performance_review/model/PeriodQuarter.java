package com.harunykt.performance_review.model;

public enum PeriodQuarter {

    Q1,Q2,Q3,Q4;


    public static PeriodQuarter fromMonth(int month) {
        if (month <= 3) return Q1;
        if (month<=6) return Q2;
        if (month<=9) return Q3;
        return Q4;
    }

    public static PeriodQuarter fromString (String s ) {
        return PeriodQuarter.valueOf(s.trim().toUpperCase() );
    }
}
