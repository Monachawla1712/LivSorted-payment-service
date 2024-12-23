package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class UserDetailsResponse {

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("updated_by")
    private String updatedBy;

    @JsonProperty("id")
    private String id;

    @JsonProperty("greeting")
    private String greeting;

    @JsonProperty("name")
    private String name;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("email")
    private String email;

    @JsonProperty("avatar_url")
    private String avatar_url;

    @JsonProperty("phone_confirmed_at")
    private Date phoneNumberConfirmedAt;

    @JsonProperty("email_confirmed_at")
    private Date emailNumberConfirmedAt;

    @JsonProperty("last_sign_in_at")
    private Date lastSignInAt;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_verified")
    private Boolean isVerified;

    @JsonProperty("is_deleted")
    private Boolean isDeleted;

    @JsonProperty("banned_until")
    private Date bannedUntil;

    @JsonProperty("userPreferences")
    private UserPreferences userPreferences;
}
