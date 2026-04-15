package DTO;

import Entity.User;

/** Résultat de {@link Service.UserService#register} : compte + code 6 chiffres envoyé par email. */
public record RegistrationResult(User user, String verificationCode) {}
