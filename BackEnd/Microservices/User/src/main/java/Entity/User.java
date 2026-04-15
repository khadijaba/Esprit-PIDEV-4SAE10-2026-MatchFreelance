package Entity;

import Entity.Role;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Access(AccessType.FIELD)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDate birthDate;

    /**
     * Rôle en base : colonne {@code user_role}.
     * Le champ s’appelle {@code userRole} pour éviter que Hibernate ne mappe {@code getRole()} sur la colonne {@code role}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private Role userRole;

    @Column(columnDefinition = "TEXT")
    private String faceDescriptor;

    @Column(length = 255)
    private String profilePictureUrl;

    private boolean enabled = true;

    // Constructors
    public User() {
    }

    public User(Long id, String firstName, String lastName, String address, String email, String password,
            LocalDate birthDate, Role userRole, boolean enabled, String faceDescriptor, String profilePictureUrl) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
        this.userRole = userRole;
        this.enabled = enabled;
        this.faceDescriptor = faceDescriptor;
        this.profilePictureUrl = profilePictureUrl;
    }

    // Builder
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String firstName;
        private String lastName;
        private String address;
        private String email;
        private String password;
        private LocalDate birthDate;
        private Role userRole;
        private String faceDescriptor;
        private String profilePictureUrl;
        private boolean enabled = true;

        public UserBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserBuilder address(String address) {
            this.address = address;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public UserBuilder role(Role role) {
            this.userRole = role;
            return this;
        }

        public UserBuilder faceDescriptor(String faceDescriptor) {
            this.faceDescriptor = faceDescriptor;
            return this;
        }

        public UserBuilder profilePictureUrl(String profilePictureUrl) {
            this.profilePictureUrl = profilePictureUrl;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public User build() {
            return new User(id, firstName, lastName, address, email, password, birthDate, userRole, enabled,
                    faceDescriptor, profilePictureUrl);
        }
    }

    // Getters and setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Role getRole() {
        return userRole;
    }

    public void setRole(Role role) {
        this.userRole = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFaceDescriptor() {
        return faceDescriptor;
    }

    public void setFaceDescriptor(String faceDescriptor) {
        this.faceDescriptor = faceDescriptor;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}