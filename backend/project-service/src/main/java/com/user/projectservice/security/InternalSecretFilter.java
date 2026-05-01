package com.user.projectservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InternalSecretFilter.class);
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String INTERNAL_SECRET_VALUE = "my-super-secret-key-12345";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        if (path.contains("/swagger-ui") || path.contains("/v3/api-docs") || path.contains("/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String secret = request.getHeader(INTERNAL_SECRET_HEADER);
        logger.info("Received {} header: {}", INTERNAL_SECRET_HEADER, secret != null ? "****" : "null");

        if (secret == null || !secret.equals(INTERNAL_SECRET_VALUE)) {
            logger.warn("Requête rejetée : Clé secrète interne manquante ou invalide pour le chemin {}", path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Accès refusé : origine non autorisée\"}");
            return;
        }

        // Inject internal authentication so Spring Security allows the request
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "INTERNAL_SERVICE", null,
                    java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
