package com.user.userservice;

import com.user.userservice.dto.*;
import com.user.userservice.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "Gestion des utilisateurs et de l'authentification")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @Operation(summary = "S'inscrire")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return new ResponseEntity<>(userService.register(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Se connecter")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(summary = "Vérifier l'email")
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(userService.verifyEmail(request.getEmail(), request.getCode()));
    }

    @Operation(summary = "Renvoyer le code de vérification")
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody VerifyEmailRequest request) {
        userService.resendVerificationCode(request.getEmail());
        return ResponseEntity.ok(java.util.Map.of("message", "Code de vérification renvoyé à " + request.getEmail()));
    }

    @Operation(summary = "Récupérer le profil de l'utilisateur connecté")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@RequestParam(required = false) String email, HttpServletRequest request) {
        String userEmail = resolveEmail(email, request);
        if (userEmail == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(userService.getUserByEmail(userEmail));
    }

    @Operation(summary = "Récupérer les quotas de l'utilisateur")
    @GetMapping("/me/quota")
    public ResponseEntity<QuotaResponse> getMeQuota(@RequestParam(required = false) String email, HttpServletRequest request) {
        return ResponseEntity.ok(QuotaResponse.builder()
                .generationsUsedThisMonth(0)
                .generationsLimit(-1)
                .deploymentsUsedThisMonth(0)
                .deploymentsLimit(-1)
                .build());
    }

    /**
     * Extracts user email from: 1) explicit param, 2) Bearer JWT, 3) Spring Security auth.
     */
    private String resolveEmail(String emailParam, HttpServletRequest request) {
        if (emailParam != null && !emailParam.isEmpty()) return emailParam;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtService.extractUsername(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Operation(summary = "Récupérer un utilisateur par ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Récupérer tous les utilisateurs")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Supprimer un utilisateur")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Mettre à jour le mot de passe")
    @PutMapping("/profile/password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordRequest passwordRequest, HttpServletRequest request) {
        String email = resolveEmail(null, request);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        try {
            userService.updatePassword(email, passwordRequest);
            return ResponseEntity.ok(java.util.Map.of("message", "Mot de passe mis à jour avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Récupérer le profil complet de l'utilisateur (plan, quotas, statut vérification)")
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }
}
