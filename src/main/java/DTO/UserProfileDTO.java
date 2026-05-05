package DTO;

import Entity.Role;
import java.time.LocalDate;

public class UserProfileDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String address;
    private String email;
    private LocalDate birthDate;
    private Role role;
    private String profilePicture;
    
    // Constructeurs
    public UserProfileDTO() {
    }
    
    public UserProfileDTO(Long id, String firstName, String lastName, String address, 
                         String email, LocalDate birthDate, Role role, String profilePicture) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.email = email;
        this.birthDate = birthDate;
        this.role = role;
        this.profilePicture = profilePicture;
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public String getProfilePicture() {
        return profilePicture;
    }
    
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
    
    // Note: Le mot de passe n'est jamais inclus dans le DTO pour des raisons de sécurité
}
