package com.harunykt.performance_review.dto;

public class UserDto {

    private Long id;
    private String fullName;
    private String email;
    private String role;


    public UserDto (Long id, String fullName, String email, String role) {
        this.id=id;
        this.fullName=fullName;
        this.email=email;
        this.role=role;
    }

    public Long getId(){return id;}
    public String getFullName() {return fullName;}
    public String getEmail() {return email; }
    public String getRole() {return role;}



}
