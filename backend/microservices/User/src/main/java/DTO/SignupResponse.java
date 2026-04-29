package DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Réponse JSON de {@code POST /api/auth/signup} (le client Angular attend du JSON, pas du texte brut). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignupResponse(String message, String email, String verificationCode) {}
