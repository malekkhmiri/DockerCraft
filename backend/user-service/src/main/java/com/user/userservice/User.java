package com.user.userservice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String displayName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlanType planType = PlanType.PRO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Builder.Default
    private int dockerfileGenerationsCount = 0;

    @Builder.Default
    private int deploymentsCount = 0;

    private LocalDateTime lastQuotaResetDate;
    private LocalDateTime cancellationDate;

    private boolean emailVerified = false;
    private boolean active = true;

    private LocalDateTime createdAt;


    public Long getId()                          { return id; }
    public String getDisplayName()               { return displayName; }
    public String getEmail()                     { return email; }
    public String getPassword()                  { return password; }
    public Role getRole()                        { return role; }
    public PlanType getPlanType()                { return planType; }
    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public int getDockerfileGenerationsCount()   { return dockerfileGenerationsCount; }
    public int getDeploymentsCount()             { return deploymentsCount; }
    public LocalDateTime getLastQuotaResetDate() { return lastQuotaResetDate; }
    public LocalDateTime getCancellationDate()   { return cancellationDate; }
    public boolean isEmailVerified()             { return emailVerified; }
    public boolean isActive()                    { return active; }
    public LocalDateTime getCreatedAt()          { return createdAt; }


    public void setId(Long id)                                           { this.id = id; }
    public void setDisplayName(String displayName)                       { this.displayName = displayName; }
    public void setEmail(String email)                                   { this.email = email; }
    public void setPassword(String password)                             { this.password = password; }
    public void setRole(Role role)                                       { this.role = role; }
    public void setPlanType(PlanType planType)                           { this.planType = planType; }
    public void setSubscriptionStatus(SubscriptionStatus s)              { this.subscriptionStatus = s; }
    public void setDockerfileGenerationsCount(int count)                 { this.dockerfileGenerationsCount = count; }
    public void setDeploymentsCount(int count)                           { this.deploymentsCount = count; }
    public void setLastQuotaResetDate(LocalDateTime d)                   { this.lastQuotaResetDate = d; }
    public void setCancellationDate(LocalDateTime d)                     { this.cancellationDate = d; }
    public void setEmailVerified(boolean emailVerified)                  { this.emailVerified = emailVerified; }
    public void setActive(boolean active)                                { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt)                    { this.createdAt = createdAt; }


    public enum Role {
        USER, ADMIN, STUDENT, WORKER
    }

    public enum PlanType {
        STUDENT_FREE, STUDENT_PAID, PRO
    }

    public enum SubscriptionStatus {
        ACTIVE, CANCELLED, PENDING_DELETION
    }


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastQuotaResetDate == null) {
            lastQuotaResetDate = LocalDateTime.now();
        }
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername()                  { return email; }

    @Override
    public boolean isAccountNonExpired()         { return true; }

    @Override
    public boolean isAccountNonLocked()          { return true; }

    @Override
    public boolean isCredentialsNonExpired()     { return true; }

    @Override
    public boolean isEnabled()                   { return active; }
}