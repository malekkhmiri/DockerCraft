package com.user.userservice.dto;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String userType;
    private String planType;
    private String subscriptionStatus;
    private int dockerfileGenerationsCount;
    private int deploymentsCount;
    private boolean active;
    private LocalDateTime createdAt;
    // Champs enrichis pour le profil et les quotas
    private boolean isStudentVerified;
    private int dockerfileLimit;   // -1 = illimité
    private int deploymentLimit;   // -1 = illimité

    public UserResponse() {}

    public UserResponse(Long id, String username, String email, String role, String userType,
                        String planType, String subscriptionStatus, int dockerfileGenerationsCount,
                        int deploymentsCount, boolean active, LocalDateTime createdAt,
                        boolean isStudentVerified, int dockerfileLimit, int deploymentLimit) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.userType = userType;
        this.planType = planType;
        this.subscriptionStatus = subscriptionStatus;
        this.dockerfileGenerationsCount = dockerfileGenerationsCount;
        this.deploymentsCount = deploymentsCount;
        this.active = active;
        this.createdAt = createdAt;
        this.isStudentVerified = isStudentVerified;
        this.dockerfileLimit = dockerfileLimit;
        this.deploymentLimit = deploymentLimit;
    }

    public static UserResponseBuilder builder() {
        return new UserResponseBuilder();
    }

    public static class UserResponseBuilder {
        private Long id;
        private String username;
        private String email;
        private String role;
        private String userType;
        private String planType;
        private String subscriptionStatus;
        private int dockerfileGenerationsCount;
        private int deploymentsCount;
        private boolean active;
        private LocalDateTime createdAt;
        private boolean isStudentVerified;
        private int dockerfileLimit;
        private int deploymentLimit;

        public UserResponseBuilder id(Long id) { this.id = id; return this; }
        public UserResponseBuilder username(String username) { this.username = username; return this; }
        public UserResponseBuilder email(String email) { this.email = email; return this; }
        public UserResponseBuilder role(String role) { this.role = role; return this; }
        public UserResponseBuilder userType(String userType) { this.userType = userType; return this; }
        public UserResponseBuilder planType(String planType) { this.planType = planType; return this; }
        public UserResponseBuilder subscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; return this; }
        public UserResponseBuilder dockerfileGenerationsCount(int v) { this.dockerfileGenerationsCount = v; return this; }
        public UserResponseBuilder deploymentsCount(int v) { this.deploymentsCount = v; return this; }
        public UserResponseBuilder active(boolean active) { this.active = active; return this; }
        public UserResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserResponseBuilder isStudentVerified(boolean v) { this.isStudentVerified = v; return this; }
        public UserResponseBuilder dockerfileLimit(int v) { this.dockerfileLimit = v; return this; }
        public UserResponseBuilder deploymentLimit(int v) { this.deploymentLimit = v; return this; }

        public UserResponse build() {
            return new UserResponse(id, username, email, role, userType, planType, subscriptionStatus,
                                    dockerfileGenerationsCount, deploymentsCount, active, createdAt,
                                    isStudentVerified, dockerfileLimit, deploymentLimit);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public int getDockerfileGenerationsCount() { return dockerfileGenerationsCount; }
    public void setDockerfileGenerationsCount(int v) { this.dockerfileGenerationsCount = v; }
    public int getDeploymentsCount() { return deploymentsCount; }
    public void setDeploymentsCount(int v) { this.deploymentsCount = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isStudentVerified() { return isStudentVerified; }
    public void setStudentVerified(boolean v) { this.isStudentVerified = v; }
    public int getDockerfileLimit() { return dockerfileLimit; }
    public void setDockerfileLimit(int v) { this.dockerfileLimit = v; }
    public int getDeploymentLimit() { return deploymentLimit; }
    public void setDeploymentLimit(int v) { this.deploymentLimit = v; }
}
