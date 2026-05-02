package com.user.userservice;

import com.user.userservice.dto.*;
import com.user.userservice.exception.QuotaExceededException;
import com.user.userservice.security.JwtService;
import com.user.userservice.verification.StudentVerification;
import com.user.userservice.verification.StudentVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ActivityService activityService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailProducer emailProducer;
    private final StudentVerificationRepository studentVerificationRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Cas spécial : compte existant mais email non vérifié → on le réinitialise
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            if (!existingUser.get().isEmailVerified()) {
                log.warn("Compte non vérifié détecté pour {}. Réinitialisation et renvoi du code.", request.getEmail());
                // Supprimer tous les anciens codes
                verificationCodeRepository.deleteAllByEmail(request.getEmail());
                userRepository.delete(existingUser.get());
            } else {
                throw new ResourceAlreadyExistsException("Email déjà utilisé");
            }
        }
        if (userRepository.existsByDisplayName(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Nom d'utilisateur déjà pris");
        }

        User user = User.builder()
                .displayName(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .planType(User.PlanType.PRO)
                .subscriptionStatus(User.SubscriptionStatus.ACTIVE)
                .emailVerified(false)
                .active(true)
                .build();

        userRepository.save(user);


        String code = generateCode();
        VerificationCode vc = new VerificationCode(user.getEmail(), code, LocalDateTime.now().plusMinutes(10));
        verificationCodeRepository.save(vc);

        // Appel asynchrone via RabbitMQ
        emailProducer.queueVerificationCode(user.getEmail(), code);
        log.info("Code de vérification envoyé à {}", user.getEmail());

        activityService.logActivity("REGISTRATION", "Nouvel utilisateur: " + user.getDisplayName(), user.getDisplayName());

        return mapToAuthResponse(user, "Inscription réussie. Code (DEBUG): " + code);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Email non vérifié");
        }

        String jwtToken = jwtService.generateToken(user);
        activityService.logActivity("LOGIN", "Connexion: " + user.getDisplayName(), user.getDisplayName());

        return mapToAuthResponse(user, "Connexion réussie.");
    }

    @Transactional
    public AuthResponse verifyEmail(String email, String code) {
        VerificationCode vc = verificationCodeRepository.findFirstByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new ResourceNotFoundException("Code introuvable"));

        if (vc.isUsed() || vc.isExpired()) {
            throw new IllegalArgumentException("Code invalide ou expiré. Veuillez faire une nouvelle demande.");
        }
        // BACKDOOR: Autoriser '123456' pour le test
        if (!vc.getCode().equals(code) && !code.equals("123456")) {
            throw new IllegalArgumentException("Code incorrect");
        }

        vc.setUsed(true);
        verificationCodeRepository.save(vc);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        user.setEmailVerified(true);
        User savedUser = userRepository.save(user);
        log.info("Email vérifié avec succès pour {}", email);

        return mapToAuthResponse(savedUser, "Email vérifié avec succès.");
    }

    private AuthResponse mapToAuthResponse(User user, String message) {

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getDisplayName())
                .email(user.getEmail())
                .token(jwtService.generateToken(user))
                .role(user.getRole().name())
                .userType(user.getRole().name())
                .planType(user.getPlanType().name())
                .isStudentVerified(false)
                .requiresVerification(!user.isEmailVerified())
                .message(message)
                .build();
    }

    /**
     * Renvoie un nouveau code de vérification si le compte existe et n'est pas encore vérifié.
     */
    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte trouvé pour cet email."));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Cet email est déjà vérifié.");
        }

        // Supprimer tous les anciens codes avant d'en créer un nouveau
        verificationCodeRepository.deleteAllByEmail(email);

        String code = generateCode();
        VerificationCode vc = new VerificationCode(email, code, LocalDateTime.now().plusMinutes(10));
        verificationCodeRepository.save(vc);

        emailProducer.queueVerificationCode(email, code);
        log.info("Nouveau code de vérification envoyé à {}", email);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return mapToResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return mapToResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .userType(user.getRole().name())
                .planType(user.getPlanType().name())
                .subscriptionStatus(user.getSubscriptionStatus().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .isStudentVerified(false)
                .dockerfileLimit(-1)
                .deploymentLimit(-1)
                .build();
    }

    @Transactional
    public void upgradePlan(String email, String newPlanName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        User.PlanType newPlan = User.PlanType.valueOf(newPlanName);
        user.setPlanType(newPlan);
        user.setSubscriptionStatus(User.SubscriptionStatus.ACTIVE);
        userRepository.save(user);
        
        activityService.logActivity("UPGRADE", "Passage au plan " + newPlanName, user.getDisplayName());
        log.info("Plan mis à jour pour {}: {}", email, newPlanName);
    }


    @Transactional
    public void updatePassword(String email, UpdatePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        activityService.logActivity("PASSWORD_UPDATE", "Mot de passe mis à jour", user.getDisplayName());
        log.info("Mot de passe mis à jour pour {}", email);
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
}
