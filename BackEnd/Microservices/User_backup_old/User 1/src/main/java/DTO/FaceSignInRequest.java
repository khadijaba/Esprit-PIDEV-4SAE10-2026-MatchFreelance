package DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class FaceSignInRequest {

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le descripteur du visage est requis")
    private String faceDescriptor;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFaceDescriptor() {
        return faceDescriptor;
    }

    public void setFaceDescriptor(String faceDescriptor) {
        this.faceDescriptor = faceDescriptor;
    }
}
