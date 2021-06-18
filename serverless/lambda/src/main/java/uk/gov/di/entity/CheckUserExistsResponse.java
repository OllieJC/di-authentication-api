package uk.gov.di.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckUserExistsResponse extends BaseAPIResponse {

    @JsonProperty("email")
    private String email;

    @JsonProperty("doesUserExist")
    private boolean doesUserExist;

    public CheckUserExistsResponse(
            @JsonProperty(required = true, value = "email") String email,
            @JsonProperty(required = true, value = "doesUserExist") boolean doesUserExist,
            @JsonProperty(required = true, value = "sessionState") SessionState sessionState) {
        super(sessionState);
        this.email = email;
        this.doesUserExist = doesUserExist;
    }

    public String getEmail() {
        return email;
    }

    public boolean doesUserExist() {
        return doesUserExist;
    }
}