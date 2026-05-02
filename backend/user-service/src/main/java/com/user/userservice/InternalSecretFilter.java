package com.user.userservice;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Filter to verify that the request contains the shared internal secret key.
 * This ensures that the request originated from our trusted Gateway or another internal service.
 * Public endpoints (login, register, etc.) are excluded from this check.
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InternalSecretFilter.class);
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String INTERNAL_SECRET_VALUE = "my-super-secret-key-12345";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/verify-email",
            "/api/users/resend-otp",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs",
            "/health",
            "/"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip check for public routes
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            filterChain.doFilter(request, response);
            return;
        }

        final String secret = request.getHeader(INTERNAL_SECRET_HEADER);

        if (secret == null || !secret.equals(INTERNAL_SECRET_VALUE)) {
            logger.warn("Requête rejetée : Clé secrète interne manquante ou invalide pour le chemin {}", path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Accès refusé : origine non autorisée\"}");
            return;
        }

        // Inject internal authentication so Spring Security allows the request
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "INTERNAL_SERVICE", null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
