package DTO;


import jakarta.validation.constraints.*;
import lombok.Data;

public class SignInRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
