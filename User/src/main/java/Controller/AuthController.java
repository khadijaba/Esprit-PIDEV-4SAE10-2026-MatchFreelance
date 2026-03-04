package Controller;

import DTO.*;
import Entity.User;
import Security.JwtUtils;
import Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    // ─── SIGN UP ────────────────────────────────────────────────
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest request) {
        try {
            User user = userService.register(request);
            return ResponseEntity.ok("Compte créé avec succès pour : " + user.getEmail());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── SIGN IN ────────────────────────────────────────────────
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            String email = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority()
                    .replace("ROLE_", "");

            String token = jwtUtils.generateToken(email, role);

            // Message de redirection selon le rôle
            String message = switch (role) {
                case "ADMIN"         -> "Redirection vers le Dashboard Admin";
                case "PROJECT_OWNER" -> "Redirection vers le Dashboard Project Owner";
                case "FREELANCER"    -> "Redirection vers le Dashboard Freelancer";
                default              -> "Connexion réussie";
            };

            return ResponseEntity.ok(new JwtResponse(token, email,
                    Entity.Role.valueOf(role), message));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Email ou mot de passe incorrect");
        }
    }

    // ─── PASSWORD RESET ────────────────────────────────────────────────
    @PostMapping("/reset-password/request")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        try {
            userService.requestPasswordReset(request);
            return ResponseEntity.ok("Un email de réinitialisation a été envoyé à " + request.getEmail());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
        try {
            String result = userService.confirmPasswordReset(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reset-password/confirm")
    public ResponseEntity<?> showResetForm(@RequestParam String token) {
        try {
            // This endpoint can be used by a frontend to show the reset form
            return ResponseEntity.ok("Token valide. Veuillez utiliser POST /api/auth/reset-password/confirm avec le token et le nouveau mot de passe.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Token invalide ou expiré");
        }
    }

    // ─── EMAIL VERIFICATION ────────────────────────────────────────────────
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        try {
            String result = userService.verifyEmail(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> showVerificationForm(@RequestParam String token, @RequestParam String email) {
        try {
            // This endpoint can be used by a frontend to show verification form
            return ResponseEntity.ok("Token valide. Veuillez utiliser POST /api/auth/verify-email avec le token et l'email.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Token invalide ou expiré");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        try {
            String result = userService.resendVerificationEmail(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}