package com.example.orchestrator.form;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegistrationForm {

    private int userId;
    private String username;
    private String password;

    @JsonProperty("userId")
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
