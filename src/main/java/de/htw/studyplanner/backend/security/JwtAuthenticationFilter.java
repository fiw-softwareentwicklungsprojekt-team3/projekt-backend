package de.htw.studyplanner.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

private final JwtService jwtService;
private final ApiErrorWriter apiErrorWriter;

public JwtAuthenticationFilter(JwtService jwtService, ApiErrorWriter apiErrorWriter) {
    this.jwtService = jwtService;
    this.apiErrorWriter = apiErrorWriter;
    
}

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring("Bearer ".length());

        if (!jwtService.isValid(token)) {
    apiErrorWriter.writeError(
            request,
            response, 
            HttpStatus.UNAUTHORIZED,
            "Dein Token ist ungültig oder abgelaufen. Bitte melde dich erneut an."
    );
    return;
}

        Claims claims = jwtService.parseClaims(token);

        String email = claims.get("email", String.class);
        String rolle = claims.get("rolle", String.class);

        var authority = new SimpleGrantedAuthority("ROLE_" + rolle);

                Integer id = Integer.valueOf(claims.getSubject());

        var authentication = new UsernamePasswordAuthenticationToken(
                new AngemeldetePerson(id, email),
                null,
                List.of(authority));

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String pfad = request.getServletPath();
        // Nur Login und Registrierung brauchen kein Token. /api/me muss gefiltert werden.
        return pfad.equals("/api/auth/login")
                || pfad.equals("/api/auth/register")
                || pfad.startsWith("/api/health")
                || pfad.startsWith("/swagger-ui")
                || pfad.startsWith("/v3/api-docs");
    }
}