package Controller;

import DTO.UserProfileDTO;
import Entity.User;
import Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Value("${welcome.message}")
    private String welcomeMessage;
    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage;
    }
    // Get the currently authenticated user
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // Utiliser un DTO pour ne pas exposer l'entité directement (sécurité)
        UserProfileDTO userDTO = new UserProfileDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setAddress(user.getAddress());
        userDTO.setEmail(user.getEmail());
        userDTO.setBirthDate(user.getBirthDate());
        userDTO.setRole(user.getRole());
        userDTO.setProfilePicture(user.getProfilePictureUrl());
        
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Update allowed fields
            if (updatedUser.getFirstName() != null)
                user.setFirstName(updatedUser.getFirstName());
            if (updatedUser.getLastName() != null)
                user.setLastName(updatedUser.getLastName());
            if (updatedUser.getAddress() != null)
                user.setAddress(updatedUser.getAddress());
            if (updatedUser.getBirthDate() != null)
                user.setBirthDate(updatedUser.getBirthDate());
            // Role and Password should NOT be updated here for security reasons
            // (Password has its own flow)

            userRepository.save(user);
            user.setPassword(null); // Strip password for response
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile: " + e.getMessage());
        }
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");

            // Define the upload directory
            Path uploadDir = Paths.get("uploads", "avatars");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename to avoid overriding other users
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                extension = originalFilename.substring(i);
            }
            String newFilename = "user_" + user.getId() + "_" + UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadDir.resolve(newFilename);

            // Save file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Construct the URL exactly as mapped by WebMvcConfig
            String fileUrl = "/uploads/avatars/" + newFilename;

            // Delete old avatar if it exists (Optional, but good for saving disk space)
            if (user.getProfilePictureUrl() != null && user.getProfilePictureUrl().startsWith("/uploads/avatars/")) {
                try {
                    String oldFileName = user.getProfilePictureUrl().replace("/uploads/avatars/", "");
                    Path oldFilePath = uploadDir.resolve(oldFileName);
                    Files.deleteIfExists(oldFilePath);
                } catch (Exception e) {
                    System.out.println("Could not delete old avatar: " + e.getMessage());
                }
            }

            // Update user entity
            user.setProfilePictureUrl(fileUrl);
            userRepository.save(user);

            return ResponseEntity.ok().body("{\"url\": \"" + fileUrl + "\"}");

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not store file. Please try again.");
        }
    }
}
