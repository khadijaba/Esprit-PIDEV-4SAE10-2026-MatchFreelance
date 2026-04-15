package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetConfirm {
    
    @NotBlank(message = "Le token est obligatoire")
    private String token;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit avoir au moins 6 caractères")
    private String newPassword;
    
    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmPassword;

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
