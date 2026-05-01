package com.user.userservice.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String role;
    private String userType;
    private Long userId;  
    private String message;
    private boolean requiresVerification; 
    private boolean isStudentVerified; // Nouveau champ pour bloquer l'accès si carte non validée
    private String planType;

    public AuthResponse() {}

    public AuthResponse(String token, String username, String email, String role, String userType, 
                        Long userId, String message, boolean requiresVerification, 
                        boolean isStudentVerified, String planType) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.role = role;
        this.userType = userType;
        this.userId = userId;
        this.message = message;
        this.requiresVerification = requiresVerification;
        this.isStudentVerified = isStudentVerified;
        this.planType = planType;
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private String token;
        private String username;
        private String email;
        private String role;
        private String userType;
        private Long userId;
        private String message;
        private boolean requiresVerification;
        private boolean isStudentVerified;
        private String planType;

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder username(String username) { this.username = username; return this; }
        public AuthResponseBuilder email(String email) { this.email = email; return this; }
        public AuthResponseBuilder role(String role) { this.role = role; return this; }
        public AuthResponseBuilder userType(String userType) { this.userType = userType; return this; }
        public AuthResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public AuthResponseBuilder message(String message) { this.message = message; return this; }
        public AuthResponseBuilder requiresVerification(boolean v) { this.requiresVerification = v; return this; }
        public AuthResponseBuilder isStudentVerified(boolean v) { this.isStudentVerified = v; return this; }
        public AuthResponseBuilder planType(String planType) { this.planType = planType; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, username, email, role, userType, userId, message, 
                                    requiresVerification, isStudentVerified, planType);
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRequiresVerification() { return requiresVerification; }
    public void setRequiresVerification(boolean requiresVerification) { this.requiresVerification = requiresVerification; }
    public boolean isStudentVerified() { return isStudentVerified; }
    public void setStudentVerified(boolean verified) { this.isStudentVerified = verified; }
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
}
